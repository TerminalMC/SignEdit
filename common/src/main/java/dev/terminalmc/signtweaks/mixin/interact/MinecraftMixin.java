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

import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.signtweaks.SignTweaks;
import dev.terminalmc.signtweaks.platform.services.PlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.signtweaks.config.Config.options;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public HitResult hitResult;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Nullable
    public LocalPlayer player;

    /**
     * Rudimentary click-through implementation for signs.
     */
    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    public void onGetItemInHand(CallbackInfo ci) {
        // must be enabled
        if (!options().clickThrough)
            return;

        // must not be holding dye/ink/honeycomb
        // (can't use if sneaking so have to allow usage while standing)
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof SignApplicator)
            return;

        // must be targeting some form of block
        if (!(hitResult instanceof BlockHitResult blockHitResult))
            return;

        BlockPos hitBlockPos = blockHitResult.getBlockPos();
        BlockEntity hitBlockEntity = level.getBlockEntity(hitBlockPos);

        // must be targeting a sign
        if (!(hitBlockEntity instanceof SignBlockEntity))
            return;

        BlockState hitBlockState = level.getBlockState(hitBlockPos);
        Block hitBlock = hitBlockState.getBlock();

        // must be targeting a wall sign
        if (!(hitBlock instanceof WallSignBlock))
            return;

        BlockPos wallBlockPos =
                hitBlockPos.offset(hitBlockState.getValue(WallSignBlock.FACING)
                        .getOpposite()
                        .getUnitVec3i());
        BlockState wallBlockState = level.getBlockState(wallBlockPos);
        Block wallBlock = wallBlockState.getBlock();

        // sign must be on a block entity
        if (!(wallBlock instanceof BaseEntityBlock))
            return;

        // sign must be on a block entity that it makes sense to click through to
        if (wallBlock instanceof AbstractBannerBlock
                || wallBlock instanceof AbstractSkullBlock
                || wallBlock instanceof SignBlock
                || wallBlock instanceof Portal
                || wallBlock instanceof SculkShriekerBlock)
            return;

        // must not be sneaking
        if (player.isSteppingCarefully()) {
            // record the time to allow keeping the editor open
            SignTweaks.avoidClickThroughTime = System.nanoTime();
            return;
        }

        // must not have clickthrough plus
        if (PlatformServices.getInstance().isModLoaded("clickthrough"))
            return;

        // retarget
        hitResult = new BlockHitResult(
                blockHitResult.getLocation(),
                blockHitResult.getDirection(),
                wallBlockPos,
                false
        );
    }

    /**
     * Records the time when a sign is placed.
     */
    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private void onUseItemOn(CallbackInfo ci, @Local(name = "heldItem") ItemStack heldItem) {
        if (heldItem.getItem() instanceof SignItem) {
            SignTweaks.signPlaceTime = System.nanoTime();
        }
    }
}
