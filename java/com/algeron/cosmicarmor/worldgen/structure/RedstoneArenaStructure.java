package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.worldgen.biome.RedstoneDepthsBiomeSource;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.slf4j.Logger;

import java.util.Optional;

public class RedstoneArenaStructure extends Structure {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<RedstoneArenaStructure> CODEC = simpleCodec(RedstoneArenaStructure::new);

    public static final int DEFAULT_GENERATION_Y = -42;

    public RedstoneArenaStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos targetChunk = RedstoneDepthsBiomeSource.getUniqueCenterChunk(context.seed());
        if (!context.chunkPos().equals(targetChunk)) {
            return Optional.empty();
        }

        int chunkX = targetChunk.getMiddleBlockX();
        int chunkZ = targetChunk.getMiddleBlockZ();
        BlockPos spawnPos = new BlockPos(chunkX, DEFAULT_GENERATION_Y, chunkZ);

        LOGGER.info("[CosmicArmor-WorldGen] StructureStart CREATED for Redstone Arena at candidate chunk [{}, {}] (WorldPos: {}, {}, {})",
                targetChunk.x, targetChunk.z, chunkX, DEFAULT_GENERATION_Y, chunkZ);

        return Optional.of(new GenerationStub(spawnPos, builder -> {
            generatePieces(builder, spawnPos);
        }));
    }

    private void generatePieces(StructurePiecesBuilder builder, BlockPos centerPos) {
        int centerChunkX = centerPos.getX() >> 4;
        int centerChunkZ = centerPos.getZ() >> 4;
        int chunkRadius = 11; // ~176 blok yarıçap

        int pieceCount = 0;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (dx * dx + dz * dz > (chunkRadius + 1) * (chunkRadius + 1)) continue;
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                builder.addPiece(new RedstoneArenaStructurePiece(chunkX, chunkZ, centerPos));
                pieceCount++;
            }
        }
        LOGGER.info("[CosmicArmor-WorldGen] Registered {} chunk pieces across 160-block radius for Redstone Arena", pieceCount);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.REDSTONE_ARENA_STRUCTURE.get();
    }
}
