package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.LeapingGroundBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class LeapingBlockRenderer extends EntityRenderer<LeapingGroundBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public LeapingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(LeapingGroundBlockEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        this.blockRenderer.renderSingleBlock(entity.getBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LeapingGroundBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
