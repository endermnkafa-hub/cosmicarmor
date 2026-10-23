package com.algeron.cosmicarmor.worldgen.structure;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class RedstoneArenaStructure extends Structure {
    public static final Codec<RedstoneArenaStructure> CODEC = simpleCodec(RedstoneArenaStructure::new);

    // Bedrock (-64) üstü derin yeraltı katmanı (Y = -42). Zemin Y: -50, Tepe Y: -10 civarındadır.
    public static final int DEFAULT_GENERATION_Y = -42;

    public RedstoneArenaStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int chunkX = context.chunkPos().getMiddleBlockX();
        int chunkZ = context.chunkPos().getMiddleBlockZ();
        BlockPos spawnPos = new BlockPos(chunkX, DEFAULT_GENERATION_Y, chunkZ);

        return Optional.of(new GenerationStub(spawnPos, builder -> {
            generatePieces(builder, spawnPos);
        }));
    }

    private void generatePieces(StructurePiecesBuilder builder, BlockPos pos) {
        builder.addPiece(new RedstoneArenaStructurePiece(pos));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.REDSTONE_ARENA_STRUCTURE.get();
    }
}
