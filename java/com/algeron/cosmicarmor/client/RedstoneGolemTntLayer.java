package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.RedstoneGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class RedstoneGolemTntLayer extends BlockAndItemGeoLayer<RedstoneGolemEntity> {

    public RedstoneGolemTntLayer(GeoRenderer<RedstoneGolemEntity> renderer) {
        super(renderer);
    }

    @Override
    protected BlockState getBlockForBone(GeoBone bone, RedstoneGolemEntity animatable) {
        if ("redstone_golem_forearm_left".equals(bone.getName())) {
            if (animatable.getAttackState() == RedstoneGolemEntity.STATE_THROW_TNT && animatable.getAttackTick() <= 17) {
                return Blocks.TNT.defaultBlockState();
            }
        }
        return null;
    }

    @Override
    protected void renderBlockForBone(PoseStack poseStack, GeoBone bone, BlockState state, RedstoneGolemEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // TNT bloğunu golem sol elinin içine konumlandır ve ölçeklendir
        poseStack.translate(-0.5D, -0.6D, -0.5D);
        poseStack.scale(1.2F, 1.2F, 1.2F);
        super.renderBlockForBone(poseStack, bone, state, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
