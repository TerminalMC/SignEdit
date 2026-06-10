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

package dev.terminalmc.signtweaks.mixin.input;

import com.mojang.blaze3d.platform.Window;
import dev.terminalmc.signtweaks.SignTweaks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.signtweaks.config.Config.options;

@Mixin(LocalPlayer.class)
@SuppressWarnings("JavadocReference")
public abstract class LocalPlayerMixin {

    /**
     * Records the keys that are pressed on opening the sign editor, to allow subsequent blocking.
     *
     * @see ContainerEventHandlerMixin#wrapKeyReleased
     * @see dev.terminalmc.signtweaks.mixin.edit.AbstractSignEditScreenMixin#wrapKeyPressed
     */
    @Inject(
            method = "openTextEdit",
            at = @At("HEAD")
    )
    private void onOpenTextEdit(SignBlockEntity sign, boolean isFrontText, CallbackInfo ci) {
        Window window = Minecraft.getInstance().getWindow();
        SignTweaks.downKeys.clear();
        if (options().blockMovementKeys) {

            if (SignTweaks.checkKeys.isEmpty()) {
                Options options = Minecraft.getInstance().options;
                SignTweaks.checkKeys.add(options.keyUp);
                SignTweaks.checkKeys.add(options.keyLeft);
                SignTweaks.checkKeys.add(options.keyDown);
                SignTweaks.checkKeys.add(options.keyRight);
                SignTweaks.checkKeys.add(options.keyJump);
                SignTweaks.checkKeys.add(options.keyShift);
                SignTweaks.checkKeys.add(options.keySprint);
                SignTweaks.checkKeys.add(options.keyUse);
            }

            for (KeyMapping keyMapping : SignTweaks.checkKeys) {
                if (keyMapping.isDown()) {
                    SignTweaks.downKeys.add(keyMapping);
                }
            }
        }
    }
}
