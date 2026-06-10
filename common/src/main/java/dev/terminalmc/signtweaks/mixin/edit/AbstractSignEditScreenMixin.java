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

package dev.terminalmc.signtweaks.mixin.edit;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.signtweaks.SignTweaks;
import dev.terminalmc.signtweaks.helper.FieldHelper;
import dev.terminalmc.signtweaks.helper.ScreenHelper;
import dev.terminalmc.signtweaks.mixin.input.ContainerEventHandlerMixin;
import dev.terminalmc.signtweaks.mixin.input.LocalPlayerMixin;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
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

import static dev.terminalmc.signtweaks.config.Config.options;

@Debug(export = true)
@Mixin(AbstractSignEditScreen.class)
@SuppressWarnings("JavadocReference")
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

    @Unique
    private static boolean signEdit$cancelKeyPressed;

    @Unique
    private static long signEdit$cancelKeyPressedTime;

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
        if (!SignTweaks.enhancedEditing) {
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
     * Blocks key-presses if required, otherwise redirects to {@link ScreenHelper#keyPressed}.
     *
     * @see LocalPlayerMixin#onOpenTextEdit
     * @see ContainerEventHandlerMixin#wrapKeyReleased
     * @see #wrapCharTyped
     */
    @WrapMethod(method = "keyPressed")
    private boolean wrapKeyPressed(KeyEvent event, Operation<Boolean> original) {
        if (options().blockMovementKeys) {
            for (KeyMapping keyMapping : SignTweaks.downKeys) {
                if (keyMapping.matches(event)) {
                    signEdit$cancelKeyPressed = true;
                    signEdit$cancelKeyPressedTime = System.nanoTime();
                    return false;
                }
            }
        }

        if (!SignTweaks.enhancedEditing)
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
     * Blocks the {@link AbstractSignEditScreen#charTyped} event associated with a blocked key
     * press.
     *
     * @see #wrapKeyPressed
     */
    @WrapMethod(method = "charTyped")
    private boolean wrapCharTyped(CharacterEvent event, Operation<Boolean> original) {
        if (signEdit$cancelKeyPressed) {
            signEdit$cancelKeyPressed = false;
            // Cancel only if the most recent canceled press
            // was less than 5 milliseconds ago
            if (System.nanoTime() - signEdit$cancelKeyPressedTime < 5_000_000)
                return false;
        }
        return original.call(event);
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
        if (!SignTweaks.enhancedEditing)
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
        if (!SignTweaks.enhancedEditing)
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
        if (!SignTweaks.enhancedEditing)
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
        if (!SignTweaks.enhancedEditing)
            return;

        assert signField != null;
        FieldHelper helper = (FieldHelper) signField;
        if (options().showLineBreakIndicator)
            ScreenHelper.renderLinebreaks(graphics, font, helper, sign, text, messages);
        ScreenHelper.renderHighlight(graphics, font, helper, sign, messages);
    }
}
