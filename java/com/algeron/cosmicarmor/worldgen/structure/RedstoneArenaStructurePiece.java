package com.algeron.cosmicarmor.worldgen.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class RedstoneArenaStructurePiece extends StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean LOGGED_FIRST_POSTPROCESS = new AtomicBoolean(false);

    private final BlockPos centerPos;

    public RedstoneArenaStructurePiece(BlockPos pos) {
        super(ModStructures.REDSTONE_ARENA_PIECE.get(), 0, new BoundingBox(
                pos.getX() - 170, -64, pos.getZ() - 170,
                pos.getX() + 170, -16, pos.getZ() + 170
        ));
        this.centerPos = pos;
        this.setOrientation(null);
    }

    public RedstoneArenaStructurePiece(int chunkX, int chunkZ, BlockPos centerPos) {
        super(ModStructures.REDSTONE_ARENA_PIECE.get(), 0, new BoundingBox(
                chunkX << 4, -64, chunkZ << 4,
                (chunkX << 4) + 15, -16, (chunkZ << 4) + 15
        ));
        this.centerPos = centerPos;
        this.setOrientation(null);
    }

    public RedstoneArenaStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.REDSTONE_ARENA_PIECE.get(), tag);
        if (tag.contains("CenterX")) {
            this.centerPos = new BlockPos(tag.getInt("CenterX"), tag.getInt("CenterY"), tag.getInt("CenterZ"));
        } else {
            this.centerPos = new BlockPos(
                    (this.boundingBox.minX() + this.boundingBox.maxX()) / 2,
                    -42,
                    (this.boundingBox.minZ() + this.boundingBox.maxZ()) / 2
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CenterX", this.centerPos.getX());
        tag.putInt("CenterY", this.centerPos.getY());
        tag.putInt("CenterZ", this.centerPos.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (LOGGED_FIRST_POSTPROCESS.compareAndSet(false, true)) {
            LOGGER.info("[CosmicArmor-WorldGen] postProcess CALLED for chunk [{}, {}]! centerPos={}, seed={}",
                    chunkPos.x, chunkPos.z, this.centerPos, level.getSeed());
        }
        ProceduralRedstoneCaveSystem.generate(level, box, chunkPos, this.centerPos, level.getSeed());
    }
}
