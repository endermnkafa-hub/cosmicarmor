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
 * Aşama 1 (r <= 54): Basık 3D Elipsoid Monolitik Dış Küre ve İç Savaş Arenası (KORUNAN ALAN)
 * Aşama 2 (54 < r <= 85): Tuzaklı Katakomplar (3D bağlantılı labirent salonlar, lav kanalları, mayınlar, tuzaklar)
 * Aşama 3 (85 < r <= 115): Kadim Kızıltaş Makineleri (3D endüstriyel galeriler, pistonlar, asılı lambalar)
 * Aşama 4 (115 < r <= 165): Devasa Katedral Mağaraları (Gerçek 3D hacimler, organik kaya sütunları, doğal %6-10 kızıltaş damarları)
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

    // Aşama 1 Elipsoid Parametreleri (KESİNLİKLE DEĞİŞTİRİLMEZ)
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

                double angle = Math.atan2(dz, dz == 0 && dx == 0 ? 0 : dx);
                if (angle < 0.0D) angle += 2.0D * Math.PI;

                // =========================================================
                // AŞAMA 1: 3D ELİPSOİD DIŞ KÜRE VE İÇ ARENA (r <= 54.0)
                // (Mevcut tasarım, heykel, podyum ve mekanikler %100 korunur)
                // =========================================================
                if (distXZ <= R_STAGE1) {
                    long mod = generateStage1Column(level, pos, x, z, dx, dz, distXZ, angle, breachAngle, worldSeed);
                    s1 += mod;
                    chunkModifiedCount += mod;
                    continue;
                }

                // =========================================================
                // AŞAMALAR 2, 3, 4: GERÇEK 3D ORGANİK MAĞARA HACİMLERİ (54.0 < r <= 165.0)
                // =========================================================
                int stage = getStage(distXZ);
                long mod = generate3DCaveColumn(level, pos, x, z, dx, dz, distXZ, angle, stage, breachAngle, worldSeed);
                if (stage == 2) s2 += mod;
                else if (stage == 3) s3 += mod;
                else s4 += mod;
                chunkModifiedCount += mod;
            }
        }

        // Sayaçları güncelle ve logla
        if (chunkModifiedCount > 0) {
            TOTAL_CHUNKS_PROCESSED.incrementAndGet();
            TOTAL_BLOCKS_CARVED.addAndGet(chunkModifiedCount);
            STAGE1_BLOCKS.addAndGet(s1);
            STAGE2_BLOCKS.addAndGet(s2);
            STAGE3_BLOCKS.addAndGet(s3);
            STAGE4_BLOCKS.addAndGet(s4);

            LOGGER.info("[CosmicArmor-WorldGen] Chunk [{}, {}]: Modified {} blocks (S1: {}, S2: {}, S3: {}, S4: {}) | Cumulative: {} chunks, {} blocks",
                    chunkPos.x, chunkPos.z, chunkModifiedCount, s1, s2, s3, s4,
                    TOTAL_CHUNKS_PROCESSED.get(), TOTAL_BLOCKS_CARVED.get());
        }

        // Merkez chunk ise heykeli dik ve ArenaManager kaydını yap
        if (chunkPos.x == (centerX >> 4) && chunkPos.z == (centerZ >> 4)) {
            spawnStatueAndRegister(level, center);
        }
    }

    public static int getStage(double distXZ) {
        if (distXZ <= R_STAGE1) return 1;
        if (distXZ <= R_STAGE2) return 2;
        if (distXZ <= R_STAGE3) return 3;
        return 4;
    }

    public static double getBreachAngle(long seed) {
        return ((seed >> 8) & 0xFF) * (2.0D * Math.PI / 256.0D);
    }

    // =========================================================================
    // AŞAMA 1: GERÇEK 3D BASIK ELİPSOİD DIŞ VE İÇ KÜRE (KORUNAN BÖLGE)
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

            // Dış elipsoidin dışı
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
    // AŞAMALAR 2, 3, 4: GERÇEK 3D COHERENT NOISE YOĞUNLUK VE OYMA SİSTEMİ
    // =========================================================================

    /**
     * 3D Yoğunluk fonksiyonu:
     * Değer < 0.0D olduğunda hacim boştur (CAVE AIR).
     * Değer >= 0.0D olduğunda katı kayadır (SOLID ROCK).
     */
    public static double getCaveDensity(int x, int y, int z, int dx, int dz, double distXZ,
                                         double angle, int stage, double breachAngle, long seed) {

        // 1. DİKEY ZARF (Y in [-60, -18] aralığını kesin olarak korur)
        double yCenter;
        double yHalfHeight;
        if (stage == 4) {
            yCenter = -38.0D;
            yHalfHeight = 16.0D; // Y: -54 ile -22 arası devasa katedral tavanı (26-32 blok)
        } else if (stage == 3) {
            yCenter = -42.0D;
            yHalfHeight = 10.0D; // Y: -52 ile -32 arası geniş makine salonları (16-20 blok)
        } else {
            yCenter = -45.0D;
            yHalfHeight = 6.0D;  // Y: -51 ile -39 arası katakomplar (9-12 blok)
        }

        double dyNorm = (y - yCenter) / yHalfHeight;
        // 4. derece parabolik zarf: orta kısımda 0 kalır, sınırlara yaklaşınca dikleşip kapatır
        double verticalPenalty = dyNorm * dyNorm * dyNorm * dyNorm;

        // 2. 3D DOMAIN WARPED ORGANİK MAĞARA GÜRÜLTÜSÜ
        double warpX = Cave3DNoise.noise3D(x * 0.035D, y * 0.035D, z * 0.035D, seed + 101L) * 5.0D;
        double warpY = Cave3DNoise.noise3D(x * 0.035D, y * 0.035D, z * 0.035D, seed + 202L) * 3.5D;
        double warpZ = Cave3DNoise.noise3D(x * 0.035D, y * 0.035D, z * 0.035D, seed + 303L) * 5.0D;

        double qx = x + warpX;
        double qy = y + warpY;
        double qz = z + warpZ;

        double cavernNoise;
        if (stage == 4) {
            double n1 = Cave3DNoise.noise3D(qx * 0.026D, qy * 0.036D, qz * 0.026D, seed + 404L);
            double n2 = Cave3DNoise.noise3D(qx * 0.060D, qy * 0.070D, qz * 0.060D, seed + 505L) * 0.35D;
            cavernNoise = n1 + n2 - 0.26D; // Geniş açık alan eğilimi
        } else if (stage == 3) {
            double n1 = Cave3DNoise.noise3D(qx * 0.038D, qy * 0.048D, qz * 0.038D, seed + 606L);
            double n2 = Cave3DNoise.noise3D(qx * 0.075D, qy * 0.085D, qz * 0.075D, seed + 707L) * 0.30D;
            cavernNoise = n1 + n2 - 0.16D;
        } else {
            double n1 = Cave3DNoise.noise3D(qx * 0.050D, qy * 0.065D, qz * 0.050D, seed + 808L);
            double n2 = Cave3DNoise.noise3D(qx * 0.100D, qy * 0.110D, qz * 0.100D, seed + 909L) * 0.25D;
            cavernNoise = n1 + n2 - 0.08D;
        }

        // 3. KESİNTİSİZ GEÇİŞ VE BAĞLANTI (Giriş Omurgası + Radyal Arterler + Halka Galeriler)
        // Giriş yarığından başlayan ana omurga
        double spineWobble = Math.sin(distXZ * 0.04D) * 0.12D;
        double spineAngleDiff = angleDiff(angle, breachAngle + spineWobble);
        double spineDistXZ = distXZ * Math.sin(spineAngleDiff);
        double spineDistY = y - (-45.0D + Math.sin(distXZ * 0.05D) * 2.5D);
        double spineDist = Math.sqrt(spineDistXZ * spineDistXZ + spineDistY * spineDistY * 1.5D);
        double spineCarve = (spineDist < 5.2D) ? -(5.2D - spineDist) * 0.45D : 0.0D;

        // 8 Adet Ana Radyal Koridor
        double radialCarve = 0.0D;
        int numRadial = 8;
        double radialSpacing = (2.0D * Math.PI) / numRadial;
        for (int i = 0; i < numRadial; i++) {
            double bAngle = breachAngle + i * radialSpacing;
            double wAngle = bAngle + Math.sin(distXZ * 0.035D + i * 1.1D) * 0.14D;
            double aDiff = angleDiff(angle, wAngle);
            double rDistXZ = distXZ * Math.sin(aDiff);
            double rDistY = y - (yCenter + Math.sin(distXZ * 0.04D + i) * 2.0D);
            double rDist = Math.sqrt(rDistXZ * rDistXZ + rDistY * rDistY * 1.4D);
            double tunnelRadius = (stage == 4) ? 6.5D : ((stage == 3) ? 5.2D : 4.2D);
            if (rDist < tunnelRadius) {
                double c = -(tunnelRadius - rDist) * 0.35D;
                if (c < radialCarve) radialCarve = c;
            }
        }

        // Eşmerkezli Dairesel Halka Tüneller
        double ringCarve = 0.0D;
        double[] ringRadii;
        if (stage == 4) ringRadii = new double[]{126.0D, 144.0D, 160.0D};
        else if (stage == 3) ringRadii = new double[]{94.0D, 108.0D};
        else ringRadii = new double[]{64.0D, 77.0D};

        for (double ringR : ringRadii) {
            double dr = Math.abs(distXZ - ringR);
            double ry = y - (yCenter + Math.sin(angle * 3.0D) * 2.0D);
            double ringDist = Math.sqrt(dr * dr + ry * ry * 1.5D);
            double ringRadius = (stage == 4) ? 5.8D : 4.2D;
            if (ringDist < ringRadius) {
                double c = -(ringRadius - ringDist) * 0.35D;
                if (c < ringCarve) ringCarve = c;
            }
        }

        // 4. DOĞAL ORGANİK KAYA SÜTUNLARI (KARE IZGARA YOK, DETERMINISTIK GÜRÜLTÜ TEPELERİ)
        double pillarCarve = 0.0D;
        if (stage == 4) {
            double pillarField = Cave3DNoise.noise2D(x * 0.048D, z * 0.048D, seed + 77777L);
            if (pillarField > 0.62D) {
                // Kum saati profili: tavan ve tabanda kalın, ortada dar
                double dyC = (y - yCenter) / yHalfHeight;
                double flare = 1.0D + 0.6D * (dyC * dyC);
                double core = (pillarField - 0.62D) / 0.38D * flare;
                core += Cave3DNoise.noise3D(x * 0.16D, y * 0.16D, z * 0.16D, seed + 88888L) * 0.14D;
                if (core > 0.30D) {
                    pillarCarve = 1.5D; // Katı kaya kütlesi ekle
                }
            }
        } else if (stage == 3) {
            // Aşama 3 Makine Kolonları: geniş mekanik odalarda organik aralıklı sütunlar
            double pillarField = Cave3DNoise.noise2D(x * 0.065D, z * 0.065D, seed + 66666L);
            if (pillarField > 0.68D) {
                pillarCarve = 1.4D;
            }
        }

        return verticalPenalty + cavernNoise + spineCarve + radialCarve + ringCarve + pillarCarve;
    }

    private static double angleDiff(double a1, double a2) {
        double diff = Math.abs(a1 - a2);
        while (diff > Math.PI) diff = Math.abs(diff - 2.0D * Math.PI);
        return diff;
    }

    private static boolean isNearAngle(double a1, double a2, double threshold) {
        return angleDiff(a1, a2) <= threshold;
    }

    /**
     * Tek bir (x, z) dikey sütunu boyunca 3D hacmi değerlendirir ve oyar.
     */
    private static long generate3DCaveColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                             int x, int z, int dx, int dz, double distXZ,
                                             double angle, int stage, double breachAngle, long worldSeed) {

        long modified = 0;
        int minY = -62;
        int maxY = -18;
        int height = maxY - minY + 1; // 45 blok

        boolean[] isAir = new boolean[height];

        // 1. ADIM: 3D Yoğunluk Değerlendirmesi
        for (int y = minY; y <= maxY; y++) {
            double density = getCaveDensity(x, y, z, dx, dz, distXZ, angle, stage, breachAngle, worldSeed);
            isAir[y - minY] = (density < 0.0D);
        }

        // 2. ADIM: Blokları Yerleştir ve Yüzeyleri İşle
        for (int y = minY; y <= maxY; y++) {
            int idx = y - minY;
            pos.set(x, y, z);

            if (isAir[idx]) {
                // Mağara Boşluğu: Havayı temizle
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                modified++;
            } else {
                // Katı Kaya: Yüzey kontrolü
                boolean isFloor = (idx + 1 < height) && isAir[idx + 1];  // Üstü hava: Mağara tabanı
                boolean isCeiling = (idx - 1 >= 0) && isAir[idx - 1];    // Altı hava: Mağara tavanı

                if (isFloor || isCeiling) {
                    BlockState surfaceState = getCaveSurfaceBlock(x, y, z, stage, isFloor, isCeiling, worldSeed);
                    level.setBlock(pos, surfaceState, 2);
                    modified++;

                    // Dekorasyon ve Tuzak/Makine Entegrasyonu (Taban seviyesinde)
                    if (isFloor) {
                        placeSurfaceFeature(level, pos, x, y, z, stage, distXZ, worldSeed);
                    }
                }
            }
        }

        return modified;
    }

    /**
     * Doğal Kaya ve %6-10 Kızıltaş Damar Paleti.
     * Modulo çapraz desenleri kesinlikle kullanılmaz; sürekli 3D gürültü kümeleri kullanılır.
     */
    public static BlockState getCaveSurfaceBlock(int x, int y, int z, int stage, boolean isFloor, boolean isCeiling, long seed) {
        // 3D Kızıltaş Damar Alanı: sürekli 3D cepler (Damarlar 3-6 blok genişlikte uzanır)
        double oreNoise = Cave3DNoise.noise3D(x * 0.15D, y * 0.15D, z * 0.15D, seed + 424242L);

        if (stage == 4) {
            // AŞAMA 4: Doğal Katedral Mağaraları
            // oreNoise > 0.68: %0.5 nadir kızıltaş bloğu çekirdeği
            // oreNoise > 0.40: %7.8 doğal bağlantılı derin cevher damarları (Hedef: %6-10)
            if (oreNoise > 0.68D) {
                return Blocks.REDSTONE_BLOCK.defaultBlockState();
            } else if (oreNoise > 0.40D) {
                return Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
            }

            // Doğal Kaya Çeşitleri: Geniş jeolojik bölgeler (Dama tahtası yok)
            double rockNoise = Cave3DNoise.noise3D(x * 0.032D, y * 0.032D, z * 0.032D, seed + 888888L);
            if (rockNoise > 0.36D) {
                return Blocks.TUFF.defaultBlockState();
            } else if (rockNoise < -0.36D) {
                return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            } else {
                return Blocks.DEEPSLATE.defaultBlockState();
            }
        } else if (stage == 3) {
            // AŞAMA 3: Kadim Makineler ve Atölyeler
            if (oreNoise > 0.62D) {
                return Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(); // %4.2 cevher
            }

            // Makine alanlarında mimari geçişler
            double ruinNoise = Cave3DNoise.noise3D(x * 0.045D, y * 0.045D, z * 0.045D, seed + 333333L);
            if (ruinNoise > 0.35D) {
                return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            } else if (ruinNoise > 0.15D) {
                return Blocks.DEEPSLATE_TILES.defaultBlockState();
            } else {
                return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            }
        } else {
            // AŞAMA 2: Tuzaklı Katakomplar
            if (oreNoise > 0.65D) {
                return Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(); // %3.5 cevher
            }

            double catacombNoise = Cave3DNoise.noise3D(x * 0.055D, y * 0.055D, z * 0.055D, seed + 222222L);
            if (catacombNoise > 0.28D) {
                return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            } else if (catacombNoise < -0.28D) {
                return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            } else {
                return Blocks.DEEPSLATE_TILES.defaultBlockState();
            }
        }
    }

    /**
     * Tuzaklar, makineler ve özel özellikler (Kesinlikle doğal mağarada rastgele kızıltaş tozu konulmaz).
     */
    private static void placeSurfaceFeature(WorldGenLevel level, BlockPos.MutableBlockPos floorPos,
                                            int x, int y, int z, int stage, double distXZ, long seed) {

        BlockPos airPos = floorPos.above(); // Yürünebilir boşluk

        if (stage == 2) {
            // AŞAMA 2: TUZAKLAR VE KATAKOMP MEKANİZMALARI
            double featureNoise = Cave3DNoise.noise2D(x * 0.16D, z * 0.16D, seed + 55555L);

            // 1. Kızıltaş Mayını (Yürünebilir zeminde)
            if (featureNoise > 0.78D && featureNoise <= 0.83D) {
                level.setBlock(airPos, ModBlocks.REDSTONE_LANDMINE.get().defaultBlockState(), 2);
                return;
            }

            // 2. Basınç Plakası ve TNT Tuzağı
            if (featureNoise > 0.83D && featureNoise <= 0.88D) {
                level.setBlock(floorPos.below(), Blocks.TNT.defaultBlockState(), 2);
                level.setBlock(floorPos, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
                level.setBlock(airPos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
                return;
            }

            // 3. Tripwire ve Fırlatıcı Tuzağı
            if (featureNoise > 0.88D && featureNoise <= 0.92D) {
                level.setBlock(airPos, Blocks.TRIPWIRE.defaultBlockState(), 2);
                BlockPos wallPos = airPos.east(2);
                if (level.getBlockState(wallPos).isSolid()) {
                    level.setBlock(wallPos, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST), 2);
                    BlockPos dispPos = wallPos.east();
                    level.setBlock(dispPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.WEST), 2);
                    BlockEntity be = level.getBlockEntity(dispPos);
                    if (be instanceof DispenserBlockEntity dispenser) {
                        ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW, 8);
                        PotionUtils.setPotion(tippedArrow, Potions.HARMING);
                        dispenser.setItem(0, tippedArrow);
                    }
                }
                return;
            }

            // 4. Canavar Üreteci (Spawner)
            if (featureNoise > 0.92D) {
                level.setBlock(airPos, Blocks.SPAWNER.defaultBlockState(), 2);
                BlockEntity be = level.getBlockEntity(airPos);
                if (be instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId((featureNoise > 0.96D) ? EntityType.SKELETON : EntityType.ZOMBIE, level.getRandom());
                }
                return;
            }

            // 5. Lav Çukuru Kanalları
            if (featureNoise < -0.82D) {
                level.setBlock(floorPos, Blocks.LAVA.defaultBlockState(), 2);
                level.setBlock(floorPos.below(), Blocks.LAVA.defaultBlockState(), 2);
            }
        } else if (stage == 3) {
            // AŞAMA 3: KADİM KIZILTAŞ MAKİNELERİ VE ATÖLYELER
            double machineNoise = Cave3DNoise.noise2D(x * 0.14D, z * 0.14D, seed + 44444L);

            // 1. Çalışan Piston Düzeneği
            if (machineNoise > 0.82D && machineNoise <= 0.88D) {
                level.setBlock(airPos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 2);
                level.setBlock(floorPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                return;
            }

            // 2. Tavan Aydınlatması (Asılı fener veya lamba)
            if (machineNoise > 0.88D) {
                level.setBlock(airPos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                level.setBlock(airPos.above(2), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                level.setBlock(airPos.above(3), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            }
        }
        // AŞAMA 4: DOĞAL MAĞARA - KESİNLİKLE RASTGELE REDSTONE_WIRE YERLEŞTİRİLMEZ!
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
