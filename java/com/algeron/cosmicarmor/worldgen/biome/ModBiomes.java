package com.algeron.cosmicarmor.worldgen.biome;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModBiomes {
    public static final ResourceKey<Biome> REDSTONE_DEPTHS = ResourceKey.create(
            Registries.BIOME,
            new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_depths")
    );

    public static final ResourceKey<Biome> REDSTONE_LABYRINTH = ResourceKey.create(
            Registries.BIOME,
            new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_labyrinth")
    );

    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, CosmicArmorMod.MOD_ID);

    public static final RegistryObject<Codec<RedstoneDepthsBiomeSource>> REDSTONE_DEPTHS_SOURCE =
            BIOME_SOURCES.register("redstone_depths", () -> RedstoneDepthsBiomeSource.CODEC);

    public static void register(IEventBus bus) {
        BIOME_SOURCES.register(bus);
    }
}
