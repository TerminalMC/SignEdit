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

package dev.terminalmc.signedit.mixin.input;

import com.mojang.blaze3d.platform.Window;
import dev.terminalmc.signedit.SignEdit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.signedit.config.Config.options;

@Mixin(LocalPlayer.class)
@SuppressWarnings("JavadocReference")
public abstract class LocalPlayerMixin {

    /**
     * Records the keys that are pressed on opening the sign editor, to allow subsequent blocking.
     *
     * @see ContainerEventHandlerMixin#wrapKeyReleased
     * @see dev.terminalmc.signedit.mixin.edit.AbstractSignEditScreenMixin#wrapKeyPressed
     */
    @Inject(
            method = "openTextEdit",
            at = @At("HEAD")
    )
    private void onOpenTextEdit(SignBlockEntity sign, boolean isFrontText, CallbackInfo ci) {
        Window window = Minecraft.getInstance().getWindow();
        SignEdit.downKeys.clear();
        if (options().blockMovementKeys) {

            if (SignEdit.checkKeys.isEmpty()) {
                Options options = Minecraft.getInstance().options;
                SignEdit.checkKeys.add(options.keyUp);
                SignEdit.checkKeys.add(options.keyLeft);
                SignEdit.checkKeys.add(options.keyDown);
                SignEdit.checkKeys.add(options.keyRight);
                SignEdit.checkKeys.add(options.keyJump);
                SignEdit.checkKeys.add(options.keyShift);
                SignEdit.checkKeys.add(options.keySprint);
                SignEdit.checkKeys.add(options.keyUse);
            }

            for (KeyMapping keyMapping : SignEdit.checkKeys) {
                if (keyMapping.isDown()) {
                    SignEdit.downKeys.add(keyMapping);
                }
            }
        }
    }
}
