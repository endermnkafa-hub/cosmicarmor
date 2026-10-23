package com.algeron.cosmicarmor.vfx;

import com.algeron.cosmicarmor.combat.GloveComboManager;
import com.algeron.cosmicarmor.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.SimpleParticleOptions;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.Color;
import java.util.Random;

/**
 * Authentic Aura VFX:
 * 1. STANDARD: Jujutsu Kaisen Cursed Energy (Yuji Itadori Divergent Fist - cyan flame, black cursed ink, sparks).
 * 2. COSMIC: Pure Deep Space / Cosmos Aura (Pitch black cosmic void, deep galactic purple/indigo nebula gas,
 *    and brilliant diamond-white twinkling star clusters. No rainbow clash).
 */
public final class CosmicAuraVFX {
    private static final Random RANDOM = new Random();

    public enum AuraType {
        STANDARD,
        COSMIC
    }

    public static void spawnAuraParticles(LivingEntity entity) {
        spawnAuraParticles(entity, AuraType.STANDARD);
    }

    public static void spawnAuraParticles(LivingEntity entity, AuraType auraType) {
        if (!entity.level().isClientSide) return;

        if (ModList.get().isLoaded("lodestone")) {
            LodestoneHelper.spawnAura(entity, auraType);
        } else {
            spawnVanillaFallbackAura(entity, auraType);
        }
    }

    private static void spawnVanillaFallbackAura(LivingEntity entity, AuraType auraType) {
        spawnVanillaHandAura(entity, true, auraType);
        spawnVanillaHandAura(entity, false, auraType);
    }

