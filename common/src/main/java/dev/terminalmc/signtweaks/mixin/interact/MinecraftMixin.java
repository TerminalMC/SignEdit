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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.signtweaks.config.Config.options;

@Mixin(Minecraft.class)
@SuppressWarnings("JavadocReference")
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
     * Allows clicking through signs, banners and hanging entities (item frames and paintings) to
     * certain block entities when not sneaking.
     */
    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    public void onGetItemInHand(CallbackInfo ci) {

        if (hitResult instanceof BlockHitResult blockHitResult) {
            // signs, banners
            BlockPos hitBlockPos = blockHitResult.getBlockPos();
            BlockState hitBlockState = level.getBlockState(hitBlockPos);
            Block hitBlock = hitBlockState.getBlock();

            if (signTweaks$clickThroughSign(blockHitResult, hitBlockPos, hitBlockState, hitBlock))
                return;
            signTweaks$clickThroughBanner(blockHitResult, hitBlockPos, hitBlockState, hitBlock);
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            // item frames, paintings
            Entity hitEntity = entityHitResult.getEntity();
            BlockPos hitEntityPos = hitEntity.blockPosition();

            signTweaks$clickThroughHangingEntity(entityHitResult, hitEntityPos, hitEntity);
        }
    }

    /**
     * Records the time when a sign is placed, to allow subsequent conditional opening of the
     * editor.
     *
     * @see ClientPacketListenerMixin#wrapOpenTextEdit
     */
    @Inject(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private void onUseItemOn(
            CallbackInfo ci,
            @Local(name = "heldItem") ItemStack heldItem,
            @Local(name = "blockHit") BlockHitResult blockHit
    ) {
        if (heldItem.getItem() instanceof SignItem) {
            SignTweaks.signPlaceTime = System.currentTimeMillis();

            BlockPos blockPos = blockHit.getBlockPos();
            BlockState blockState = level.getBlockState(blockPos);
            Block block = blockState.getBlock();

            if (signTweaks$isClickableBlockEntity(block)) {
                SignTweaks.signPlaceOnBlockEntityTime = System.currentTimeMillis();
            }
        }
    }

    @Unique
    private boolean signTweaks$clickThroughSign(
            BlockHitResult blockHitResult,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            Block hitBlock
    ) {
        // must be enabled
        if (!options().clickThroughSigns)
            return false;

        // must not be holding dye/ink/honeycomb
        // (can't use while sneaking so have to allow usage while standing)
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof SignApplicator)
            return false;

        // must be targeting a wall sign
        if (!(hitBlock instanceof WallSignBlock))
            return false;

        // get the block that the sign is on
        BlockPos wallBlockPos = hitBlockPos.offset(hitBlockState.getValue(WallSignBlock.FACING)
                .getOpposite()
                .getUnitVec3i());
        BlockState wallBlockState = level.getBlockState(wallBlockPos);
        Block wallBlock = wallBlockState.getBlock();

        // must be on a block entity that it makes sense to click through to
        if (!signTweaks$isClickableBlockEntity(wallBlock))
            return false;

        // must not be sneaking
        if (player.isSteppingCarefully()) {
            // record the time to allow overriding the editor condition in
            // ClientPacketListenerMixin#wrapOpenTextEdit
            SignTweaks.avoidClickThroughTime = System.currentTimeMillis();
            return false;
        }

        // must not have ClickThrough Plus
        if (PlatformServices.getInstance().isModLoaded("clickthrough"))
            return false;

        // retarget
        hitResult = new BlockHitResult(
                blockHitResult.getLocation(),
                blockHitResult.getDirection(),
                wallBlockPos,
                false
        );
        return true;
    }

    @Unique
    private boolean signTweaks$clickThroughBanner(
            BlockHitResult blockHitResult,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            Block hitBlock
    ) {
        // must be enabled
        if (!options().clickThroughBanners)
            return false;

        // must not be sneaking
        if (player.isSteppingCarefully())
            return false;

        // must be targeting a wall banner
        if (!(hitBlock instanceof WallBannerBlock))
            return false;

        // get the block that the banner is on
        BlockPos wallBlockPos = hitBlockPos.offset(hitBlockState.getValue(WallBannerBlock.FACING)
                .getOpposite()
                .getUnitVec3i());
        BlockState wallBlockState = level.getBlockState(wallBlockPos);
        Block wallBlock = wallBlockState.getBlock();

        // must be on a block entity that it makes sense to click through to
        if (!signTweaks$isClickableBlockEntity(wallBlock))
            return false;

        // retarget
        hitResult = new BlockHitResult(
                blockHitResult.getLocation(),
                blockHitResult.getDirection(),
                wallBlockPos,
                false
        );
        return true;
    }

    @Unique
    private boolean signTweaks$clickThroughHangingEntity(
            EntityHitResult entityHitResult,
            BlockPos hitEntityPos,
            Entity hitEntity
    ) {
        // must be enabled
        if (!options().clickThroughHangingEntities)
            return false;

        // must not be sneaking
        if (player.isSteppingCarefully())
            return false;

        // must be targeting a hanging entity
        if (!(hitEntity instanceof HangingEntity hangingEntity))
            return false;

        // must not have ClickThrough Plus
        if (PlatformServices.getInstance().isModLoaded("clickthrough"))
            return false;

        // get the block that the entity is on
        BlockPos wallBlockPos = hitEntityPos.offset(hitEntity.getDirection()
                .getOpposite()
                .getUnitVec3i());
        BlockState wallBlockState = level.getBlockState(wallBlockPos);
        Block wallBlock = wallBlockState.getBlock();

        // must be on a block entity that it makes sense to click through to
        if (!signTweaks$isClickableBlockEntity(wallBlock))
            return false;

        // retarget
        hitResult = new BlockHitResult(
                entityHitResult.getLocation(),
                hangingEntity.getDirection().getOpposite(),
                wallBlockPos,
                false
        );
        return true;
    }

    @Unique
    private boolean signTweaks$isClickableBlockEntity(Block block) {
        if (!(block instanceof BaseEntityBlock))
            return false;

        // generally, exclude anything that can be clicked through plus anything
        // that doesn't have a right-click interaction
        if (block instanceof AbstractBannerBlock
                || block instanceof AbstractSkullBlock
                || block instanceof ConduitBlock
                || block instanceof CreakingHeartBlock
                || block instanceof SignBlock
                || block instanceof Portal
                || block instanceof SculkSensorBlock
                || block instanceof SculkCatalystBlock
                || block instanceof SculkShriekerBlock
        )
            return false;

        return true;
    }
}
