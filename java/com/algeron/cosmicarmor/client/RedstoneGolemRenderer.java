package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.entity.RedstoneGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RedstoneGolemRenderer extends GeoEntityRenderer<RedstoneGolemEntity> {

    public RedstoneGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RedstoneGolemModel());
        this.shadowRadius = 1.6F;
        addRenderLayer(new RedstoneGolemTntLayer(this));
    }

    @Override
    protected void applyRotations(RedstoneGolemEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        // Saldırı veya kilitlenme durumunda gövde doğrudan hedefe bakar
        float bodyYaw = animatable.getAttackState() != RedstoneGolemEntity.STATE_IDLE
                ? animatable.getYRot()
                : Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
    }
}
