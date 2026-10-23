package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.RedstoneLandmineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class RedstoneLandmineRenderer extends EntityRenderer<RedstoneLandmineEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public RedstoneLandmineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(RedstoneLandmineEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Zemine hafifçe gömülü, 3D kristal/küp olarak durur
        poseStack.translate(0.0D, 0.1D, 0.0D);
        poseStack.scale(0.48F, 0.32F, 0.48F);
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        this.blockRenderer.renderSingleBlock(Blocks.REDSTONE_BLOCK.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RedstoneLandmineEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
