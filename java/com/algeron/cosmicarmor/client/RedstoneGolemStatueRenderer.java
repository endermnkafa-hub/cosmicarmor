package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RedstoneGolemStatueRenderer extends GeoEntityRenderer<RedstoneGolemStatueEntity> {

    public RedstoneGolemStatueRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedstoneGolemStatueModel());
        this.shadowRadius = 1.6F;
    }

    @Override
    public void render(RedstoneGolemStatueEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Uyanış esnasında şiddetlenen titreme / deprem efekti
        if (entity.isAwakening()) {
            float progress = Math.min(1.0F, entity.getAwakeningTicks() / 110.0F);
            float shakeIntensity = progress * 0.15F;
            double offsetX = (Math.random() - 0.5D) * shakeIntensity;
            double offsetZ = (Math.random() - 0.5D) * shakeIntensity;
            poseStack.translate(offsetX, 0, offsetZ);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
