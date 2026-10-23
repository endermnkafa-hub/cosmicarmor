package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.RedstoneLaserOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class RedstoneLaserOrbRenderer extends EntityRenderer<RedstoneLaserOrbEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public RedstoneLaserOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.35F;
    }

    @Override
    public void render(RedstoneLaserOrbEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        float age = entity.tickCount + partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 22.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 18.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 14.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        this.blockRenderer.renderSingleBlock(Blocks.REDSTONE_BLOCK.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RedstoneLaserOrbEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
