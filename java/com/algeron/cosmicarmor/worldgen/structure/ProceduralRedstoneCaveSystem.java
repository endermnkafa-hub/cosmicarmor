package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.block.ModBlocks;
import com.algeron.cosmicarmor.entity.ModEntities;
import com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * 4 Aşamalı Prosedürel Kızıltaş Derinlikleri Mağara Sistemi ve Boss Arenası.
 *
 * Aşama 1: Yarıçap 0 - 54 blok  (Dış koruyucu küre ve iç boss savaş arenası)
 * Aşama 2: Yarıçap 54 - 85 blok (Tuzaklı dar mağaralar, lav hendekleri, spawnerlar)
 * Aşama 3: Yarıçap 85 - 115 blok (Basit kızıltaş makineleri, pistonlar, lambalı galeriler)
 * Aşama 4: Yarıçap 115 - 165 blok (Devasa yüksek tavanlı doğal mağaralar, kaya sütunları, zengin kızıltaş damarları)
 */
public class ProceduralRedstoneCaveSystem {

    public static final double R_STAGE1 = 54.0D;
    public static final double R_STAGE2 = 85.0D;
    public static final double R_STAGE3 = 115.0D;
    public static final double R_STAGE4 = 165.0D;

    public static final int ARENA_FLOOR_Y = -50;
    public static final int ARENA_CEIL_Y = -22;

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
                // AŞAMA 1: DIŞ KÜRE VE İÇ ARENA (r <= 54.0)
                // =========================================================
                if (distXZ <= R_STAGE1) {
                    generateStage1Column(level, pos, x, z, dx, dz, distXZ, angle, breachAngle, worldSeed);
                    continue;
                }

                // =========================================================
                // AŞAMALAR 2, 3, 4: PROSEDÜREL MAĞARA AĞI (54.0 < r <= 165.0)
                // =========================================================
                int stage = getStage(distXZ);
                boolean inCavern = isInsideCavern(dx, dz, distXZ, angle, stage, breachAngle, worldSeed);

