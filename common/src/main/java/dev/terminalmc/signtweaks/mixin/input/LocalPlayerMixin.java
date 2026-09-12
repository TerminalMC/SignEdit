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

import dev.terminalmc.signtweaks.SignTweaks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
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
    private void onOpenTextEdit(SignBlockEntity sign, SignTextSlot slot, CallbackInfo ci) {
        SignTweaks.downKeys.clear();
        if (options().blockHeldKeys) {
            for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                if (keyMapping.isDown()) {
                    SignTweaks.downKeys.add(keyMapping);
                }
            }
        }
    }
}
