package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.RedstoneGolemTntEntity;
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

public class RedstoneGolemTntRenderer extends EntityRenderer<RedstoneGolemTntEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public RedstoneGolemTntRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(RedstoneGolemTntEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        float age = entity.tickCount + partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 16.0F));
        this.blockRenderer.renderSingleBlock(Blocks.TNT.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RedstoneGolemTntEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