                if (inCavern) {
                    generateCavernColumn(level, pos, x, z, distXZ, angleDeg, stage, worldSeed);
                }
            }
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
    // AŞAMA 1 İNŞASI (KODLA SIFIRDAN KÜRE VE ARENA)
    // =========================================================================
    private static void generateStage1Column(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                            int x, int z, int dx, int dz, double distXZ,
                                            double angle, double breachAngle, long seed) {

        boolean isBreach = isNearAngle(angle, breachAngle, 0.085D);

        // 1. DIŞ KABUK KAYA KÜRESİ (49.0 <= distXZ <= 54.0)
        if (distXZ >= 49.0D) {
            if (isBreach) {
                // Giriş Yarığı: Aşama 2'den içeri girilebilen antik çatlak
                for (int y = -62; y <= -18; y++) {
                    pos.set(x, y, z);
                    if (y < ARENA_FLOOR_Y) {
                        level.setBlock(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                    } else if (y >= ARENA_FLOOR_Y && y <= ARENA_FLOOR_Y + 6) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        if (y == ARENA_FLOOR_Y) {
                            level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                        }
                    } else {
                        level.setBlock(pos, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 2);
                    }
                }
            } else {
                // Aşılmaz Monolitik Derintaş Duvarı
                for (int y = -62; y <= -18; y++) {
                    pos.set(x, y, z);
                    int hash = (x * 31 + y * 17 + z * 13) & 0x7FFFFFFF;
                    BlockState wall;
                    if (hash % 9 == 0) wall = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else if (hash % 5 == 0) wall = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
                    else if (hash % 3 == 0) wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    else wall = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();

                    level.setBlock(pos, wall, 2);
                }
            }
            return;
        }

        // 2. İÇ ARENA HACMİ (distXZ < 49.0)
        // 4 Destek Sütunu Kontrolü (dx, dz = +-20 civarı)
        boolean isPillar = isNearPillar(dx, dz);

        for (int y = -62; y <= -18; y++) {
            pos.set(x, y, z);

            // Alt taban dolgusu
            if (y < ARENA_FLOOR_Y) {
                level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 2);
                continue;
            }

            // Zemin Katmanı (y == ARENA_FLOOR_Y)
            if (y == ARENA_FLOOR_Y) {
                BlockState floorState = getArenaFloorBlock(distXZ, seed, x, z);
                level.setBlock(pos, floorState, 2);
                continue;
            }

            // Sütunlar: Tavana kadar uzanan görkemli mimari sütunlar
            if (isPillar && y <= ARENA_CEIL_Y) {
                int hash = (x * 7 + y * 19 + z * 11) & 0x7FFFFFFF;
                BlockState pillarState = (hash % 4 == 0)
                        ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                        : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                level.setBlock(pos, pillarState, 2);
                continue;
            }

            // Tavan Kubbesi (y >= ARENA_CEIL_Y)
            if (y >= ARENA_CEIL_Y) {
                int hash = (x * 13 + y * 7 + z * 23) & 0x7FFFFFFF;
                BlockState roofState = (hash % 6 == 0)
                        ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                        : Blocks.DEEPSLATE_TILES.defaultBlockState();
                level.setBlock(pos, roofState, 2);
                continue;
            }

            // İç Hacim Boşluğu (Hava) & Su/Lav Temizliği
            if (distXZ <= 6.0D && y == ARENA_FLOOR_Y + 1) {
                // Merkez Podyum (Yükseltilmiş platform)
                BlockState dais = (distXZ <= 3.0D)
                        ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                        : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                level.setBlock(pos, dais, 2);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static boolean isNearPillar(int dx, int dz) {
        int px = Math.abs(dx);
        int pz = Math.abs(dz);
        return (Math.abs(px - 20) <= 1 && Math.abs(pz - 20) <= 1);
    }

    private static BlockState getArenaFloorBlock(double distXZ, long seed, int x, int z) {
        int hash = (x * 23 + z * 41 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;

        // Podyum ve Merkez Halka (r <= 6)
        if (distXZ <= 6.0D) {
            return (distXZ <= 2.0D) ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        }
        // İç Savaş Alanı (6 < r <= 22)
        if (distXZ <= 22.0D) {
            return (hash % 5 == 0) ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        // Aydınlatmalı Kızıltaş Lamba Halkası (22 < r <= 35)
        if (distXZ <= 35.0D) {
            if ((int) distXZ % 4 == 0 && hash % 3 == 0) {
                return Blocks.REDSTONE_LAMP.defaultBlockState();
            }
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        // Dış Çevre Döşemesi (35 < r < 49)
        return (hash % 4 == 0) ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    // =========================================================================
    // AŞAMALAR 2, 3, 4 MAĞARA GEOMETRİSİ VE OYMA
    // =========================================================================
    private static boolean isInsideCavern(int dx, int dz, double distXZ, double angle, int stage, double breachAngle, long seed) {
        // 1. Ana Giriş Omurgası (Giriş yarığından Aşama 2, 3, 4 boyunca uzanan koridor)
        double entranceWobble = Math.sin(distXZ * 0.05D + seed % 100) * 0.15D;
        if (isNearAngle(angle, breachAngle + entranceWobble, 0.08D)) {
            return true;
        }

        // 2. 6 Büyük Radyal Ana Arter Kolu (Tohuma bağlı kıvrımlı geniş salonlar)
        int numBranches = 6;
        double branchSpacing = (2.0D * Math.PI) / numBranches;

        for (int i = 0; i < numBranches; i++) {
            double baseBranchAngle = breachAngle + i * branchSpacing;
            double wobble = Math.sin(distXZ * 0.04D + i * 1.5D + (seed & 0xFF)) * 0.16D;
            double targetAngle = baseBranchAngle + wobble;

            double widthThreshold;
            if (stage == 2) {
                // Aşama 2: 6-8 blok genişlik (gergin tuzaklı koridorlar)
                widthThreshold = 0.08D + Math.sin(distXZ * 0.1D) * 0.02D;
            } else if (stage == 3) {
                // Aşama 3: 10-15 blok genişlik (büyük mekanik galeriler)
                widthThreshold = 0.12D + Math.cos(distXZ * 0.08D) * 0.03D;
            } else {
                // Aşama 4: 18-28 blok genişlik (devasa görkemli doğal salonlar!)
                widthThreshold = 0.18D + Math.sin(distXZ * 0.06D + i) * 0.04D;
            }

            if (isNearAngle(angle, targetAngle, widthThreshold)) {
                return true;
            }
        }

        // 3. Eşmerkezli Halka Galeriler (Concentric Ring Caverns)
        // Aşama 2 Halkası: r in [68, 75]
        if (stage == 2 && distXZ >= 68.0D && distXZ <= 75.0D) {
            int segment = (int) (Math.toDegrees(angle) / 25.0D);
            if ((segment + (int)(seed % 5)) % 4 != 0) return true;
        }

        // Aşama 3 Halkası: r in [96, 106] (Geniş çepeçevre fabrika/makine koridoru)
        if (stage == 3 && distXZ >= 96.0D && distXZ <= 106.0D) {
            int segment = (int) (Math.toDegrees(angle) / 30.0D);
            if ((segment + (int)(seed % 7)) % 5 != 0) return true;
        }

        // Aşama 4 Halkası: r in [135, 150] (Devasa dış doğal halka mağarası)
        if (stage == 4 && distXZ >= 135.0D && distXZ <= 150.0D) {
            return true;
        }

        return false;
    }

    private static boolean isNearAngle(double a1, double a2, double threshold) {
        double diff = Math.abs(a1 - a2);
        while (diff > Math.PI) diff = Math.abs(diff - 2.0D * Math.PI);
        return diff <= threshold;
    }

    private static void generateCavernColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                             int x, int z, double distXZ, double angleDeg,
                                             int stage, long seed) {

        int floorY;
        int ceilY;

        if (stage == 4) {
            // AŞAMA 4: 20-30 BLOK YÜKSEKLİKTE DEVASA KATEDRAL MAĞARALARI
            floorY = -54 + (int) Math.round(Math.sin(x * 0.04D + z * 0.04D) * 2.0D);
            ceilY = -22 + (int) Math.round(Math.cos(x * 0.03D - z * 0.03D) * 3.0D);

            // Aşama 4 Kaya Sütunları: Belli noktalarda zeminden tavana bağlanan dev kolonlar
            int pillarHash = (x * 73 + z * 37 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;
            if (pillarHash % 89 == 0) {
                buildNaturalRockPillar(level, pos, x, z, floorY, ceilY);
                return;
            }
        } else if (stage == 3) {
            // AŞAMA 3: 12-16 BLOK YÜKSEKLİKTE MAKİNE GALERİLERİ
            floorY = -51 + (int) Math.round(Math.sin(x * 0.06D + z * 0.06D) * 1.5D);
            ceilY = -36 + (int) Math.round(Math.cos(x * 0.05D - z * 0.05D) * 2.0D);
        } else {
            // AŞAMA 2: 7-9 BLOK YÜKSEKLİKTE KLASİK TUZAKLI MAĞARALAR
            floorY = -50 + (int) Math.round(Math.sin(x * 0.08D + z * 0.08D) * 1.0D);
            ceilY = -42 + (int) Math.round(Math.cos(x * 0.07D - z * 0.07D) * 1.2D);
        }

        floorY = Math.max(-62, floorY);
        ceilY = Math.min(-18, ceilY);
        if (floorY >= ceilY) return;

        int hash = (x * 37 + z * 29 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;

        for (int y = floorY - 1; y <= ceilY; y++) {
            pos.set(x, y, z);

            // 1. ZEMİN KAPLAMASI (y == floorY - 1)
            if (y == floorY - 1) {
                BlockState fl;
                if (stage == 4) {
                    // Aşama 4: Yoğun kızıltaş cevherleri ve doğal derintaş
                    if (hash % 3 == 0) fl = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else if (hash % 11 == 0) fl = Blocks.REDSTONE_BLOCK.defaultBlockState();
                    else fl = Blocks.DEEPSLATE.defaultBlockState();
                } else if (stage == 3) {
                    // Aşama 3: Cilalı derintaş ve tuğlalar
                    if (hash % 6 == 0) fl = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    else if (hash % 4 == 0) fl = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    else fl = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                } else {
                    // Aşama 2: Kayrak tuğlaları, kızıltaş ve lav çukurları
                    if (hash % 5 == 0) fl = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else fl = Blocks.DEEPSLATE_TILES.defaultBlockState();
                }
                level.setBlock(pos, fl, 2);
            }
            // 2. MAĞARA İÇİ HAVA VE DEKORASYONLAR
            else if (y >= floorY && y < ceilY) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);

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
            }
        }
    }

    private static void buildNaturalRockPillar(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int floorY, int ceilY) {
        for (int y = floorY; y <= ceilY; y++) {
            pos.set(x, y, z);
            int h = (x * 19 + y * 13 + z * 7) & 0x7FFFFFFF;
            BlockState pillar = (h % 3 == 0)
                    ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                    : Blocks.DEEPSLATE.defaultBlockState();
            level.setBlock(pos, pillar, 2);
        }
    }

    private static void placeDecorations(WorldGenLevel level, BlockPos pos, int x, int z,
                                         int floorY, int stage, double distXZ, double angleDeg, long seed) {
        int hash = (x * 47 + z * 31 + (int)(seed & 0xFFF)) & 0x7FFFFFFF;

        // ==========================================
        // AŞAMA 2: TUZAKLAR VE LANETLİ CANAVARLAR
        // ==========================================
        if (stage == 2) {
            // 1. Lav Çukuru Tuzağı: Fissure
            if (distXZ >= 70.0D && distXZ <= 72.0D && (hash % 9 == 0)) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                level.setBlock(pos.below(), Blocks.LAVA.defaultBlockState(), 2);
                return;
            }

            // 2. Basınç Plakası & Mayın / TNT Tuzağı
            if (distXZ >= 76.0D && distXZ <= 79.0D && (hash % 11 == 0)) {
                level.setBlock(pos.below(2), Blocks.TNT.defaultBlockState(), 2);
                level.setBlock(pos.below(1), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
                level.setBlock(pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
                return;
            }

            // 3. Modun Özel Kızıltaş Mayını
            if (hash % 29 == 0) {
                level.setBlock(pos, ModBlocks.REDSTONE_LANDMINE.get().defaultBlockState(), 2);
                return;
            }

            // 4. İpli Fırlatıcı (Tripwire) Tuzağı
            if (distXZ >= 62.0D && distXZ <= 64.0D && (hash % 15 == 0)) {
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

            // 5. Lanetli Canavar Spawnerı (Aşama 2 derin odalarında)
            if (distXZ >= 80.0D && distXZ <= 83.0D && (hash % 25 == 0)) {
                level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId((hash % 2 == 0) ? EntityType.SKELETON : EntityType.ZOMBIE, level.getRandom());
                }
                return;
            }

            // 6. Kızıltaş Tozu İzleri
            if (hash % 5 == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        }

        // ==========================================
        // AŞAMA 3: BASİT KIZILTAŞ MAKİNELERİ
        // ==========================================
        else if (stage == 3) {
            // 1. Çalışan Piston Düzeneği
            if (distXZ >= 92.0D && distXZ <= 95.0D && (hash % 11 == 0)) {
                level.setBlock(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 2);
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                return;
            }

            // 2. Kızıltaş Lambası & Enerji Paneli
            if (distXZ >= 102.0D && distXZ <= 105.0D && (hash % 13 == 0)) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                level.setBlock(pos.above(1), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                level.setBlock(pos.above(2), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
                return;
            }

            // 3. Asılı Zincirler ve Fenerler
            if (hash % 17 == 0) {
                level.setBlock(pos, Blocks.CHAIN.defaultBlockState(), 2);
                level.setBlock(pos.above(1), Blocks.LANTERN.defaultBlockState(), 2);
            }

            // 4. Kablo Hatları
            if (hash % 6 == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        }

        // ==========================================
        // AŞAMA 4: EN DIŞ DOĞAL KIZILTAŞ MAĞARALARI
        // ==========================================
        else if (stage == 4) {
            // Zengin Kızıltaş Damarları
            if (hash % 7 == 0 && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos.below(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
            } else if (hash % 19 == 0) {
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
    }

    public static void repairArena(net.minecraft.server.level.ServerLevel level, BlockPos center) {
        BoundingBox arenaBox = new BoundingBox(
                center.getX() - 55, -62, center.getZ() - 55,
                center.getX() + 55, -18, center.getZ() + 55
        );
        ChunkPos chunkPos = new ChunkPos(center);
        generate(level, arenaBox, chunkPos, center, level.getSeed());
        ArenaManager.cleanLiquidsInSphere(level, center);
    }
}
