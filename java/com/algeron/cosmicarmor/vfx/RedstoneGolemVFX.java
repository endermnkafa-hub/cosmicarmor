package com.algeron.cosmicarmor.vfx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;
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

public final class RedstoneGolemVFX {
    private static final Random RANDOM = new Random();

    public static void spawnLaserCharge(Level level, Vec3 handPos, int chargeTick) {
        if (!level.isClientSide) return;

        if (ModList.get().isLoaded("lodestone")) {
            LodestoneHelper.spawnLaserCharge(level, handPos, chargeTick);
        } else {
            spawnVanillaLaserCharge(level, handPos);
        }
    }

    public static void spawnLaserFire(Level level, Vec3 handPos, Vec3 shootDir) {
        if (!level.isClientSide) return;

        if (ModList.get().isLoaded("lodestone")) {
            LodestoneHelper.spawnLaserFire(level, handPos, shootDir);
        }
    }

    public static void spawnLaserOrbTrail(Level level, Vec3 pos, Vec3 motion) {
        if (!level.isClientSide) return;

        if (ModList.get().isLoaded("lodestone")) {
            LodestoneHelper.spawnLaserOrbTrail(level, pos, motion);
        } else {
            level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.8F),
                    pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    public static void spawnLaserImpact(Level level, Vec3 hitPos) {
        if (!level.isClientSide) return;

        if (ModList.get().isLoaded("lodestone")) {
            LodestoneHelper.spawnLaserImpact(level, hitPos);
        }
    }

    private static void spawnVanillaLaserCharge(Level level, Vec3 handPos) {
        for (int i = 0; i < 6; i++) {
            double ox = (RANDOM.nextDouble() - 0.5D) * 3.0D;
            double oy = (RANDOM.nextDouble() - 0.5D) * 3.0D;
            double oz = (RANDOM.nextDouble() - 0.5D) * 3.0D;
            level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.6F),
                    handPos.x + ox, handPos.y + oy, handPos.z + oz,
                    -ox * 0.12D, -oy * 0.12D, -oz * 0.12D);
        }
    }

    private static final class LodestoneHelper {
        private static void spawnLaserCharge(Level level, Vec3 handPos, int chargeTick) {
            float progress = Math.min(1.0F, (chargeTick - 10) / 15.0F);

            // 1. Etraftan ele doğru çekilen dairesel kızıltaş enerji parçacıkları (Inward Suction Wisps)
            int suctionCount = 6 + RANDOM.nextInt(4);
            for (int i = 0; i < suctionCount; i++) {
                double r = 1.8D + RANDOM.nextDouble() * 1.4D;
                double theta = RANDOM.nextDouble() * 2.0D * Math.PI;
                double phi = Math.acos(2.0D * RANDOM.nextDouble() - 1.0D);

                double ox = r * Math.sin(phi) * Math.cos(theta);
                double oy = r * Math.sin(phi) * Math.sin(theta);
                double oz = r * Math.cos(phi);

                double speed = 0.12D + 0.05D * progress;

                WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setDiscardFunction(SimpleParticleOptions.ParticleDiscardFunctionType.INVISIBLE)
                        .setScaleData(GenericParticleData.create(0.28F, 0.38F, 0.0F)
                                .setEasing(Easing.QUAD_OUT, Easing.QUAD_IN)
                                .build())
                        .setTransparencyData(GenericParticleData.create(0.85F, 0.0F)
                                .setEasing(Easing.SINE_IN)
                                .build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 35, 45),     // Parlak kızıltaş kırmızısı
                                new Color(180, 0, 15)       // Derin kor kırmızı
                        ).build())
                        .setSpinData(SpinParticleData.create(
                                (RANDOM.nextFloat() - 0.5F) * 0.5F,
                                (RANDOM.nextFloat() - 0.5F) * 0.9F
                        ).build())
                        .setLifetime(6 + RANDOM.nextInt(4))
                        .setMotion(-ox * speed, -oy * speed, -oz * speed)
                        .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
            }

            // 2. İçe doğru süzülen kızıltaş elektrik kıvılcımları
            if (RANDOM.nextFloat() < 0.75F) {
                double r = 1.5D + RANDOM.nextDouble() * 1.0D;
                double theta = RANDOM.nextDouble() * 2.0D * Math.PI;
                double phi = RANDOM.nextDouble() * Math.PI;
                double ox = r * Math.sin(phi) * Math.cos(theta);
                double oy = r * Math.cos(phi);
                double oz = r * Math.sin(phi) * Math.sin(theta);

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setScaleData(GenericParticleData.create(0.22F, 0.0F).build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 230, 200),   // Beyaz-altın kıvılcım çekirdeği
                                new Color(255, 25, 35)      // Kızıltaş dış hare
                        ).build())
                        .setLifetime(5)
                        .setMotion(-ox * 0.16D, -oy * 0.16D, -oz * 0.16D)
                        .spawn(level, handPos.x + ox, handPos.y + oy, handPos.z + oz);
            }

            // 3. Merkezde biriken, gittikçe büyüyen yuvarlak kızıltaş enerji küresi
            float coreScale = 0.30F + 0.55F * progress;
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                    .setFullBrightLighting()
                    .enableNoClip()
                    .disableCull()
                    .setScaleData(GenericParticleData.create(coreScale, 0.0F).build())
                    .setTransparencyData(GenericParticleData.create(0.92F, 0.0F).build())
                    .setColorData(ColorParticleData.create(
                            new Color(255, 240, 240),       // Beyaz akkor merkez
                            new Color(255, 20, 30)          // Yoğun kızıltaş alevi
                    ).build())
                    .setLifetime(2)
                    .spawn(level, handPos.x, handPos.y, handPos.z);
        }

        private static void spawnLaserFire(Level level, Vec3 handPos, Vec3 shootDir) {
            // Patlama / fırlatış anı şok dalgası
            for (int i = 0; i < 20; i++) {
                double speed = 0.35D + RANDOM.nextDouble() * 0.45D;
                double theta = RANDOM.nextDouble() * 2.0D * Math.PI;
                double phi = RANDOM.nextDouble() * Math.PI;
                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.cos(phi) * speed;
                double vz = Math.sin(phi) * Math.sin(theta) * speed;

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setFrictionStrength(0.85F)
                        .setScaleData(GenericParticleData.create(0.28F, 0.0F).build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 255, 255),
                                new Color(255, 25, 40)
                        ).build())
                        .setLifetime(10 + RANDOM.nextInt(8))
                        .setMotion(vx, vy, vz)
                        .spawn(level, handPos.x, handPos.y, handPos.z);
            }
        }

        private static void spawnLaserOrbTrail(Level level, Vec3 pos, Vec3 motion) {
            // Uçuş sırasında kürenin arkasında kalan parıldayan iz
            WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                    .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                    .setFullBrightLighting()
                    .enableNoClip()
                    .disableCull()
                    .setScaleData(GenericParticleData.create(0.35F, 0.0F)
                            .setEasing(Easing.EXPO_IN)
                            .build())
                    .setColorData(ColorParticleData.create(
                            new Color(255, 220, 220),
                            new Color(240, 20, 30)
                    ).build())
                    .setLifetime(5)
                    .spawn(level, pos.x, pos.y, pos.z);

            if (RANDOM.nextFloat() < 0.50F) {
                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setScaleData(GenericParticleData.create(0.18F, 0.0F).build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 255, 255),
                                new Color(255, 30, 45)
                        ).build())
                        .setLifetime(4)
                        .setMotion(
                                motion.x * -0.2D + (RANDOM.nextDouble() - 0.5D) * 0.04D,
                                motion.y * -0.2D + (RANDOM.nextDouble() - 0.5D) * 0.04D,
                                motion.z * -0.2D + (RANDOM.nextDouble() - 0.5D) * 0.04D
                        )
                        .spawn(level, pos.x, pos.y, pos.z);
            }
        }

        private static void spawnLaserImpact(Level level, Vec3 hitPos) {
            for (int i = 0; i < 28; i++) {
                double speed = 0.40D + RANDOM.nextDouble() * 0.50D;
                double theta = RANDOM.nextDouble() * 2.0D * Math.PI;
                double phi = RANDOM.nextDouble() * Math.PI;
                double vx = Math.sin(phi) * Math.cos(theta) * speed;
                double vy = Math.cos(phi) * speed;
                double vz = Math.sin(phi) * Math.sin(theta) * speed;

                WorldParticleBuilder.create(LodestoneParticleRegistry.SPARK_PARTICLE)
                        .setRenderType(LodestoneWorldParticleRenderType.ADDITIVE)
                        .setFullBrightLighting()
                        .enableNoClip()
                        .disableCull()
                        .setFrictionStrength(0.82F)
                        .setScaleData(GenericParticleData.create(0.32F, 0.0F).build())
                        .setColorData(ColorParticleData.create(
                                new Color(255, 255, 255),
                                new Color(255, 20, 35)
                        ).build())
                        .setLifetime(14 + RANDOM.nextInt(8))
                        .setMotion(vx, vy, vz)
                        .spawn(level, hitPos.x, hitPos.y, hitPos.z);
            }
        }
    }
}
