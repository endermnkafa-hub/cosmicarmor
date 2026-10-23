package com.algeron.cosmicarmor.worldgen.structure;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;

public class SchematicLoader {

    public static class SchematicData {
        public final short width;
        public final short height;
        public final short length;
        public final BlockState[] palette;
        public final byte[] blockData;
        public BlockPos floorCenterOffset = null;

        public SchematicData(short width, short height, short length, BlockState[] palette, byte[] blockData) {
            this.width = width;
            this.height = height;
            this.length = length;
            this.palette = palette;
            this.blockData = blockData;
        }
    }

    public static SchematicData load(InputStream inputStream) {
        try {
            CompoundTag root = NbtIo.readCompressed(inputStream);
            short width = root.getShort("Width");
            short height = root.getShort("Height");
            short length = root.getShort("Length");

            CompoundTag paletteTag = root.getCompound("Palette");
            int paletteSize = paletteTag.getAllKeys().size();
            BlockState[] palette = new BlockState[paletteSize];

            for (String key : paletteTag.getAllKeys()) {
                int id = paletteTag.getInt(key);
                BlockState state;
                try {
                    state = BlockStateParser.parseForBlock(
                            BuiltInRegistries.BLOCK.asLookup(),
                            new StringReader(key),
                            false
                    ).blockState();
                } catch (Exception e) {
                    state = Blocks.AIR.defaultBlockState();
                }
                if (id >= 0 && id < paletteSize) {
                    palette[id] = state;
                }
            }

            byte[] blockData = root.getByteArray("BlockData");
            SchematicData data = new SchematicData(width, height, length, palette, blockData);

            // İç kubbenin taban zemin yüzeyi merkezini tespit et:
            // X = width / 2 (55), Z = length / 2 (56)
            int centerX = width / 2;
            int centerZ = length / 2;
            int floorY = 2; // Varsayılan zemin yüzeyi

            for (int y = 0; y < height / 2; y++) {
                int idxCurr = (y * length + centerZ) * width + centerX;
                int idxAbove = ((y + 1) * length + centerZ) * width + centerX;

                if (idxCurr < blockData.length && idxAbove < blockData.length) {
                    int idCurr = blockData[idxCurr] & 0xFF;
                    int idAbove = blockData[idxAbove] & 0xFF;

                    boolean currSolid = idCurr < palette.length && palette[idCurr] != null && !palette[idCurr].isAir() && !palette[idCurr].is(Blocks.DIAMOND_BLOCK);
                    boolean aboveAir = idAbove >= palette.length || palette[idAbove] == null || palette[idAbove].isAir() || palette[idAbove].is(Blocks.DIAMOND_BLOCK);

                    if (currSolid && aboveAir) {
                        floorY = y + 1;
                        break;
                    }
                }
            }

            data.floorCenterOffset = new BlockPos(centerX, floorY, centerZ);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BlockPos paste(ServerLevel level, BlockPos origin, SchematicData data) {
        if (data == null) return origin;

        for (int y = 0; y < data.height; y++) {
            for (int z = 0; z < data.length; z++) {
                for (int x = 0; x < data.width; x++) {
                    int index = (y * data.length + z) * data.width + x;
                    if (index >= data.blockData.length) continue;

                    int id = data.blockData[index] & 0xFF;
                    if (id >= data.palette.length) continue;

                    BlockState state = data.palette[id];
                    if (state == null) continue;

                    // WorldEdit seçiminde iskele olarak konulan elmas blokları tamamen atla, dünyaya koyma!
                    if (state.is(Blocks.DIAMOND_BLOCK)) {
                        continue;
                    }

                    BlockPos targetPos = origin.offset(x, y, z);
                    if (!state.isAir()) {
                        level.setBlock(targetPos, state, 2);
                    }
                }
            }
        }

        // İç kubbenin taban merkez yüzeyinin dünyadaki kesin konumu
        return origin.offset(data.floorCenterOffset != null ? data.floorCenterOffset : new BlockPos(data.width / 2, 2, data.length / 2));
    }
}
