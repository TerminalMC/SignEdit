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

package dev.terminalmc.signedit.mixin.edit;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.signedit.SignEdit;
import dev.terminalmc.signedit.helper.FieldHelper;
import dev.terminalmc.signedit.helper.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
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
    protected SignBlockEntity sign;

    @Shadow
    @Final
    private boolean isFrontText;

    /**
     * Replaces the existing {@link TextFieldHelper} with a new {@link FieldHelper}.
     */
    @WrapOperation(
            method = "init",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractSignEditScreen;signField:Lnet/minecraft/client/gui/font/TextFieldHelper;",
                    opcode = Opcodes.PUTFIELD
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

        FieldHelper.cachedText = null;
        original.call(
                instance, new FieldHelper(
                        () -> messages,
                        this::signEdit$setMessages,
                        TextFieldHelper.createClipboardGetter(Minecraft.getInstance()),
                        TextFieldHelper.createClipboardSetter(Minecraft.getInstance()),
                        sign::getMaxTextLineWidth,
                        messages.length
                )
        );
    }

    /**
     * Updates the sign text.
     */
    @Unique
    private void signEdit$setMessages(String[] newMessages) {
        for (int i = 0; i < newMessages.length; i++) {
            messages[i] = newMessages[i];
            text.setMessage(i, Component.literal(newMessages[i]));
        }
        sign.setText(text, isFrontText);
    }

    /**
     * Diverts key-presses to {@link ScreenHelper#keyPressed}.
     */
    @WrapMethod(method = "keyPressed")
    private boolean wrapKeyPressed(KeyEvent event, Operation<Boolean> original) {
        if (!SignEdit.enhancedEditing)
            return original.call(event);

        if (signField != null && messages.length > 0) {
            if (ScreenHelper.keyPressed(messages, (FieldHelper) signField, line, event)
                    || signField.keyPressed(event))
                return true;
        }

        if (event.key() != GLFW.GLFW_KEY_SPACE && event.key() != GLFW.GLFW_KEY_TAB)
            return super.keyPressed(event);

        return false;
    }

    /**
     * At the start of the render pass, sets the active line to the line that the cursor is
     * currently on.
     */
    @Inject(
            method = "extractSignText",
            at = @At("HEAD")
    )
    private void beforeRenderSignText(
            GuiGraphicsExtractor graphics,
            Vector2f cursorPosOutput,
            CallbackInfo ci
    ) {
        if (!SignEdit.enhancedEditing)
            return;

        assert signField != null;
        this.line = ((FieldHelper) signField).linePoint(signField.getCursorPos()).line();
    }

    /**
     * Sets the cursor rendering position to the position on the active line.
     */
    @WrapOperation(
            method = "extractSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getCursorPos()I"
            )
    )
    private int wrapGetCursorPos(TextFieldHelper instance, Operation<Integer> original) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance);

        assert signField != null;
        return ((FieldHelper) signField).linePoint(original.call(instance)).point();
    }

    /**
     * Sets the selection rendering position to the same value as the cursor position to prevent the
     * selection highlight code from running.
     */
    @WrapOperation(
            method = "extractSignText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextFieldHelper;getSelectionPos()I"
            )
    )
    private int wrapGetSelectionPos(TextFieldHelper instance, Operation<Integer> original) {
        if (!SignEdit.enhancedEditing)
            return original.call(instance);

        assert signField != null;
        return ((FieldHelper) signField).linePoint(signField.getCursorPos()).point();
    }

    /**
     * At the end of the render pass, renders the multiline selection highlight and linebreak
     * indicators.
     */
    @Inject(
            method = "extractSignText",
            at = @At(value = "RETURN")
    )
    private void afterRenderSignText(
            GuiGraphicsExtractor graphics,
            Vector2f cursorPosOutput,
            CallbackInfo ci
    ) {
        if (!SignEdit.enhancedEditing)
            return;

        assert signField != null;
        FieldHelper helper = (FieldHelper) signField;
        ScreenHelper.renderLinebreaks(graphics, font, helper, sign, text, messages);
        ScreenHelper.renderHighlight(graphics, font, helper, sign, messages);
    }
}
