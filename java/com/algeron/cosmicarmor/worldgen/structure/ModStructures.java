package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, CosmicArmorMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, CosmicArmorMod.MOD_ID);

    public static final RegistryObject<StructureType<RedstoneArenaStructure>> REDSTONE_ARENA_STRUCTURE =
            STRUCTURE_TYPES.register("redstone_arena", () -> () -> RedstoneArenaStructure.CODEC);

    public static final RegistryObject<StructurePieceType> REDSTONE_ARENA_PIECE =
            STRUCTURE_PIECE_TYPES.register("redstone_arena_piece", () -> RedstoneArenaStructurePiece::new);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECE_TYPES.register(eventBus);
    }
}
