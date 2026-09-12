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

package dev.terminalmc.signtweaks.mixin.copy;

import dev.terminalmc.signtweaks.SignTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static dev.terminalmc.signtweaks.util.Localization.localized;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin {

    /**
     * Copies the text from a waxed sign when the player 'uses' it with an empty hand.
     */
    @Inject(
            method = "useItemOn",
            at = @At("HEAD")
    )
    public void onSignUse(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign
                && sign.isWaxed()
                && itemStack.isEmpty()) {
            List<Component> textLines = sign.getText(SignTextSlot.FRONT).getMessages(false);
            SignTweaks.copiedLines.clear();
            SignTweaks.copiedLines.addAll(textLines.stream().map(Component::getString).toList());
            player.sendOverlayMessage(localized("message", "copied"));
        }
    }
}
