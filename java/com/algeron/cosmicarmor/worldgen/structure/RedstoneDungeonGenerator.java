package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
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

import java.util.Random;

public class RedstoneDungeonGenerator {

    public static final double R_SPHERE = 54.0D;   // Aşama 1: İç ve Dış Küre Sınırı
    public static final double R_STAGE2 = 85.0D;   // Aşama 2: Lanetli & Tuzaklı Mağaralar
    public static final double R_STAGE3 = 115.0D;  // Aşama 3: Basit Kızıltaş Makineleri
    public static final double R_STAGE4 = 160.0D;  // Aşama 4: En Dış Doğal Mağaralar

    /**
     * Chunk sınırları (box) içinde kalan 4 aşamalı prosedürel mağara ağını üretir.
     */
    public static void generateSurroundingLayers(WorldGenLevel level, BoundingBox box, BlockPos origin, SchematicLoader.SchematicData data) {
        int centerX = origin.getX() + data.width / 2;
        int floorY = origin.getY() + (data.floorCenterOffset != null ? data.floorCenterOffset.getY() : 2);
        int centerZ = origin.getZ() + data.length / 2;

        long arenaSeed = level.getSeed() ^ ((long) centerX * 341873128712L + (long) centerZ * 132897987541L);
        Random arenaRand = new Random(arenaSeed);

        // Ana giriş kemerinin tohumdan türetilen açısı (Dış küreyi delen ana koridor)
        double mainEntranceAngle = (arenaRand.nextInt(360)) * (Math.PI / 180.0D);

        int minX = box.minX();
        int maxX = box.maxX();
        int minZ = box.minZ();
        int maxZ = box.maxZ();

        int minY = Math.max(box.minY(), floorY - 5);
        int maxY = Math.min(box.maxY(), floorY + 8);
        if (minY > maxY) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                double distXZ = Math.sqrt(dx * dx + dz * dz);

                // Dış kürenin içi (r < 51) şematik tarafından inşa edilir ve korunur
                if (distXZ < 51.0D || distXZ > R_STAGE4) continue;

                double angleRad = Math.atan2(dz, dx);
                double angleDeg = ((Math.toDegrees(angleRad) % 360.0D) + 360.0D) % 360.0D;

                // --- AŞAMA 1 & DIŞ KÜRE CEPHESİ (51.0 <= distXZ <= 54.5) ---
                if (distXZ <= 54.5D) {
                    boolean isEntranceBreach = isNearAngle(angleRad, mainEntranceAngle, 0.08D);
                    if (isEntranceBreach) {
                        // Kemerli giriş geçidi: Dış küreyi Aşama 2 mağaralarına bağlar
                        carveCorridor(level, pos, x, z, floorY, minY, maxY, 1, distXZ, angleDeg, arenaSeed);
                    } else {
                        // Dış küre duvarı: Sert derintaş ve kızıltaş damarlarıyla aşılmaz kaya kütlesi hissi
                        for (int y = minY; y <= maxY; y++) {
                            pos.set(x, y, z);
                            BlockState current = level.getBlockState(pos);
                            if (current.isAir() || current.is(Blocks.WATER) || current.is(Blocks.LAVA)) {
                                BlockState wall = ((x * 7 + y * 13 + z * 3) % 4 == 0)
                                        ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                                        : Blocks.DEEPSLATE.defaultBlockState();
                                level.setBlock(pos, wall, 2);
                            }
                        }
                    }
                    continue;
                }

                // --- PROSEDÜREL MAĞARA OYMA KONTROLÜ (54.5 < distXZ <= 160.0) ---
                int stage = getStage(distXZ);
                boolean inCavern = isInsideProceduralCavern(dx, dz, distXZ, angleRad, mainEntranceAngle, stage, arenaSeed);

                if (inCavern) {
                    carveCorridor(level, pos, x, z, floorY, minY, maxY, stage, distXZ, angleDeg, arenaSeed);
                }
            }
        }
    }

    private static int getStage(double distXZ) {
        if (distXZ <= R_STAGE2) return 2;
        if (distXZ <= R_STAGE3) return 3;
        return 4;
    }

    /**
     * 3D Sinüzoidal / Dairesel dallanma ve fraktal gürültü ile prosedürel mağara tünelleri üretir.
     * Her tohumda mağara kıvrımları, genişlikleri, çıkmazları ve dalları farklılaşır.
     */
    private static boolean isInsideProceduralCavern(int dx, int dz, double distXZ, double angleRad,
                                                   double mainEntranceAngle, int stage, long seed) {
        // 1. Ana Giriş Kolu (Giriş kemerinden Aşama 2 ve 3'e uzanan omurga)
        double entranceWobble = Math.sin(distXZ * 0.08D + seed % 100) * 0.12D;
        if (isNearAngle(angleRad, mainEntranceAngle + entranceWobble, 0.075D)) {
            return true;
        }

        // 2. Prosedürel 5 Radyal Ana Kol (Seeded Curvature)
        int numBranches = 5;
        double branchSpacing = (2.0D * Math.PI) / numBranches;

        for (int i = 0; i < numBranches; i++) {
            double baseBranchAngle = mainEntranceAngle + i * branchSpacing;
            // Kıvrım dalgası
            double wobble = Math.sin(distXZ * 0.06D + i * 1.7D + (seed & 0xFF)) * 0.18D;
            double targetAngle = baseBranchAngle + wobble;

            double widthThreshold;
            if (stage == 2) {
                // Aşama 2: 4-6 blok genişliğinde gergin koridorlar
                widthThreshold = 0.065D + Math.sin(distXZ * 0.15D) * 0.015D;
            } else if (stage == 3) {
                // Aşama 3: 5-8 blok genişliğinde mekanik galeriler
                widthThreshold = 0.075D + Math.cos(distXZ * 0.12D) * 0.02D;
            } else {
                // Aşama 4: 6-10 blok genişliğinde geniş organik doğal mağaralar
                widthThreshold = 0.085D + Math.sin(distXZ * 0.1D + i) * 0.025D;
            }

            if (isNearAngle(angleRad, targetAngle, widthThreshold)) {
                return true;
            }
        }

        // 3. Aşama 2 ve 3 Çember Bağlantı Koridorları (Ring Connectors)
        // Oyuncuyu farklı güzergahlara ve alternatif yollara yönlendiren dairesel geçitler
        if (stage == 2 && distXZ >= 66.0D && distXZ <= 72.0D) {
            // Belirli açılarda açık, aralarda doğal kayalık çıkmazlar
            int segment = (int) (Math.toDegrees(angleRad) / 20.0D);
            if ((segment + (int)(seed % 5)) % 3 != 0) {
                return true;
            }
        }

        if (stage == 3 && distXZ >= 96.0D && distXZ <= 104.0D) {
            int segment = (int) (Math.toDegrees(angleRad) / 25.0D);
            if ((segment + (int)(seed % 7)) % 4 != 0) {
                return true;
            }
        }

        // 4. Aşama 4'te Rastgele Çıkmaz Mağaralar (Dead-Ends & Sub-branches)
        if (stage == 4 && distXZ >= 125.0D && distXZ <= 145.0D) {
            double deadEndAngle = mainEntranceAngle + 0.9D + Math.sin(distXZ * 0.05D) * 0.25D;
            if (isNearAngle(angleRad, deadEndAngle, 0.07D)) {
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

    private static void carveCorridor(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z,
                                      int floorY, int minY, int maxY, int stage,
                                      double distXZ, double angleDeg, long seed) {
        int height = (stage == 4) ? 6 : (stage == 3 ? 5 : 4);

        // Doğal zemin dalgalanması (Yükseklik varyasyonu)
        int localFloorY = floorY + (int) Math.round(Math.sin((x * 0.08D) + (z * 0.08D)) * 1.2D);
        int ceilY = localFloorY + height;

        for (int y = minY; y <= maxY; y++) {
            pos.set(x, y, z);

            // Taban Zemin Kaplaması (y == localFloorY - 1)
            if (y == localFloorY - 1) {
                int hash = (x * 37 + z * 19 + (int)(seed & 0xFFFF)) & 0x7FFFFFFF;
                if (stage == 1 || stage == 2) {
                    // Aşama 2: Kayrak taşı tuğlaları, kızıltaş cevherleri ve kan kırmızısı bloklar
                    BlockState fl;
                    if (hash % 7 == 0) fl = Blocks.REDSTONE_BLOCK.defaultBlockState();
                    else if (hash % 3 == 0) fl = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
                    else fl = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    level.setBlock(pos, fl, 2);
                } else if (stage == 3) {
                    // Aşama 3: Cilalı derin taş, kısmen kırılmış döşemeler
                    BlockState fl = (hash % 5 == 0) ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                            : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                    level.setBlock(pos, fl, 2);
                } else {
                    // Aşama 4: Doğal derin taş ve zengin kızıltaş damarları
                    BlockState fl = (hash % 4 == 0) ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                            : Blocks.DEEPSLATE.defaultBlockState();
                    level.setBlock(pos, fl, 2);
                }
            }
            // Mağara Boşluğu (Hava & Tuzak/Makine Yerleşimleri)
            else if (y >= localFloorY && y < ceilY) {
                // Sıvı sızıntılarını önle
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);

                if (y == localFloorY) {
                    placeStageDecorationsAndTraps(level, pos, x, z, localFloorY, stage, distXZ, angleDeg, seed);
                }
            }
            // Tavan (y == ceilY)
            else if (y == ceilY) {
                int hash = (x * 17 + z * 31) & 0x7FFFFFFF;
                if (stage == 2 && hash % 6 == 0) {
                    level.setBlock(pos, Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
                } else if (stage == 3 && hash % 7 == 0) {
                    // 3. Aşama tavanında çalışan kızıltaş lambaları
                    level.setBlock(pos, Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                    level.setBlock(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                } else if (stage == 4 && hash % 4 == 0) {
                    level.setBlock(pos, Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
                } else {
                    level.setBlock(pos, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void placeStageDecorationsAndTraps(WorldGenLevel level, BlockPos pos, int x, int z,
                                                      int floorY, int stage, double distXZ, double angleDeg, long seed) {
        int hash = (x * 43 + z * 29 + (int)(seed & 0xFFF)) & 0x7FFFFFFF;

        // ==========================================
        // AŞAMA 2: LANETLİ MAĞARALAR VE ÇALIŞAN TUZAKLAR
        // ==========================================
        if (stage == 2) {
            // 1. Lav Çukuru Tuzağı: Belirli dönemeçlerde zemin açılır ve derin lav tuzağı bulunur
            if (distXZ >= 67.0D && distXZ <= 69.0D && (hash % 11 == 0)) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                level.setBlock(pos.below(), Blocks.LAVA.defaultBlockState(), 2);
                return;
            }

            // 2. Basınç Plakası & Mayın / TNT Tuzağı
            if (distXZ >= 73.0D && distXZ <= 76.0D && (hash % 13 == 0)) {
                level.setBlock(pos.below(2), Blocks.TNT.defaultBlockState(), 2);
                level.setBlock(pos.below(1), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
                level.setBlock(pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
                return;
            }

            // 3. İpli Fırlatıcı (Tripwire) Tuzağı: Koridor boyu gerilmiş ip ve duvarda ok fırlatıcı
            if (distXZ >= 62.0D && distXZ <= 64.0D && (hash % 17 == 0)) {
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

            // 4. Lanetli Canavar Spawnerı (Aşama 2'nin derin odalarında)
            if (distXZ >= 79.0D && distXZ <= 81.0D && (hash % 23 == 0)) {
                level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId((hash % 2 == 0) ? EntityType.SKELETON : EntityType.ZOMBIE, level.getRandom());
                }
                return;
            }

            // 5. Kan ve Kızıltaş Tozu İzleri (Terk Edilmiş Tehlike Hissi)
            if (hash % 5 == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        }

        // ==========================================
        // AŞAMA 3: BASİT KIZILTAŞ MAKİNELERİ
        // ==========================================
        else if (stage == 3) {
            // 1. Çalışan Piston Düzeneği: Yukarı iten piston ve kızıltaş bloğu
            if (distXZ >= 92.0D && distXZ <= 95.0D && (hash % 12 == 0)) {
                level.setBlock(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 2);
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
                return;
            }

            // 2. Kızıltaş Lambası & Enerji Paneli: Yanmış lamba ve devre
            if (distXZ >= 102.0D && distXZ <= 105.0D && (hash % 14 == 0)) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
                level.setBlock(pos.above(1), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                level.setBlock(pos.above(2), Blocks.REDSTONE_TORCH.defaultBlockState(), 2);
                return;
            }

            // 3. Basit Köprüler / Geçitler
            if (hash % 16 == 0) {
                level.setBlock(pos, Blocks.CHAIN.defaultBlockState(), 2);
                level.setBlock(pos.above(2), Blocks.LANTERN.defaultBlockState(), 2);
            }

            // 4. Duvara Gömülü Kızıltaş Kabloları
            if (hash % 6 == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        }

        // ==========================================
        // AŞAMA 4: EN DIŞ DOĞAL MAĞARALAR
        // ==========================================
        else if (stage == 4) {
            // Yoğun doğal kızıltaş cevherleri ve ham kızıltaş blok damarları
            if (hash % 8 == 0 && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos.below(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), 2);
            } else if (hash % 20 == 0) {
                level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            }
        }
    }
}
