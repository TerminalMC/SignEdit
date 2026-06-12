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

package dev.terminalmc.signtweaks.mixin.interact;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.terminalmc.signtweaks.SignTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.terminalmc.signtweaks.config.Config.options;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    /**
     * Alters the {@link ClientboundOpenSignEditorPacket} handler to optionally prevent opening the
     * editor screen.
     */
    @WrapOperation(
            method = "handleOpenSignEditor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;openTextEdit(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Z)V"
            )
    )
    private void wrapOpenTextEdit(
            LocalPlayer instance,
            SignBlockEntity sign,
            boolean isFrontText,
            Operation<Void> original
    ) {
        long timeNow = System.nanoTime();
        long timeSincePlace = timeNow - SignTweaks.signPlaceTime;
        long timeSinceClickThrough = timeNow - SignTweaks.avoidClickThroughTime;
        System.out.println("time since last clickthrough: " + timeSinceClickThrough);

        if (timeSincePlace < 1_000_000_000L) { // 1 sec
            SignTweaks.signPlaceTime = 0;
            if (options().useAutoFill) {
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.send(new ServerboundSignUpdatePacket(
                            sign.getBlockPos(),
                            isFrontText,
                            options().autoFillLines[0],
                            options().autoFillLines[1],
                            options().autoFillLines[2],
                            options().autoFillLines[3]
                    ));
                }
                return;
            }
        }

        boolean allow = switch (options().editCondition) {
            case SNEAKING -> instance.isSteppingCarefully();
            case NOT_SNEAKING -> !instance.isSteppingCarefully()
                    // override when sneak-clicking to avoid click-through
                    || timeSinceClickThrough < 1_000_000_000L; // 1 sec
            case ALWAYS -> true;
            case NEVER -> false;
        };

        if (allow) {
            original.call(instance, sign, isFrontText);
        }
    }
}
