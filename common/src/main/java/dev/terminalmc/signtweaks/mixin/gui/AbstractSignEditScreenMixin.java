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

package dev.terminalmc.signtweaks.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.signtweaks.SignTweaks;
import dev.terminalmc.signtweaks.gui.widget.UnfocusingButton;
import dev.terminalmc.signtweaks.util.inject.ISignScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

import static dev.terminalmc.signtweaks.config.Config.options;
import static dev.terminalmc.signtweaks.util.Localization.localized;

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

    @Shadow
    @Final
    private TextFieldHelper signField;

    /**
     * On creation of an {@link AbstractSignEditScreen}, resets the value of
     * {@link SignTweaks#enhancedEditing} to the default value and stores the original sign text.
     */
    @Inject(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN")
    )
    private void afterConstructor(
            SignBlockEntity sign,
            SignTextSlot slot,
            boolean shouldFilter,
            Component title,
            CallbackInfo ci
    ) {
        SignTweaks.originalLines.clear();
        SignTweaks.originalLines.addAll(Arrays.asList(messages));
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

        int totalWidth = 200;
        int buttonHeight = 20;
        int spaceX = 4;
        int spaceY = 2;
        int rowHeight = buttonHeight + spaceY;
        int baseX = width / 2 - 100;
        int movingY = height / 4 + 144 - rowHeight * rowCount;

        // Quick-action buttons
        if (options().showActionButtons) {
            int movingX = baseX;
            int buttonWidth = (totalWidth - spaceX * 3) / 4;

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

            // Replace
            Button replaceButton =
                    Button.builder(
                                    localized("button", "replace"),
                                    (button) -> signEdit$replaceText()
                            )
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, replaceButton);
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
        }

        // Add the 'Done' button last.
        if (options().showEditorToggleButton) {
            if (doneButton instanceof Button button) {
                // reduce width to add space for editor toggle button
                button.setWidth(button.getWidth() - spaceX - buttonHeight);
                GuiEventListener result = original.call(instance, button);

                // add editor toggle button
                UnfocusingButton toggleButton = new UnfocusingButton(
                        button.getX() + button.getWidth() + spaceX,
                        button.getY(),
                        buttonHeight, // square
                        buttonHeight,
                        Component.literal("\u270E").withStyle(SignTweaks.enhancedEditing // ✎
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED),
                        (b) -> {
                            SignTweaks.enhancedEditing = !SignTweaks.enhancedEditing;
                            init();
                        }
                );
                toggleButton.setTooltip(Tooltip.create(localized(
                        "button",
                        "enhancedEditing.tooltip." + (SignTweaks.enhancedEditing
                                ? "enabled"
                                : "disabled")
                )));
                original.call(instance, toggleButton);
                return result;
            } else {
                SignTweaks.LOG.error(
                        "Done button has wrong type! Expected '{}', got '{}'",
                        Button.class,
                        doneButton.getClass()
                );
            }
        }
        return original.call(instance, doneButton);
    }

    /**
     * Copies the sign text to {@link SignTweaks#copiedLines}.
     */
    @Unique
    private void signEdit$copyText() {
        if (signEdit$hasText()) {
            SignTweaks.copiedLines.clear();
            SignTweaks.copiedLines.addAll(Arrays.asList(messages));
            signEdit$finish();
        }
    }

    /**
     * Replaces the sign text with {@link SignTweaks#copiedLines}.
     */
    @Unique
    private void signEdit$replaceText() {
        if (!SignTweaks.copiedLines.isEmpty()) {
            for (int i = 0; i < messages.length && i < SignTweaks.copiedLines.size(); i++) {
                messages[i] = SignTweaks.copiedLines.get(i);
            }
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
     * Reverts the sign text to {@link SignTweaks#originalLines}.
     */
    @Unique
    @Override
    public void signEdit$revertText(boolean isManual) {
        if (!SignTweaks.originalLines.isEmpty()) {
            for (int i = 0; i < messages.length && i < SignTweaks.originalLines.size(); i++) {
                messages[i] = SignTweaks.originalLines.get(i);
            }
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
