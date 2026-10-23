package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.entity.RedstoneMortarTargetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RedstoneMortarTargetRenderer extends EntityRenderer<RedstoneMortarTargetEntity> {
    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "textures/misc/white.png");

    public RedstoneMortarTargetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RedstoneMortarTargetEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Level level = entity.level();
        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();

        int lifetime = entity.getLifetime();
        float pulseSpeed = lifetime > 20 ? 0.85F : 0.40F;
        float pulse = 0.38F + 0.18F * Mth.sin((entity.tickCount + partialTicks) * pulseSpeed);
        if (lifetime > 22) {
            pulse = Math.min(0.75F, pulse + 0.20F);
        }
        int alpha = (int) (pulse * 255.0F);
        alpha = Mth.clamp(alpha, 60, 200);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        int fullLight = LightTexture.pack(15, 15);

        poseStack.pushPose();
        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        int centerBlockX = Mth.floor(entityX);
        int centerBlockY = Mth.floor(entityY);
        int centerBlockZ = Mth.floor(entityZ);

        // 5x5 dairesel alandaki zemin bloklarının üst yüzeyine saydam kırmızı katman çizer
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5.5) continue;

                int targetBx = centerBlockX + dx;
                int targetBz = centerBlockZ + dz;

                // En yakın katı zemin yüksekliğini bul
                int groundY = centerBlockY;
                for (int checkY = centerBlockY + 1; checkY >= centerBlockY - 3; checkY--) {
                    BlockPos checkPos = new BlockPos(targetBx, checkY, targetBz);
                    BlockState bs = level.getBlockState(checkPos);
                    if (!bs.isAir() && bs.isSolidRender(level, checkPos)) {
                        groundY = checkY;
                        break;
                    }
                }

                float minX = (float) (targetBx - entityX);
                float maxX = minX + 1.0F;
                float minZ = (float) (targetBz - entityZ);
                float maxZ = minZ + 1.0F;
                // Z-fighting'i önlemek için blok yüzeyinden hafifçe yukarıda (3mm)
                float surfaceY = (float) ((groundY + 1.0D + 0.003D) - entityY);

                int cellAlpha = (dx == 0 && dz == 0) ? Math.min(220, alpha + 35) : alpha;

                consumer.vertex(poseMatrix, minX, surfaceY, minZ)
                        .color(240, 25, 25, cellAlpha)
                        .uv(0.0F, 0.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(fullLight)
                        .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                        .endVertex();

                consumer.vertex(poseMatrix, minX, surfaceY, maxZ)
                        .color(240, 25, 25, cellAlpha)
                        .uv(0.0F, 1.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(fullLight)
                        .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                        .endVertex();

                consumer.vertex(poseMatrix, maxX, surfaceY, maxZ)
                        .color(240, 25, 25, cellAlpha)
                        .uv(1.0F, 1.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(fullLight)
                        .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                        .endVertex();

                consumer.vertex(poseMatrix, maxX, surfaceY, minZ)
                        .color(240, 25, 25, cellAlpha)
                        .uv(1.0F, 0.0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(fullLight)
                        .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                        .endVertex();
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RedstoneMortarTargetEntity entity) {
        return WHITE_TEXTURE;
    }
}
