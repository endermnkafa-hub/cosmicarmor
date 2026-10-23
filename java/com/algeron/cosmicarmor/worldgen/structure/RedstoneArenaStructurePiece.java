package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.entity.ModEntities;
import com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class RedstoneArenaStructurePiece extends StructurePiece {

    private final BlockPos centerPos;

    public RedstoneArenaStructurePiece(BlockPos pos) {
        super(ModStructures.REDSTONE_ARENA_PIECE.get(), 0, new BoundingBox(
                pos.getX() - 160, pos.getY() - 15, pos.getZ() - 160,
                pos.getX() + 160, pos.getY() + 105, pos.getZ() + 160
        ));
        this.centerPos = pos;
        this.setOrientation(null);
    }

    public RedstoneArenaStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.REDSTONE_ARENA_PIECE.get(), tag);
        this.centerPos = new BlockPos(
                this.boundingBox.minX() + 160,
                this.boundingBox.minY() + 15,
                this.boundingBox.minZ() + 160
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        SchematicLoader.SchematicData data = ArenaManager.getOrLoadSchematic();
        if (data == null) return;

        // Şematik (Küre) başlangıç noktası: Merkeze göre hizalanmış
        int originX = this.centerPos.getX() - data.width / 2;
        int originY = this.centerPos.getY() - 10;
        int originZ = this.centerPos.getZ() - data.length / 2;
        BlockPos origin = new BlockPos(originX, originY, originZ);

        // 2. KATMAN 1: İç ve Dış Küre Bloklarını Şematikten Mevcut Chunk İçine Yerleştir
        int minX = Math.max(0, box.minX() - originX);
        int maxX = Math.min(data.width - 1, box.maxX() - originX);
        int minY = Math.max(0, box.minY() - originY);
        int maxY = Math.min(data.height - 1, box.maxY() - originY);
        int minZ = Math.max(0, box.minZ() - originZ);
        int maxZ = Math.min(data.length - 1, box.maxZ() - originZ);

        if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        int index = (y * data.length + z) * data.width + x;
                        if (index >= data.blockData.length) continue;

                        int id = data.blockData[index] & 0xFF;
                        if (id >= data.palette.length) continue;

                        BlockState state = data.palette[id];
                        if (state == null || state.isAir() || state.is(Blocks.DIAMOND_BLOCK)) continue;

                        targetPos.set(originX + x, originY + y, originZ + z);
                        level.setBlock(targetPos, state, 2);
                    }
                }
            }
        }

        // 3. Su ve Doğal Sıvı Koruması: Kürenin içine sızan su kaynaklarını temizle
        int centerX = this.centerPos.getX();
        int centerZ = this.centerPos.getZ();
        int floorY = originY + (data.floorCenterOffset != null ? data.floorCenterOffset.getY() : 2);
        BlockPos statuePos = new BlockPos(centerX, floorY, centerZ);

        // 4. Merkez Chunk: Heykeli dik ve ArenaManager kaydını yap
        if (chunkPos.x == (centerX >> 4) && chunkPos.z == (centerZ >> 4)) {
            RedstoneGolemStatueEntity statue = new RedstoneGolemStatueEntity(ModEntities.REDSTONE_GOLEM_STATUE.get(), level.getLevel());
            statue.setPos(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 0.5D);
            statue.setYRot(180.0F);
            statue.setPersistenceRequired();
            level.addFreshEntity(statue);

            if (level.getLevel() != null) {
                ArenaManager.get(level.getLevel()).registerGeneratedArena(originX, originY, originZ, statuePos);
            }
        }

        // 5. 4 AŞAMALI PROSEDÜREL MAĞARA AĞI:
        // Aşama 2 (Lanetli tuzaklar), Aşama 3 (Basit makineler) ve Aşama 4 (Doğal kızıltaş mağaraları)
        RedstoneDungeonGenerator.generateSurroundingLayers(level, box, origin, data);
    }
}
