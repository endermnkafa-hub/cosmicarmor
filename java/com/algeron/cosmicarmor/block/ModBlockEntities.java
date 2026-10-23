package com.algeron.cosmicarmor.block;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CosmicArmorMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<RedstoneLandmineBlockEntity>> REDSTONE_LANDMINE =
            BLOCK_ENTITIES.register("redstone_landmine",
                    () -> BlockEntityType.Builder.of(RedstoneLandmineBlockEntity::new, ModBlocks.REDSTONE_LANDMINE.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