    private static void spawnVanillaHandAura(LivingEntity entity, boolean rightHand, AuraType auraType) {
        Vec3 handPos = getHandPosition(entity, rightHand);
        Vec3 entityMotion = entity.getDeltaMovement();
        var level = entity.level();

        if (auraType == AuraType.COSMIC) {
            // Pure Deep Space fallback: Deep portal vacuum + dragon breath nebula + end rod diamond stars
            level.addParticle(
                    ParticleTypes.PORTAL,
                    handPos.x + (RANDOM.nextDouble() - 0.5) * 0.12,
                    handPos.y + (RANDOM.nextDouble() - 0.5) * 0.12,
                    handPos.z + (RANDOM.nextDouble() - 0.5) * 0.12,
                    (RANDOM.nextDouble() - 0.5) * 0.15,
                    (RANDOM.nextDouble() - 0.5) * 0.15,
                    (RANDOM.nextDouble() - 0.5) * 0.15
            );
            if (RANDOM.nextFloat() < 0.60f) {
                level.addParticle(
                        ParticleTypes.DRAGON_BREATH,
                        handPos.x + (RANDOM.nextDouble() - 0.5) * 0.08,
                        handPos.y + (RANDOM.nextDouble() - 0.5) * 0.08,
                        handPos.z + (RANDOM.nextDouble() - 0.5) * 0.08,
                        entityMotion.x,
                        entityMotion.y + 0.015,
                        entityMotion.z
                );
            }
            if (RANDOM.nextFloat() < 0.45f) {
                level.addParticle(
                        ParticleTypes.END_ROD,
                        handPos.x + (RANDOM.nextDouble() - 0.5) * 0.06,
                        handPos.y + (RANDOM.nextDouble() - 0.5) * 0.06,
                        handPos.z + (RANDOM.nextDouble() - 0.5) * 0.06,
                        entityMotion.x,
                        entityMotion.y + 0.01,
                        entityMotion.z
                );
            }
        } else {
            // Standard JJK fallback: soul fire flame, glow squid ink, electric spark
            level.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    handPos.x + (RANDOM.nextDouble() - 0.5) * 0.08,
                    handPos.y + (RANDOM.nextDouble() - 0.5) * 0.08,
                    handPos.z + (RANDOM.nextDouble() - 0.5) * 0.08,
                    entityMotion.x + (RANDOM.nextDouble() - 0.5) * 0.01,
                    entityMotion.y + 0.02 + RANDOM.nextDouble() * 0.02,
                    entityMotion.z + (RANDOM.nextDouble() - 0.5) * 0.01
            );

            if (RANDOM.nextFloat() < 0.6f) {
                level.addParticle(
                        ParticleTypes.GLOW_SQUID_INK,
                        handPos.x + (RANDOM.nextDouble() - 0.5) * 0.10,
                        handPos.y + (RANDOM.nextDouble() - 0.5) * 0.10,
                        handPos.z + (RANDOM.nextDouble() - 0.5) * 0.10,
                        entityMotion.x,
                        entityMotion.y + 0.02,
                        entityMotion.z
                );
            }

            if (RANDOM.nextFloat() < 0.35f) {
                level.addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        handPos.x, handPos.y, handPos.z,
                        entityMotion.x + (RANDOM.nextDouble() - 0.5) * 0.03,
                        entityMotion.y + (RANDOM.nextDouble() - 0.5) * 0.03,
                        entityMotion.z + (RANDOM.nextDouble() - 0.5) * 0.03
                );
            }
        }
    }

    /**
     * Extracts the exact world coordinates of the fist/knuckles for both 1st and 3rd person.
     */
    public static Vec3 getHandPosition(LivingEntity entity, boolean rightHand) {
        Minecraft mc = Minecraft.getInstance();
        float partialTicks = mc.getFrameTime();

        // 1. FIRST-PERSON CAMERA-LOCKED CALCULATION (Primary for first-person player)
        // In 1st person, yBodyRot does not track camera pitch/yaw immediately, so we must calculate
        // directly from the camera vectors (eyePos, lookVec, upVec, rightVec) to keep particles locked to the hands on-screen!
        if (mc.player == entity && mc.options.getCameraType().isFirstPerson()) {
            Vec3 eyePos = entity.getEyePosition(partialTicks);
            Vec3 lookVec = entity.getViewVector(partialTicks);
            Vec3 upVec = entity.getUpVector(partialTicks);
            Vec3 rightVec = lookVec.cross(upVec).normalize();

            // Default resting hand position in 1st person view (centered directly on visible hand)
            double side = rightHand ? 0.33 : -0.33;
            double forward = 0.42;
            double down = -0.20;

            if (entity instanceof Player player) {
                int activeFist = GloveComboManager.getActivePunchFist(player, true);
                if (activeFist > 0) {
                    float ext = GloveComboManager.getPunchExtension(player, true);
                    boolean isPunchingHand = (activeFist == 2) ? !rightHand : rightHand;
                    if (isPunchingHand) {
                        // Thrusts forward and aligns toward crosshair center
                        forward += 0.48 * ext;
                        down += 0.12 * ext;
                        side += (rightHand ? -0.15 : 0.15) * ext;
                    } else {
                        // Off-hand pulls back slightly
                        forward -= 0.08 * ext;
                    }
                }
            }

            return eyePos.add(lookVec.scale(forward)).add(rightVec.scale(side)).add(upVec.scale(down));
        }

        // 2. EXACT MODEL BONE MATRIX RECONSTRUCTION (For 3rd person / other entities)
        if (mc.getEntityRenderDispatcher() != null) {
            var renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
            if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer
                    && livingRenderer.getModel() instanceof HumanoidModel<?> humanoidModel) {
                HumanoidArm arm = rightHand ? HumanoidArm.RIGHT : HumanoidArm.LEFT;

                double interpX = net.minecraft.util.Mth.lerp(partialTicks, entity.xOld, entity.getX());
                double interpY = net.minecraft.util.Mth.lerp(partialTicks, entity.yOld, entity.getY());
                double interpZ = net.minecraft.util.Mth.lerp(partialTicks, entity.zOld, entity.getZ());
                float interpBodyRot = net.minecraft.util.Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);

                PoseStack poseStack = new PoseStack();
                // 1) Translate to interpolated entity origin
                poseStack.translate(interpX, interpY, interpZ);

                // 2) Minecraft LivingEntity setupRotations (180 - yBodyRot)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - interpBodyRot));

                // 3) Player Animator injects into setupRotations BEFORE model scale (-1, -1, 1)!
                if (entity instanceof dev.kosmx.playerAnim.impl.IAnimatedPlayer animatedPlayer) {
                    var animApplier = animatedPlayer.playerAnimator_getAnimation();
                    if (animApplier != null && animApplier.isActive()) {
                        animApplier.setTickDelta(partialTicks);
                        var bodyPos = animApplier.get3DTransform("body", dev.kosmx.playerAnim.api.TransformType.POSITION, dev.kosmx.playerAnim.core.util.Vec3f.ZERO);
                        poseStack.translate(
                                (double) bodyPos.getX().floatValue(),
                                (double) bodyPos.getY().floatValue() + 0.7D,
                                (double) bodyPos.getZ().floatValue()
                        );
                        var bodyRot = animApplier.get3DTransform("body", dev.kosmx.playerAnim.api.TransformType.ROTATION, dev.kosmx.playerAnim.core.util.Vec3f.ZERO);
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotation((float) bodyRot.getZ().floatValue()));
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotation((float) bodyRot.getY().floatValue()));
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotation((float) bodyRot.getX().floatValue()));
                        poseStack.translate(0.0D, -0.7D, 0.0D);
                    }
                }

                // 4) LivingEntityRenderer scale (-1, -1, 1) and offset
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.scale(0.9375F, 0.9375F, 0.9375F);
                poseStack.translate(0.0D, -1.501D, 0.0D);

                // 5) Setup model angles & translate to hand
                setupModelAnim(humanoidModel, entity, partialTicks);
                humanoidModel.translateToHand(arm, poseStack);

                // 6) Exact knuckles/fist center offset:
                // Arm box is 12 pixels tall (-2 to +10 from shoulder pivot). Bottom edge is at 10 px (0.625 blocks).
                // Center of the 4-pixel hand is at Y = 8.0 px (~0.46 - 0.50 blocks), placing particles right in the palm center!
                poseStack.translate(rightHand ? -0.04D : 0.04D, 0.46D, 0.0D);

                Matrix4f matrix = poseStack.last().pose();
                Vector4f posVec = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
                matrix.transform(posVec);

                return new Vec3(posVec.x, posVec.y, posVec.z);
            }
        }

        // Trigonometric fallback
        float bodyYaw = (float) Math.toRadians(entity.yBodyRot);
        double sideOffset = rightHand ? 0.38 : -0.38;
        double forwardOffset = 0.08;
        double x = entity.getX() - Math.sin(bodyYaw) * forwardOffset + Math.cos(bodyYaw) * sideOffset;
        double y = entity.getY() + entity.getBbHeight() * 0.55;
        double z = entity.getZ() + Math.cos(bodyYaw) * forwardOffset + Math.sin(bodyYaw) * sideOffset;
        return new Vec3(x, y, z);
    }

    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity> void setupModelAnim(HumanoidModel<?> model, LivingEntity entity, float partialTicks) {
        ((HumanoidModel<T>) model).setupAnim((T) entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);
    }

    /**
     * Lodestone implementation recreating both JJK standard Divergent Fist aura
     * and the deep space / cosmic void variant with pure celestial atmosphere.
     */
    private static final class LodestoneHelper {
        private static void spawnAura(LivingEntity entity, AuraType auraType) {
            spawnHandAura(entity, true, auraType);
            spawnHandAura(entity, false, auraType);
        }

        private static void spawnHandAura(LivingEntity entity, boolean rightHand, AuraType auraType) {
            Vec3 handPos = getHandPosition(entity, rightHand);
            Vec3 entityMotion = entity.getDeltaMovement();
            var level = entity.level();
            Minecraft mc = Minecraft.getInstance();
            boolean isFirstPersonSelf = (mc.player == entity && mc.options.getCameraType().isFirstPerson());
            float scaleMultiplier = isFirstPersonSelf ? 0.60f : 1.20f;

            if (auraType == AuraType.COSMIC) {
                // =========================================================================
                // TRUE DEEP SPACE / COSMOS AURA (High Visibility & Intensity):
                // 1. Deep Space Void Core (Pitch black cosmic darkness)
                // 2. Dense Cosmic Purple & Deep Indigo Nebula (Pure celestial space gas)
                // 3. Bright Twinkling Diamond White Stars & Starfield
                // 4. Stellar White Sparks & Star Glimmers
                // =========================================================================

                // --- 1. DEEP SPACE VOID CORE (Pitch Black Space Darkness) ---
                int voidCount = isFirstPersonSelf ? 2 : 3;
                for (int i = 0; i < voidCount; i++) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.09 * scaleMultiplier;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.09 * scaleMultiplier;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.09 * scaleMultiplier;

                    WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                            .enableNoClip()
                            .disableCull()
                            .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                            .setScaleData(GenericParticleData.create(0.24f * scaleMultiplier, 0.32f * scaleMultiplier, 0.0f)
                                    .setEasing(Easing.QUAD_OUT, Easing.QUAD_IN)
                                    .build())
                            .setTransparencyData(GenericParticleData.create(isFirstPersonSelf ? 0.88f : 0.98f, 0.0f)
                                    .setEasing(Easing.QUAD_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(0, 0, 0),         // Pure Pitch Black Space
                                    new Color(24, 6, 42)        // Deepest Cosmic Void
                            ).build())
                            .setSpinData(SpinParticleData.create(
                                    (RANDOM.nextFloat() - 0.5f) * 0.40f,
                                    (RANDOM.nextFloat() - 0.5f) * 0.80f
                            ).build())
                            .setLifetime(3 + RANDOM.nextInt(2))
                            .setMotion(
                                    entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.012,
                                    entityMotion.y * 0.8 + 0.018 + RANDOM.nextDouble() * 0.012,
                                    entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.012
                            )
                            .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                }

                // --- 2. DEEP COSMIC NEBULA (Galactic Purple & Indigo Gas) ---
                int nebulaCount = isFirstPersonSelf ? 3 : 5;
                for (int i = 0; i < nebulaCount; i++) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.12 * scaleMultiplier;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.12 * scaleMultiplier;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.12 * scaleMultiplier;

                    WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                            .setFullBrightLighting()
                            .enableNoClip()
                            .disableCull()
                            .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                            .setScaleData(GenericParticleData.create(0.26f * scaleMultiplier, 0.38f * scaleMultiplier, 0.0f)
                                    .setEasing(Easing.QUAD_OUT, Easing.QUAD_IN)
                                    .build())
                            .setTransparencyData(GenericParticleData.create(isFirstPersonSelf ? 0.78f : 0.92f, 0.0f)
                                    .setEasing(Easing.SINE_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(168, 85, 247),    // Radiant Cosmic Violet
                                    new Color(59, 7, 100)       // Deep Indigo Nebula
                            ).build())
                            .setSpinData(SpinParticleData.create(
                                    (RANDOM.nextFloat() - 0.5f) * 0.45f,
                                    (RANDOM.nextFloat() - 0.5f) * 0.90f
                            ).build())
                            .setLifetime(3 + RANDOM.nextInt(2))
                            .setMotion(
                                    entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.015,
                                    entityMotion.y * 0.8 + 0.022 + RANDOM.nextDouble() * 0.015,
                                    entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.015
                            )
                            .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                }

                // --- 3. TWINKLING DIAMOND WHITE STARS (Deep Space Starfield) ---
                int starCount = isFirstPersonSelf ? 3 : 5;
                for (int i = 0; i < starCount; i++) {
                    if (RANDOM.nextFloat() < 0.85f) {
                        double ox = (RANDOM.nextDouble() - 0.5) * 0.16 * scaleMultiplier;
                        double oy = (RANDOM.nextDouble() - 0.5) * 0.16 * scaleMultiplier;
                        double oz = (RANDOM.nextDouble() - 0.5) * 0.16 * scaleMultiplier;

                        WorldParticleBuilder.create(LodestoneParticleRegistry.STAR_PARTICLE)
                                .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                                .setFullBrightLighting()
                                .enableNoClip()
                                .disableCull()
                                .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                                .setScaleData(GenericParticleData.create(0.20f * scaleMultiplier, 0.0f).build())
                                .setColorData(ColorParticleData.create(
                                        new Color(255, 255, 255), // Pure Diamond Star White
                                        new Color(192, 132, 252)  // Soft Stellar Violet Glow
                                ).build())
                                .setLifetime(3 + RANDOM.nextInt(2))
                                .setMotion(
                                        entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.01,
                                        entityMotion.y * 0.8 + 0.012 + RANDOM.nextDouble() * 0.01,
                                        entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.01
                                )
                                .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                    }
                }

                // --- 4. STELLAR WHITE SPARKS (Sharp Star Glimmers) ---
                if (RANDOM.nextFloat() < 0.65f) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.10 * scaleMultiplier;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.10 * scaleMultiplier;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.10 * scaleMultiplier;

                    WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                            .setFullBrightLighting()
                            .enableNoClip()
                            .disableCull()
                            .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                            .setScaleData(GenericParticleData.create(0.18f * scaleMultiplier, 0.0f).build())
                            .setColorData(ColorParticleData.create(
                                    new Color(255, 255, 255), // Pure Diamond White
                                    new Color(216, 180, 254)  // Soft Lavender Spark
                            ).build())
                            .setLifetime(2 + RANDOM.nextInt(2))
                            .setMotion(
                                    entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.035,
                                    entityMotion.y * 0.8 + 0.025,
                                    entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.035
                            )
                            .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                }

            } else {
                // =========================================================================
                // STANDARD JJK DIVERGENT FIST:
                // Electric Cyan & Blue Flame + Pitch Black Cursed Ink + Sparks
                // =========================================================================

                // LAYER 1: Core Electric Cyan & Blue Flame
                int flameCount = isFirstPersonSelf ? 1 : 2;
                for (int i = 0; i < flameCount; i++) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.06 * scaleMultiplier;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.06 * scaleMultiplier;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.06 * scaleMultiplier;

                    WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                            .setFullBrightLighting()
                            .enableNoClip()
                            .disableCull()
                            .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                            .setScaleData(GenericParticleData.create(0.20f * scaleMultiplier, 0.28f * scaleMultiplier, 0.0f)
                                    .setEasing(Easing.QUAD_OUT, Easing.QUAD_IN)
                                    .build())
                            .setTransparencyData(GenericParticleData.create(isFirstPersonSelf ? 0.65f : 0.90f, 0.0f)
                                    .setEasing(Easing.SINE_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(195, 250, 255), // Bright cyan core
                                    new Color(20, 130, 250)   // Deep electric blue flame
                            ).build())
                            .setSpinData(SpinParticleData.create(
                                    (RANDOM.nextFloat() - 0.5f) * 0.35f,
                                    (RANDOM.nextFloat() - 0.5f) * 0.75f
                            ).build())
                            .setLifetime(2 + RANDOM.nextInt(2))
                            .setMotion(
                                    entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.015,
                                    entityMotion.y * 0.8 + 0.02 + RANDOM.nextDouble() * 0.015,
                                    entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.015
                            )
                            .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                }

                // LAYER 2: Pitch Black Cursed Ink
                int inkCount = isFirstPersonSelf ? 1 : 2;
                for (int i = 0; i < inkCount; i++) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.08 * scaleMultiplier;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.08 * scaleMultiplier;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.08 * scaleMultiplier;

                    WorldParticleBuilder.create(LodestoneParticleRegistry.SMOKE_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                            .enableNoClip()
                            .disableCull()
                            .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                            .setScaleData(GenericParticleData.create(0.16f * scaleMultiplier, 0.22f * scaleMultiplier, 0.0f)
                                    .setEasing(Easing.CUBIC_OUT, Easing.QUAD_IN)
                                    .build())
                            .setTransparencyData(GenericParticleData.create(isFirstPersonSelf ? 0.70f : 0.95f, 0.0f)
                                    .setEasing(Easing.QUAD_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(0, 0, 4),      // Deep ink black
                                    new Color(22, 5, 38)     // Dark cursed violet edge
                            ).build())
                            .setSpinData(SpinParticleData.create(
                                    (RANDOM.nextFloat() - 0.5f) * 0.40f,
                                    (RANDOM.nextFloat() - 0.5f) * 0.85f
                            ).build())
                            .setLifetime(2 + RANDOM.nextInt(2))
                            .setMotion(
                                    entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.015,
                                    entityMotion.y * 0.8 + 0.02 + RANDOM.nextDouble() * 0.015,
                                    entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.015
                            )
                            .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                }

                // LAYER 3: White/Cyan Electric Sparks
                if (RANDOM.nextFloat() < 0.65f) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.06 * scaleMultiplier;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.06 * scaleMultiplier;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.06 * scaleMultiplier;

                    WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                            .setFullBrightLighting()
                            .enableNoClip()
                            .disableCull()
                            .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                            .setScaleData(GenericParticleData.create(0.16f * scaleMultiplier, 0.0f)
                                    .setEasing(Easing.EXPO_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(255, 255, 255), // Pure white spark
                                    new Color(85, 225, 255)   // Cyan lightning edge
                            ).build())
                            .setLifetime(2)
                            .setMotion(
                                    entityMotion.x * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.04,
                                    entityMotion.y * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.04,
                                    entityMotion.z * 0.8 + (RANDOM.nextDouble() - 0.5) * 0.04
                            )
                            .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
                }
            }
        }

        private static void spawnBlackFlash(Level level, double x, double y, double z) {
            Vec3 origin = new Vec3(x, y, z);

            // =========================================================================
            // AUTHENTIC JUJUTSU KAISEN BLACK FLASH (KOKUSEN - YUJI ITADORI):
            // Zero giant billboard cubes/squares! Pure sharp jagged black & crimson lightning!
            // 1. Instantaneous Pinpoint Flash (Pure White -> Blood Crimson Spark)
            // 2. High-Speed Shockwave Ring (Sparks blasting radially outward)
            // 3. Dense Jagged Black & Crimson Lightning Bolts (Spatial Fractures)
            // 4. Sub-Branching Lightning Arcs (Forked Tendrils)
            // 5. Crackling Electric Sparks & Embers
            // =========================================================================

            // --- 1. PINPOINT IMPACT FLASH (Zero huge boxes, just sharp intense point flash) ---
            for (int i = 0; i < 4; i++) {
                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setScaleData(GenericParticleData.create(0.35f, 0.0f)
                                .setEasing(Easing.EXPO_IN)
                                .build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 255, 255), // Pure White Center
                                new Color(255, 20, 50)    // Intense Crimson
                        ).build())
                        .setLifetime(6)
                        .spawn(level, x, y, z);
            }

            // --- 2. EXPANDING SHOCKWAVE RING (Crimson & White Spark Circle) ---
            int ringSparks = 24;
            for (int i = 0; i < ringSparks; i++) {
                double angle = (2.0 * Math.PI * i) / ringSparks;
                double speed = 0.42 + RANDOM.nextDouble() * 0.12;
                double vx = Math.cos(angle) * speed;
                double vy = (RANDOM.nextDouble() - 0.5) * 0.10;
                double vz = Math.sin(angle) * speed;

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setFrictionStrength(0.82f)
                        .setScaleData(GenericParticleData.create(0.24f, 0.0f)
                                .setEasing(Easing.EXPO_IN)
                                .build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 255, 255), // White spark
                                new Color(255, 15, 35)    // Crimson spark
                        ).build())
                        .setLifetime(10 + RANDOM.nextInt(6))
                        .setMotion(vx, vy, vz)
                        .spawn(level, x, y, z);
            }

            // --- 3. DENSE JAGGED BLACK & CRIMSON LIGHTNING BOLTS ---
            // 18 distinct lightning bolts radiating outward in 3D space like cracked glass!
            int boltCount = 18;
            for (int b = 0; b < boltCount; b++) {
                // Sphere-distributed direction with random variation
                double theta = RANDOM.nextDouble() * 2.0 * Math.PI;
                double phi = Math.acos(2.0 * RANDOM.nextDouble() - 1.0); // Uniform sphere distribution
                Vec3 dir = new Vec3(
                        Math.sin(phi) * Math.cos(theta),
                        Math.cos(phi),
                        Math.sin(phi) * Math.sin(theta)
                ).normalize();

                double totalLength = 1.6 + RANDOM.nextDouble() * 1.6; // 1.6 to 3.2 blocks length
                int segments = 5 + RANDOM.nextInt(4);                // 5 to 8 jagged kinks
                int baseLifetime = 22 + RANDOM.nextInt(18);          // 1.1 to 2.0 seconds lingering presence

                // Thin, sleek black outer edges (0.16f) encasing vivid crimson energy core (0.11f)
                generateLightningBolt(level, origin, dir, totalLength, segments, baseLifetime, 0.11f, 0.16f, true);
            }

            // --- 4. HIGH-SPEED EXPLOSIVE SPARKS & EMBERS ---
            for (int i = 0; i < 28; i++) {
                double speed = 0.55 + RANDOM.nextDouble() * 0.65;
                double theta = RANDOM.nextDouble() * 2.0 * Math.PI;
                double phi = RANDOM.nextDouble() * Math.PI;
                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.cos(phi) * speed;
                double vz = Math.sin(phi) * Math.sin(theta) * speed;

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setFrictionStrength(0.85f)
                        .setScaleData(GenericParticleData.create(0.24f, 0.0f)
                                .setEasing(Easing.EXPO_IN)
                                .build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 255, 255), // Pure White
                                new Color(255, 15, 35)    // Crimson Red
                        ).build())
                        .setLifetime(16 + RANDOM.nextInt(12))
                        .setMotion(vx, vy, vz)
                        .spawn(level, x, y, z);
            }

            // --- 5. VANILLA ELECTRIC SPARK COMPLEMENTS ---
            for (int i = 0; i < 8; i++) {
                level.addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        x + (RANDOM.nextDouble() - 0.5) * 0.3,
                        y + (RANDOM.nextDouble() - 0.5) * 0.3,
                        z + (RANDOM.nextDouble() - 0.5) * 0.3,
                        (RANDOM.nextDouble() - 0.5) * 0.2,
                        (RANDOM.nextDouble() - 0.5) * 0.2,
                        (RANDOM.nextDouble() - 0.5) * 0.2
                );
            }
        }

        private static void generateLightningBolt(Level level, Vec3 origin, Vec3 dir, double totalLength,
                                                   int segments, int lifetime, float crimsonCoreScale, float blackEdgeScale,
                                                   boolean allowFork) {
            Vec3 curPos = origin;
            double segLen = totalLength / segments;

            // Find two perpendicular reference vectors to create angular zigzagging
            Vec3 arbitrary = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 perp1 = dir.cross(arbitrary).normalize();
            Vec3 perp2 = dir.cross(perp1).normalize();

            for (int s = 0; s < segments; s++) {
                // Jagged angular offset (randomly shifts along the perpendicular plane)
                double j1 = (RANDOM.nextDouble() - 0.5) * segLen * 0.95;
                double j2 = (RANDOM.nextDouble() - 0.5) * segLen * 0.95;
                Vec3 stepVec = dir.scale(segLen)
                        .add(perp1.scale(j1))
                        .add(perp2.scale(j2));

                Vec3 nextPos = curPos.add(stepVec);

                // Draw continuous solid lightning beam along this segment with sleek black coated edges
                drawLightningLine(level, curPos, nextPos, lifetime, crimsonCoreScale, blackEdgeScale);

                // Spawn sleek black coated spark node framing a crimson spark at the kink corner
                if (RANDOM.nextFloat() < 0.65f) {
                    // Outer sleek black crackle node
                    WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                            .enableNoClip()
                            .disableCull()
                            .setScaleData(GenericParticleData.create(blackEdgeScale * 1.15f, 0.0f)
                                    .setEasing(Easing.EXPO_IN)
                                    .build())
                            .setTransparencyData(GenericParticleData.create(1.0f, 0.0f)
                                    .setEasing(Easing.QUAD_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(0, 0, 0),
                                    new Color(15, 0, 3)
                            ).build())
                            .setLifetime(lifetime)
                            .spawn(level, nextPos.x, nextPos.y, nextPos.z);

                    // Inner crimson cursed spark
                    WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                            .setFullBrightLighting()
                            .enableNoClip()
                            .disableCull()
                            .setScaleData(GenericParticleData.create(crimsonCoreScale * 1.15f, 0.0f)
                                    .setEasing(Easing.EXPO_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(255, 20, 45),
                                    new Color(160, 0, 20)
                            ).build())
                            .setLifetime(lifetime)
                            .spawn(level, nextPos.x, nextPos.y, nextPos.z);
                }

                // Sub-branch fork (secondary fractured lightning tendril)
                if (allowFork && s >= 2 && RANDOM.nextFloat() < 0.40f) {
                    double forkAngle = 0.5 + RANDOM.nextDouble() * 0.5;
                    Vec3 forkDir = dir.add(perp1.scale((RANDOM.nextBoolean() ? 1.0 : -1.0) * forkAngle)).normalize();
                    double forkLen = segLen * (1.4 + RANDOM.nextDouble() * 1.4);
                    int forkLifetime = (int) (lifetime * 0.8);
                    generateLightningBolt(level, nextPos, forkDir, forkLen, 3, forkLifetime,
                            crimsonCoreScale * 0.85f, blackEdgeScale * 0.85f, false);
                }

                curPos = nextPos;
            }
        }

        private static void drawLightningLine(Level level, Vec3 p1, Vec3 p2, int lifetime,
                                              float crimsonCoreScale, float blackEdgeScale) {
            double dist = p1.distanceTo(p2);
            int steps = Math.max(1, (int) Math.ceil(dist / 0.045)); // Dense 4.5cm interval

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double px = p1.x + (p2.x - p1.x) * t;
                double py = p1.y + (p2.y - p1.y) * t;
                double pz = p1.z + (p2.z - p1.z) * t;

                // Micro electrical jitter
                double jx = (RANDOM.nextDouble() - 0.5) * 0.012;
                double jy = (RANDOM.nextDouble() - 0.5) * 0.012;
                double jz = (RANDOM.nextDouble() - 0.5) * 0.012;

                // 1. PITCH BLACK OUTER BORDER COATING (Kenarları Siyah Kaplı)
                // Thick transparent black sheath outlining and enclosing the lightning bolt
                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                        .enableNoClip()
                        .disableCull()
                        .setScaleData(GenericParticleData.create(blackEdgeScale, 0.0f)
                                .setEasing(Easing.EXPO_IN)
                                .build())
                        .setTransparencyData(GenericParticleData.create(1.0f, 0.0f)
                                .setEasing(Easing.QUAD_IN)
                                .build())
                        .setColorData(ColorParticleData.create(
                                new Color(0, 0, 0),       // Pure Pitch Black Void
                                new Color(12, 0, 3)       // Dark Void Cursed Shadow
                        ).build())
                        .setLifetime(lifetime)
                        .spawn(level, px + jx, py + jy, pz + jz);

                // 2. INNER CRIMSON CURSED ENERGY VEIN (Additive - nestled inside the black edges)
                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setScaleData(GenericParticleData.create(crimsonCoreScale, 0.0f)
                                .setEasing(Easing.EXPO_IN)
                                .build())
                        .setTransparencyData(GenericParticleData.create(0.95f, 0.0f)
                                .setEasing(Easing.QUAD_IN)
                                .build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 10, 35),   // Vivid Blood Crimson
                                new Color(150, 0, 15)     // Deep Cursed Red
                        ).build())
                        .setLifetime(lifetime)
                        .spawn(level, px + jx, py + jy, pz + jz);

                // 3. WHITE-HOT CENTER FILAMENT (Intense energy center)
                if (i % 2 == 0) {
                    WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                            .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                            .setFullBrightLighting()
                            .enableNoClip()
                            .disableCull()
                            .setScaleData(GenericParticleData.create(crimsonCoreScale * 0.55f, 0.0f)
                                    .setEasing(Easing.EXPO_IN)
                                    .build())
                            .setColorData(ColorParticleData.create(
                                    new Color(255, 255, 255), // Blinding White Spark
                                    new Color(255, 80, 100)   // Bright Rose Crimson
                            ).build())
                            .setLifetime(lifetime)
                            .spawn(level, px + jx, py + jy, pz + jz);
                }
            }
        }

        private static void spawnNormalHitSparks(Level level, double x, double y, double z, boolean isCosmic) {
            for (int i = 0; i < 14; i++) {
                double vx = (RANDOM.nextDouble() - 0.5) * 0.38;
                double vy = (RANDOM.nextDouble() - 0.5) * 0.38;
                double vz = (RANDOM.nextDouble() - 0.5) * 0.38;

                Color c1 = isCosmic ? new Color(168, 85, 247) : new Color(85, 225, 255);
                Color c2 = isCosmic ? new Color(59, 7, 100) : new Color(20, 30, 80);

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .setFrictionStrength(0.88f)
                        .setScaleData(GenericParticleData.create(0.24f, 0.0f).build())
                        .setColorData(ColorParticleData.create(c1, c2).build())
                        .setLifetime(8 + RANDOM.nextInt(6))
                        .setMotion(vx, vy, vz)
                        .spawn(level, x, y, z);
            }
        }
    }

    public static void handleHitEffectPacket(double x, double y, double z, int fistIndex, boolean isCosmic, int targetEntityId) {
        spawnHitEffect(x, y, z, fistIndex, isCosmic);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.Entity ent = mc.level.getEntity(targetEntityId);
            if (ent instanceof LivingEntity living) {
                com.algeron.cosmicarmor.combat.GloveCombatHandler.applyClientStun(living, 500);
            }
        }
    }

    public static void spawnHitEffect(double x, double y, double z, int fistIndex, boolean isCosmic) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Level level = mc.level;

        // DIRECT LOCAL CLIENT SOUND PLAYBACK (100% guaranteed, instant, zero dropped packets!)
        if (isCosmic && fistIndex == 3) {
            level.playLocalSound(x, y, z, ModSounds.BF.get(), SoundSource.PLAYERS, 3.0F, 1.0F, false);
        } else {
            level.playLocalSound(x, y, z, ModSounds.FIST.get(), SoundSource.PLAYERS, 2.5F, 1.0F, false);
        }

        if (isCosmic && fistIndex == 3) {
            // AUTHENTIC ANIME BLACK FLASH (Kokusen)!
            if (ModList.get().isLoaded("lodestone")) {
                LodestoneHelper.spawnBlackFlash(level, x, y, z);
            } else {
                spawnVanillaBlackFlash(level, x, y, z);
            }
        } else {
            // Normal hit impact sparks
            if (ModList.get().isLoaded("lodestone")) {
                LodestoneHelper.spawnNormalHitSparks(level, x, y, z, isCosmic);
            } else {
                level.addParticle(ParticleTypes.CRIT, x, y, z, 0.0, 0.1, 0.0);
            }
        }
    }

    private static void spawnVanillaBlackFlash(Level level, double x, double y, double z) {
        level.addParticle(ParticleTypes.SONIC_BOOM, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 40; i++) {
            double vx = (RANDOM.nextDouble() - 0.5) * 0.65;
            double vy = (RANDOM.nextDouble() - 0.5) * 0.65;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.65;
            level.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, vx, vy, vz);
            level.addParticle(ParticleTypes.DAMAGE_INDICATOR, x, y, z, vx, vy, vz);
            level.addParticle(ParticleTypes.CRIT, x, y, z, vx, vy, vz);
        }
    }
}
