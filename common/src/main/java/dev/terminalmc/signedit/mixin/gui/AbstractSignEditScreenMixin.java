/*
 * Copyright 2026 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.signedit.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.signedit.SignEdit;
import dev.terminalmc.signedit.util.inject.ISignScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

import static dev.terminalmc.signedit.config.Config.options;
import static dev.terminalmc.signedit.util.Localization.localized;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen implements ISignScreen {

    protected AbstractSignEditScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    @Final
    private String[] messages;

    @Shadow
    public abstract void onClose();

    /**
     * On creation of an {@link AbstractSignEditScreen}, resets the value of
     * {@link SignEdit#enhancedEditing} to the default value and stores the original sign text.
     */
    @Inject(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN")
    )
    private void afterConstructor(
            SignBlockEntity sign,
            boolean isFrontText,
            boolean shouldFilter,
            Component title,
            CallbackInfo ci
    ) {
        SignEdit.enhancedEditing = options().useEnhancedEditor;

        SignEdit.originalLines = new String[messages.length];
        System.arraycopy(messages, 0, SignEdit.originalLines, 0, messages.length);
    }

    /**
     * Wraps the addition of the 'Done' button to add extra buttons as required by mod config.
     */
    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractSignEditScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"
            )
    )
    private GuiEventListener wrapAddDoneButton(
            AbstractSignEditScreen instance,
            GuiEventListener doneButton,
            Operation<GuiEventListener> original
    ) {
        clearWidgets();

        int rowCount = 0;
        if (options().showActionButtons)
            rowCount++;
        if (options().showEditorToggleButton)
            rowCount++;

        int totalWidth = 200;
        int buttonHeight = 20;
        int spaceX = 5;
        int spaceY = 2;
        int rowHeight = buttonHeight + spaceY;
        int baseX = width / 2 - 100;
        int movingY = height / 4 + 144 - rowHeight * rowCount;

        // Quick-action buttons
        if (options().showActionButtons) {
            int movingX = baseX;
            int buttonWidth = (totalWidth - spaceX * 2) / 4;

            // Copy
            Button copyButton =
                    Button.builder(
                                    localized("button", "copy"),
                                    (button) -> signEdit$copyText()
                            )
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, copyButton);
            movingX += buttonWidth + spaceX;

            // Insert
            Button insertButton =
                    Button.builder(
                                    localized("button", "insert"),
                                    (button) -> signEdit$insertText()
                            )
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, insertButton);
            movingX = baseX + totalWidth - buttonWidth * 2 - spaceX;

            // Erase
            Button eraseButton =
                    Button.builder(
                                    localized("button", "erase"),
                                    (button) -> signEdit$eraseText()
                            )
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, eraseButton);
            movingX += buttonWidth + spaceX;

            // Revert
            Button revertButton =
                    Button.builder(
                                    localized("button", "revert"),
                                    (button) -> signEdit$revertText(true)
                            )
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, revertButton);

            movingY += rowHeight;
        }

        // Enhanced editor toggle button
        if (options().showEditorToggleButton) {
            CycleButton<@NotNull Boolean> statusButton = CycleButton
                    .onOffBuilder(SignEdit.enhancedEditing)
                    .create(
                            baseX,
                            movingY,
                            totalWidth,
                            buttonHeight,
                            localized("button", "enhancedEditing"),
                            (button, status) -> {
                                if (SignEdit.enhancedEditing != status) {
                                    SignEdit.enhancedEditing = status;
                                    init();
                                }
                            }
                    );
            original.call(instance, statusButton);
        }

        // Add the 'Done' button last.
        return original.call(instance, doneButton);
    }

    /**
     * Copies the sign text to {@link SignEdit#copiedLines}.
     */
    @Unique
    private void signEdit$copyText() {
        if (signEdit$hasText()) {
            SignEdit.copiedLines = new String[messages.length];
            System.arraycopy(messages, 0, SignEdit.copiedLines, 0, messages.length);
            signEdit$finish();
        }
    }

    /**
     * Replaces the sign text with {@link SignEdit#copiedLines}.
     */
    @Unique
    private void signEdit$insertText() {
        if (SignEdit.copiedLines != null) {
            System.arraycopy(
                    SignEdit.copiedLines,
                    0,
                    messages,
                    0,
                    Math.min(SignEdit.copiedLines.length, messages.length)
            );
            signEdit$finish();
        }
    }

    /**
     * Clears the sign text.
     */
    @Unique
    private void signEdit$eraseText() {
        if (signEdit$hasText()) {
            Arrays.fill(messages, "");
            signEdit$finish();
        }
    }

    /**
     * Reverts the sign text to {@link SignEdit#originalLines}.
     */
    @Unique
    @Override
    public void signEdit$revertText(boolean isManual) {
        if (SignEdit.originalLines != null) {
            System.arraycopy(
                    SignEdit.originalLines,
                    0,
                    messages,
                    0,
                    Math.min(SignEdit.originalLines.length, messages.length)
            );
            if (isManual) {
                signEdit$finish();
            }
        }
    }

    /**
     * @return {@code true} if the sign is not empty of text.
     */
    @Unique
    private boolean signEdit$hasText() {
        for (String s : messages) {
            if (!s.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes or re-initializes the screen as required by mod config.
     */
    @Unique
    private void signEdit$finish() {
        if (options().actionButtonsCloseUi) {
            onClose();
        } else {
            init();
        }
    }
}
