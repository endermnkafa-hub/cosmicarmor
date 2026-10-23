package com.algeron.cosmicarmor.block;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CosmicArmorMod.MOD_ID);

    public static final RegistryObject<Block> REDSTONE_LANDMINE =
            BLOCKS.register("redstone_landmine", RedstoneLandmineBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
