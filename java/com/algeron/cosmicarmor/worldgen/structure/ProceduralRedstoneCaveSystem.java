package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.block.ModBlocks;
import com.algeron.cosmicarmor.entity.ModEntities;
import com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kızıltaş Derinlikleri Prosedürel 3D Elipsoid Mağara ve Boss Arenası Sistemi.
 *
 * Aşama 1 (r <= 54): Basık 3D Elipsoid Monolitik Dış Küre ve İç Savaş Arenası
 * Aşama 2 (54 < r <= 85): Tuzaklı Katakomplar (Geniş birbirine bağlı labirent salonlar, lav hendekleri, mayınlar, tuzaklar)
 * Aşama 3 (85 < r <= 115): Kadim Kızıltaş Makineleri (Geniş atölye galerileri, pistonlar, asılı lambalar, makineler)
 * Aşama 4 (115 < r <= 165): Devasa Katedral Mağaraları (25-34 blok tavan, dev kaya sütunları, yoğun kızıltaş cevher damarları)
 */
public class ProceduralRedstoneCaveSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Debug Sayaçları
    public static final AtomicInteger TOTAL_CHUNKS_PROCESSED = new AtomicInteger();
    public static final AtomicLong TOTAL_BLOCKS_CARVED = new AtomicLong();
    public static final AtomicLong STAGE1_BLOCKS = new AtomicLong();
    public static final AtomicLong STAGE2_BLOCKS = new AtomicLong();
    public static final AtomicLong STAGE3_BLOCKS = new AtomicLong();
    public static final AtomicLong STAGE4_BLOCKS = new AtomicLong();

    public static final double R_STAGE1 = 54.0D;
    public static final double R_STAGE2 = 85.0D;
    public static final double R_STAGE3 = 115.0D;
    public static final double R_STAGE4 = 165.0D;

    public static final int ARENA_FLOOR_Y = -50;
    public static final int ELLIPSOID_CENTER_Y = -38;
    public static final double ELLIPSOID_OUTER_V = 22.0D; // Y: -60 ile -16 arası
    public static final double ELLIPSOID_INNER_V = 19.0D; // Y: -57 ile -19 arası
    public static final double ELLIPSOID_OUTER_H = 54.0D;
    public static final double ELLIPSOID_INNER_H = 48.0D;

    /**
     * Chunk bounding box sınırları içindeki blokları aşamalara göre oyar ve inşa eder.
     */
    public static void generate(WorldGenLevel level, BoundingBox box, ChunkPos chunkPos, BlockPos center, long worldSeed) {
        int centerX = center.getX();
        int centerZ = center.getZ();

        double breachAngle = getBreachAngle(worldSeed);

        int minX = box.minX();
        int maxX = box.maxX();
        int minZ = box.minZ();
        int maxZ = box.maxZ();

        long chunkModifiedCount = 0;
        long s1 = 0, s2 = 0, s3 = 0, s4 = 0;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                double distXZ = Math.sqrt(dx * dx + dz * dz);

                if (distXZ > R_STAGE4 + 5.0D) continue;

                double angle = Math.atan2(dz, dx);
                double angleDeg = ((Math.toDegrees(angle) % 360.0D) + 360.0D) % 360.0D;

                // =========================================================
                // AŞAMA 1: 3D ELİPSOİD DIŞ KÜRE VE İÇ ARENA (r <= 54.0)
                // =========================================================
                if (distXZ <= R_STAGE1) {
                    long mod = generateStage1Column(level, pos, x, z, dx, dz, distXZ, angle, breachAngle, worldSeed);
                    s1 += mod;
                    chunkModifiedCount += mod;
                    continue;
                }

                // =========================================================
                // AŞAMALAR 2, 3, 4: PROSEDÜREL GENİŞ MAĞARA AĞI (54.0 < r <= 165.0)
                // =========================================================
                int stage = getStage(distXZ);
                boolean inCavern = isInsideCavern(x, z, dx, dz, distXZ, angle, stage, breachAngle, worldSeed);

                if (inCavern) {
                    long mod = generateCavernColumn(level, pos, x, z, distXZ, angleDeg, stage, worldSeed);
                    if (stage == 2) s2 += mod;
                    else if (stage == 3) s3 += mod;
                    else s4 += mod;
                    chunkModifiedCount += mod;
                }
            }
        }

        // Sayaçları güncelle
        if (chunkModifiedCount > 0) {
            TOTAL_CHUNKS_PROCESSED.incrementAndGet();
            TOTAL_BLOCKS_CARVED.addAndGet(chunkModifiedCount);
            STAGE1_BLOCKS.addAndGet(s1);
            STAGE2_BLOCKS.addAndGet(s2);
            STAGE3_BLOCKS.addAndGet(s3);
            STAGE4_BLOCKS.addAndGet(s4);

            LOGGER.info("[CosmicArmor-WorldGen] Chunk [{}, {}]: Carved/Modified {} blocks (S1: {}, S2: {}, S3: {}, S4: {}) | Cumulative: {} chunks, {} blocks",
                    chunkPos.x, chunkPos.z, chunkModifiedCount, s1, s2, s3, s4,
                    TOTAL_CHUNKS_PROCESSED.get(), TOTAL_BLOCKS_CARVED.get());
        }

        // Merkez chunk ise heykeli dik ve ArenaManager kaydını yap
        if (chunkPos.x == (centerX >> 4) && chunkPos.z == (centerZ >> 4)) {
            spawnStatueAndRegister(level, center);
        }
    }

    private static int getStage(double distXZ) {
        if (distXZ <= R_STAGE2) return 2;
        if (distXZ <= R_STAGE3) return 3;
        return 4;
    }

    public static double getBreachAngle(long seed) {
        return ((seed >> 8) & 0xFF) * (2.0D * Math.PI / 256.0D);
    }

    // =========================================================================
    // AŞAMA 1: GERÇEK 3D BASIK ELİPSOİD DIŞ VE İÇ KÜRE
    // =========================================================================
    private static long generateStage1Column(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                             int x, int z, int dx, int dz, double distXZ,
                                             double angle, double breachAngle, long seed) {

        long modified = 0;
        boolean isBreach = isNearAngle(angle, breachAngle, 0.095D);

        for (int y = -62; y <= -18; y++) {
            pos.set(x, y, z);
            double dy = y - ELLIPSOID_CENTER_Y;

            double normOuter = (dx * dx + dz * dz) / (ELLIPSOID_OUTER_H * ELLIPSOID_OUTER_H)
                    + (dy * dy) / (ELLIPSOID_OUTER_V * ELLIPSOID_OUTER_V);

            double normInner = (dx * dx + dz * dz) / (ELLIPSOID_INNER_H * ELLIPSOID_INNER_H)
                    + (dy * dy) / (ELLIPSOID_INNER_V * ELLIPSOID_INNER_V);

            // Dış elipsoidin dışı: bu sütun elipsoid dışında kalıyor
            if (normOuter > 1.0D) {
                continue;
            }

            // 1. DIŞ MONOLİTİK KABUK (normInner >= 1.0 && normOuter <= 1.0)
            if (normInner >= 1.0D) {
                if (isBreach && y >= ARENA_FLOOR_Y && y <= ARENA_FLOOR_Y + 5) {
                    // Gizli giriş yarığı
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    if (y == ARENA_FLOOR_Y) {
                        level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                    }
                } else if (isBreach && y < ARENA_FLOOR_Y) {
                    level.setBlock(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                } else {
                    int hash = (x * 31 + y * 17 + z * 13) & 0x7FFFFFFF;
                    BlockState wall;
                    if (hash % 11 == 0) wall = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else if (hash % 5 == 0) wall = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
                    else if (hash % 3 == 0) wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    else wall = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

                    level.setBlock(pos, wall, 2);
                }
                modified++;
                continue;
            }

            // 2. İÇ ARENA HACMİ (normInner < 1.0)
            // Taban dolgusu
            if (y < ARENA_FLOOR_Y) {
                level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 2);
                modified++;
                continue;
            }

            // Zemin Katmanı (y == ARENA_FLOOR_Y)
            if (y == ARENA_FLOOR_Y) {
                BlockState floorState = getArenaFloorBlock(distXZ, seed, x, z);
                level.setBlock(pos, floorState, 2);
                // Aydınlatmalı lamba altı güç bloğu
                if (floorState.is(Blocks.REDSTONE_LAMP)) {
                    level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                }
                modified++;
                continue;
            }

            // 4 Büyük Destek Sütunu Kontrolü (|dx| == 18..21 ve |dz| == 18..21)
            if (isNearPillar(dx, dz) && y <= ARENA_FLOOR_Y + 16) {
                int hash = (x * 7 + y * 19 + z * 11) & 0x7FFFFFFF;
                BlockState pillarState = (hash % 3 == 0)
                        ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                        : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                level.setBlock(pos, pillarState, 2);
                modified++;
                continue;
            }

            // Merkez Podyum (r <= 6, y == ARENA_FLOOR_Y + 1)
            if (distXZ <= 6.0D && y == ARENA_FLOOR_Y + 1) {
                BlockState dais = (distXZ <= 2.5D)
                        ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                        : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                level.setBlock(pos, dais, 2);
                modified++;
                continue;
            }

            // İç Hacim Boşluğu: Hava ve sıvı temizliği
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            modified++;
        }

        return modified;
    }

    private static boolean isNearPillar(int dx, int dz) {
        int px = Math.abs(dx);
        int pz = Math.abs(dz);
        return (Math.abs(px - 20) <= 1 && Math.abs(pz - 20) <= 1);
    }

    private static BlockState getArenaFloorBlock(double distXZ, long seed, int x, int z) {
        int hash = (x * 23 + z * 41 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;

        if (distXZ <= 6.0D) {
            return (distXZ <= 2.0D) ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        }
        if (distXZ <= 22.0D) {
            return (hash % 5 == 0) ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (distXZ <= 35.0D) {
            if ((int) distXZ % 3 == 0 && hash % 2 == 0) {
                return Blocks.REDSTONE_LAMP.defaultBlockState();
            }
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        return (hash % 4 == 0) ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    // =========================================================================
    // AŞAMALAR 2, 3, 4: GERÇEK VE GENİŞLETİLMİŞ ORGANİK MAĞARA OYMA ALGORİTMASI
    // =========================================================================
    private static boolean isInsideCavern(int x, int z, int dx, int dz, double distXZ, double angle, int stage, double breachAngle, long seed) {
        double seedTerm = (seed & 0xFFFF) * 0.0013D;

        // 1. Ana Giriş Omurgası (Giriş yarığından tüm aşamalar boyunca uzanan ana koridor)
        double entranceWobble = Math.sin(distXZ * 0.05D + seedTerm * 10.0D) * 0.15D;
        if (isNearAngle(angle, breachAngle + entranceWobble, 0.12D)) {
            return true;
        }

        // 2. 8 Adet Ana Radyal Koridor (Her yöne açılan geniş caddeler)
        int numBranches = 8;
        double branchSpacing = (2.0D * Math.PI) / numBranches;
        for (int i = 0; i < numBranches; i++) {
            double baseBranchAngle = breachAngle + i * branchSpacing;
            double wobble = Math.sin(distXZ * 0.04D + i * 1.3D + seedTerm) * 0.16D;
            double targetAngle = baseBranchAngle + wobble;

            double widthThreshold = (stage == 2) ? 0.11D : ((stage == 3) ? 0.16D : 0.22D);
            if (isNearAngle(angle, targetAngle, widthThreshold)) {
                return true;
            }
        }

        // 3. Eşmerkezli Dairesel Halka Koridorlar (Bütün radyal caddeleri ve odaları kesintisiz bağlar)
        // Aşama 2 Halkaları: r in [62, 68] ve r in [76, 82]
        if (stage == 2 && ((distXZ >= 62.0D && distXZ <= 68.0D) || (distXZ >= 76.0D && distXZ <= 82.0D))) {
            return true;
        }

        // Aşama 3 Halkaları: r in [92, 99] ve r in [106, 113]
        if (stage == 3 && ((distXZ >= 92.0D && distXZ <= 99.0D) || (distXZ >= 106.0D && distXZ <= 113.0D))) {
            return true;
        }

        // Aşama 4 Halkaları: r in [124, 134], r in [142, 151], r in [157, 164]
        if (stage == 4 && ((distXZ >= 124.0D && distXZ <= 134.0D) ||
                           (distXZ >= 142.0D && distXZ <= 151.0D) ||
                           (distXZ >= 157.0D && distXZ <= 164.0D))) {
            return true;
        }

        // 4. Katı Ölü Bölgeleri Ortadan Kaldıran Organik Hücresel Gürültü Oyması
        if (stage == 4) {
            // Aşama 4: Devasa katedral mağaraları (~%75-80 alan oyuktur)
            double cavernNoise = Math.sin(x * 0.042D + z * 0.038D + seedTerm) * Math.cos(x * 0.038D - z * 0.042D)
                    + 0.38D * Math.sin(x * 0.082D + z * 0.076D);
            if (cavernNoise > -0.22D) {
                return true;
            }
        } else if (stage == 3) {
            // Aşama 3: Geniş atölye ve mekanik galeriler (~%65 alan oyuktur)
            double workshopNoise = Math.sin(x * 0.055D - z * 0.050D + seedTerm) * Math.cos(x * 0.050D + z * 0.055D)
                    + 0.32D * Math.sin(x * 0.095D + z * 0.090D);
            if (workshopNoise > -0.12D) {
                return true;
            }
        } else if (stage == 2) {
            // Aşama 2: Birbirine bağlı labirent katakomp salonları (~%55 alan oyuktur)
            double catacombNoise = Math.sin(x * 0.075D + z * 0.068D + seedTerm) * Math.cos(x * 0.068D - z * 0.075D)
                    + 0.28D * Math.sin(x * 0.130D - z * 0.120D);
            if (catacombNoise > -0.05D) {
                return true;
            }
        }

        return false;
    }

    private static boolean isNearAngle(double a1, double a2, double threshold) {
        double diff = Math.abs(a1 - a2);
        while (diff > Math.PI) diff = Math.abs(diff - 2.0D * Math.PI);
        return diff <= threshold;
    }

    private static long generateCavernColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                             int x, int z, double distXZ, double angleDeg,
                                             int stage, long seed) {

        long modified = 0;
        int floorY;
        int ceilY;

        if (stage == 4) {
            // AŞAMA 4: 25-34 BLOK YÜKSEKLİKTE DEVASA KATEDRAL DOĞAL MAĞARALARI
            floorY = -54 + (int) Math.round(Math.sin(x * 0.04D + z * 0.04D) * 2.5D);
            ceilY = -22 + (int) Math.round(Math.cos(x * 0.03D - z * 0.03D) * 3.5D);

            // Aşama 4 Doğal Kaya Sütunları: Devasa kolonlar
            int pillarHash = (x * 73 + z * 37 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;
            if (pillarHash % 67 == 0 || (Math.abs(x % 24) <= 1 && Math.abs(z % 24) <= 1 && pillarHash % 2 == 0)) {
                return buildNaturalRockPillar(level, pos, x, z, floorY, ceilY);
            }
        } else if (stage == 3) {
            // AŞAMA 3: 15-19 BLOK YÜKSEKLİKTE MAKİNE GALERİLERİ
            floorY = -51 + (int) Math.round(Math.sin(x * 0.06D + z * 0.06D) * 1.5D);
            ceilY = -34 + (int) Math.round(Math.cos(x * 0.05D - z * 0.05D) * 2.0D);

            // Aşama 3 Destek Kolonları
            if (Math.abs(x % 16) == 0 && Math.abs(z % 16) == 0) {
                return buildMachineSupportPillar(level, pos, x, z, floorY, ceilY);
            }
        } else {
            // AŞAMA 2: 9-11 BLOK YÜKSEKLİKTE TUZAKLI KATAKOMPLAR
            floorY = -50 + (int) Math.round(Math.sin(x * 0.08D + z * 0.08D) * 1.0D);
            ceilY = -40 + (int) Math.round(Math.cos(x * 0.07D - z * 0.07D) * 1.2D);
        }

        floorY = Math.max(-62, floorY);
        ceilY = Math.min(-18, ceilY);
        if (floorY >= ceilY) return 0;

        int hash = (x * 37 + z * 29 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;

        for (int y = floorY - 1; y <= ceilY; y++) {
            pos.set(x, y, z);

            // 1. ZEMİN KAPLAMASI (y == floorY - 1)
            if (y == floorY - 1) {
                BlockState fl;
                if (stage == 4) {
                    if (hash % 3 == 0) fl = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else if (hash % 11 == 0) fl = Blocks.REDSTONE_BLOCK.defaultBlockState();
                    else fl = Blocks.DEEPSLATE.defaultBlockState();
                } else if (stage == 3) {
                    if (hash % 5 == 0) fl = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    else if (hash % 3 == 0) fl = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    else fl = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                } else {
                    if (hash % 4 == 0) fl = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else fl = Blocks.DEEPSLATE_TILES.defaultBlockState();
                }
                level.setBlock(pos, fl, 2);
                modified++;
            }
            // 2. MAĞARA İÇİ HAVA VE DEKORASYONLAR
            else if (y >= floorY && y < ceilY) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                modified++;

                if (y == floorY) {
                    placeDecorations(level, pos, x, z, floorY, stage, distXZ, angleDeg, seed);
                }
            }
            // 3. TAVAN (y == ceilY)
            else if (y == ceilY) {
                BlockState roof;
                if (stage == 4 && hash % 3 == 0) {
                    roof = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                } else if (stage == 3 && hash % 7 == 0) {
                    roof = Blocks.REDSTONE_LAMP.defaultBlockState();
                    level.setBlock(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                } else if (stage == 2 && hash % 5 == 0) {
                    roof = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                } else {
                    roof = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                }
                level.setBlock(pos, roof, 2);
                modified++;
            }
        }

        return modified;
    }

    private static long buildNaturalRockPillar(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int floorY, int ceilY) {
        long count = 0;
        for (int y = floorY - 1; y <= ceilY; y++) {
            pos.set(x, y, z);
            int h = (x * 19 + y * 13 + z * 7) & 0x7FFFFFFF;
            BlockState pillar = (h % 3 == 0)
                    ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                    : Blocks.DEEPSLATE.defaultBlockState();
            level.setBlock(pos, pillar, 2);
            count++;
        }
        return count;
    }

    private static long buildMachineSupportPillar(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int floorY, int ceilY) {
        long count = 0;
        for (int y = floorY - 1; y <= ceilY; y++) {
            pos.set(x, y, z);
            BlockState pillar = (y == floorY + 1 || y == ceilY - 1)
                    ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                    : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            level.setBlock(pos, pillar, 2);
            count++;
        }
        return count;
    }

    private static void placeDecorations(WorldGenLevel level, BlockPos pos, int x, int z,
                                         int floorY, int stage, double distXZ, double angleDeg, long seed) {
        int hash = (x * 47 + z * 31 + (int)(seed & 0xFFF)) & 0x7FFFFFFF;

        if (stage == 2) {
            // Lav Çukurları
            if (distXZ >= 70.0D && distXZ <= 72.0D && (hash % 11 == 0)) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                level.setBlock(pos.below(), Blocks.LAVA.defaultBlockState(), 2);
                return;
            }
            // Mayınlar ve TNT
            if (distXZ >= 76.0D && distXZ <= 79.0D && (hash % 13 == 0)) {
                level.setBlock(pos.below(2), Blocks.TNT.defaultBlockState(), 2);
                level.setBlock(pos.below(1), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
                level.setBlock(pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
                return;
            }
            if (hash % 29 == 0) {
                level.setBlock(pos, ModBlocks.REDSTONE_LANDMINE.get().defaultBlockState(), 2);
                return;
            }
            // Tripwire ve Hasarlı Ok Atan Dispenserlar
            if (distXZ >= 62.0D && distXZ <= 65.0D && (hash % 17 == 0)) {
                level.setBlock(pos, Blocks.TRIPWIRE.defaultBlockState(), 2);
                BlockPos wallPos = pos.east(2);
                level.setBlock(wallPos, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST), 2);
                BlockPos dispPos = wallPos.east();
                level.setBlock(dispPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.WEST), 2);

                BlockEntity be = level.getBlockEntity(dispPos);
                if (be instanceof DispenserBlockEntity dispenser) {
                    ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW, 8);
                    PotionUtils.setPotion(tippedArrow, Potions.HARMING);
                    dispenser.setItem(0, tippedArrow);
                }
                return;
            }
            // Spawnerlar
            if (distXZ >= 80.0D && distXZ <= 84.0D && (hash % 29 == 0)) {
                level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId((hash % 2 == 0) ? EntityType.SKELETON : EntityType.ZOMBIE, level.getRandom());
                }
                return;
            }
            if (hash % 5 == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        } else if (stage == 3) {
            // Çalışan Piston Düzenekleri
            if (distXZ >= 92.0D && distXZ <= 95.0D && (hash % 11 == 0)) {
                level.setBlock(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 2);
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                return;
            }
            // Asılı Lambalar ve Kızıltaş Fenerleri
            if (distXZ >= 102.0D && distXZ <= 106.0D && (hash % 13 == 0)) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                level.setBlock(pos.above(1), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                level.setBlock(pos.above(2), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
                return;
            }
            if (hash % 19 == 0) {
                level.setBlock(pos, Blocks.CHAIN.defaultBlockState(), 2);
                level.setBlock(pos.above(1), Blocks.LANTERN.defaultBlockState(), 2);
            }
            if (hash % 6 == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        } else if (stage == 4) {
            if (hash % 5 == 0 && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos.below(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
            } else if (hash % 17 == 0) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        }
    }

    private static void spawnStatueAndRegister(WorldGenLevel level, BlockPos center) {
        BlockPos statuePos = new BlockPos(center.getX(), ARENA_FLOOR_Y + 2, center.getZ());

        RedstoneGolemStatueEntity statue = new RedstoneGolemStatueEntity(ModEntities.REDSTONE_GOLEM_STATUE.get(), level.getLevel());
        statue.setPos(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 0.5D);
        statue.setYRot(180.0F);
        statue.setPersistenceRequired();
        level.addFreshEntity(statue);

        if (level.getLevel() != null) {
            ArenaManager.get(level.getLevel()).registerGeneratedArena(
                    center.getX() - 54, ARENA_FLOOR_Y, center.getZ() - 54, statuePos
            );
        }
        LOGGER.info("[CosmicArmor-WorldGen] Redstone Golem Statue spawned at center: {}", statuePos);
    }

    public static void repairArena(ServerLevel level, BlockPos center) {
        BoundingBox arenaBox = new BoundingBox(
                center.getX() - 55, -62, center.getZ() - 55,
                center.getX() + 55, -18, center.getZ() + 55
        );
        ChunkPos chunkPos = new ChunkPos(center);
        generate(level, arenaBox, chunkPos, center, level.getSeed());
        ArenaManager.cleanLiquidsInSphere(level, center);
    }
}
