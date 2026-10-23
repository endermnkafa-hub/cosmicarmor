package com.algeron.cosmicarmor.worldgen.biome;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BiomeSourceInjector {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            injectIntoLevel(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld != null) {
            injectIntoLevel(overworld);
        }
    }

    public static void injectIntoLevel(ServerLevel level) {
        try {
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            BiomeSource current = generator.getBiomeSource();
            if (current instanceof RedstoneDepthsBiomeSource) {
                return; // Zaten entegre edilmiş
            }

            Holder<Biome> depthsHolder = level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(ModBiomes.REDSTONE_DEPTHS);

            long seed = level.getSeed();
            RedstoneDepthsBiomeSource customSource = new RedstoneDepthsBiomeSource(current, depthsHolder, seed);

            // ChunkGenerator içindeki BiomeSource alanını tip-güvenli reflection ile güncelle
            boolean injected = false;
            for (Field f : ChunkGenerator.class.getDeclaredFields()) {
                if (BiomeSource.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(generator, customSource);
                    injected = true;
                    LOGGER.info("[CosmicArmor] Successfully injected RedstoneDepthsBiomeSource into Overworld ChunkGenerator!");
                    break;
                }
            }

            if (!injected) {
                LOGGER.warn("[CosmicArmor] Could not find BiomeSource field in ChunkGenerator");
            }

            // ChunkMap içindeki chunkGeneratorState alanını da yeni customSource ile yenile
            try {
                ChunkGeneratorStructureState newState = generator.createState(
                        level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),
                        level.getChunkSource().randomState(),
                        seed
                );
                for (Field f : ChunkMap.class.getDeclaredFields()) {
                    if (ChunkGeneratorStructureState.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        f.set(level.getChunkSource().chunkMap, newState);
                        LOGGER.info("[CosmicArmor] Successfully refreshed ChunkGeneratorStructureState in ChunkMap!");
                        break;
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn("[CosmicArmor] Could not refresh ChunkGeneratorStructureState in ChunkMap: {}", ex.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("[CosmicArmor] Failed to inject RedstoneDepthsBiomeSource", e);
        }
    }
}
