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

package dev.terminalmc.signedit.mixin.edit;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.signedit.SignEdit;
import dev.terminalmc.signedit.helper.FieldHelper;
import dev.terminalmc.signedit.helper.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export = true)
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

    @Shadow
    @Nullable
    private TextFieldHelper signField;

    @Shadow
    private int line;

    @Shadow
    @Final
    private SignBlockEntity sign;

    @Shadow
    @Final
    private boolean isFrontText;

    @WrapOperation(
            method = "init",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractSignEditScreen;signField:Lnet/minecraft/client/gui/font/TextFieldHelper;"
            )
    )
    private void wrapSetHelper(
            AbstractSignEditScreen instance,
            TextFieldHelper value,
            Operation<Void> original
    ) {
        if (!SignEdit.enhancedEditing) {
            original.call(instance, value);
            return;
        }

        FieldHelper.text = null;
        original.call(
                instance, new FieldHelper(
                        () -> messages,
                        this::signEdit$setMessages,
                        TextFieldHelper.createClipboardGetter(Minecraft.getInstance()),
                        TextFieldHelper.createClipboardSetter(Minecraft.getInstance()),
                        sign::getMaxTextLineWidth,
                        4
                )
        );
    }

    @Unique
    private void signEdit$setMessages(String[] messages) {
        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = messages[i];
            this.text.setMessage(i, Component.literal(messages[i]));
        }
        this.sign.setText(this.text, this.isFrontText);
    }

    @WrapMethod(method = "keyPressed")
    private boolean wrapKeyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            Operation<Boolean> original
    ) {
        if (!SignEdit.enhancedEditing)
            return original.call(keyCode, scanCode, modifiers);

        if (signField == null || messages.length == 0)
            return super.keyPressed(keyCode, scanCode, modifiers);

        return ScreenHelper.keyPressed(messages, (FieldHelper) signField, line, keyCode)
                || signField.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Inject(
            method = "renderSignText",
            at = @At("HEAD")
    )
    private void beforeInit(GuiGraphics graphics, CallbackInfo ci) {
        if (!SignEdit.enhancedEditing)
            return;

        this.line = ((FieldHelper) signField).linePoint(signField.getCursorPos()).line();
    }

    @WrapOperation(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getCursorPos()I"
            )
    )
    private int wrapGetCursorPos(TextFieldHelper instance, Operation<Integer> original) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance);

        return ((FieldHelper) signField).linePoint(original.call(instance)).point();
    }

    @WrapOperation(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getSelectionPos()I"
            )
    )
    private int wrapGetSelectionPos(TextFieldHelper instance, Operation<Integer> original) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance);

        return ((FieldHelper) signField).linePoint(original.call(instance)).point();
    }

    @Inject(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getSelectionPos()I"
            )
    )
    private void afterGetSelectionPos(GuiGraphics graphics, CallbackInfo ci) {
        int color = text.hasGlowingText()
                ? text.getColor().getTextColor()
                : SignRenderer.getDarkColor(text);
        int lineHeight = sign.getTextLineHeight();
        int centerY = messages.length * sign.getTextLineHeight() / 2;
        for (int i = 1; i < messages.length; i++) {
            if (((FieldHelper) signField).newLineBefore(i)) {
                graphics.drawString(
                        font,
                        "\u21a9",
                        sign.getMaxTextLineWidth() / 2,
                        (i - 1) * lineHeight - centerY,
                        color,
                        false
                );
            }
        }
    }

    @WrapOperation(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;substring(II)Ljava/lang/String;",
                    ordinal = 2
            )
    )
    private String wrapSubstring2(
            String instance,
            int beginIndex,
            int endIndex,
            Operation<String> original
    ) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance, beginIndex, endIndex);

        return instance;
    }

    @WrapOperation(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;substring(II)Ljava/lang/String;",
                    ordinal = 3
            )
    )
    private String wrapSubstring3(
            String instance,
            int beginIndex,
            int endIndex,
            Operation<String> original
    ) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance, beginIndex, endIndex);

        return instance;
    }

    @WrapOperation(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I",
                    ordinal = 4
            )
    )
    private int wrapWidth(
            Font instance,
            String text,
            Operation<Integer> original,
            @Local(argsOnly = true) GuiGraphics graphics
    ) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance, text);

        ScreenHelper.render(graphics, (FieldHelper) signField, font, messages, sign);
        return original.call(instance, text);
    }

    @WrapOperation(
            method = "renderSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"
            )
    )
    private void wrapFill(
            GuiGraphics graphics,
            RenderType renderType,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color,
            Operation<Void> original
    ) {
        if (!SignEdit.enhancedEditing) {
            original.call(graphics, renderType, minX, minY, maxX, maxY, color);
        }
    }
}
