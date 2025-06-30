/*
 * Copyright 2025 TerminalMC
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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
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
public abstract class AbstractSignEditScreenMixin extends Screen {

    protected AbstractSignEditScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    private SignText text;

    @Shadow
    @Final
    private String[] messages;

    @Shadow
    public abstract void onClose();

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN")
    )
    private void afterConstructor(
            SignBlockEntity sign,
            boolean isFrontText,
            boolean isFiltered,
            Component title,
            CallbackInfo ci
    ) {
        SignEdit.enhancedEditing = options().useEnhancedEditor;
    }

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
        if (options().useEnhancedEditor)
            rowCount++;

        int totalWidth = 200;
        int buttonHeight = 20;
        int spaceX = 10;
        int spaceY = 2;
        int rowHeight = buttonHeight + spaceY;
        int baseX = width / 2 - 100;
        int baseY = height / 4 + 144 - rowHeight * rowCount;
        int movingY = baseY;

        if (options().showActionButtons) {
            int movingX = baseX;
            int buttonWidth = (totalWidth - spaceX * 2) / 3;

            Button copyButton =
                    Button.builder(localized("button", "copy"), (button) -> signEdit$copyText())
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, copyButton);
            movingX += buttonWidth + spaceX;

            Button insertButton =
                    Button.builder(localized("button", "insert"), (button) -> signEdit$insertText())
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, insertButton);
            movingX = baseX + totalWidth - buttonWidth;

            Button eraseButton =
                    Button.builder(localized("button", "erase"), (button) -> signEdit$eraseText())
                            .bounds(movingX, movingY, buttonWidth, buttonHeight)
                            .build();
            original.call(instance, eraseButton);

            movingY += rowHeight;
        }

        if (options().useEnhancedEditor) {
            CycleButton<Boolean> statusButton = CycleButton.onOffBuilder().create(
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
            statusButton.setValue(SignEdit.enhancedEditing);
            original.call(instance, statusButton);
        }

        return original.call(instance, doneButton);
    }

    @Unique
    private void signEdit$copyText() {
        if (!signEdit$isEmpty()) {
            SignEdit.copiedLines = new String[messages.length];
            System.arraycopy(messages, 0, SignEdit.copiedLines, 0, messages.length);
            signEdit$finish();
        }
    }

    @Unique
    private void signEdit$insertText() {
        if (SignEdit.copiedLines != null) {
            System.arraycopy(SignEdit.copiedLines, 0, messages, 0, messages.length);
            signEdit$finish();
        }
    }

    @Unique
    private void signEdit$eraseText() {
        if (!signEdit$isEmpty()) {
            Arrays.fill(messages, "");
            signEdit$finish();
        }
    }

    @Unique
    private boolean signEdit$isEmpty() {
        for (String s : messages) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private void signEdit$finish() {
        if (options().actionButtonsCloseUi) {
            onClose();
        } else {
            init();
        }
    }
}
