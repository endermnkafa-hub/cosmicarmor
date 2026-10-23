package com.algeron.cosmicarmor.worldgen.structure;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.entity.ModEntities;
import com.algeron.cosmicarmor.entity.RedstoneGolemEntity;
import com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArenaManager extends SavedData {

    private static final String DATA_NAME = "cosmicarmor_arena_manager";

    public static final double INNER_RADIUS = 48.0D; // İç savaş küresi sınırı
    public static final double OUTER_RADIUS = 56.0D; // Dış kubbe sınırı

    public static class ArenaInstance {
        public BlockPos center = BlockPos.ZERO;
        public BlockPos schematicOrigin = BlockPos.ZERO;
        public boolean fightActive = false;
        public UUID activeBossUUID = null;

        public ArenaInstance() {}

        public ArenaInstance(BlockPos center, BlockPos schematicOrigin) {
            this.center = center;
            this.schematicOrigin = schematicOrigin;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.put("Center", NbtUtils.writeBlockPos(this.center));
            tag.put("Origin", NbtUtils.writeBlockPos(this.schematicOrigin));
            tag.putBoolean("FightActive", this.fightActive);
            if (this.activeBossUUID != null) {
                tag.putUUID("BossUUID", this.activeBossUUID);
            }
            return tag;
        }

        public static ArenaInstance load(CompoundTag tag) {
            ArenaInstance inst = new ArenaInstance();
            if (tag.contains("Center")) {
                inst.center = NbtUtils.readBlockPos(tag.getCompound("Center"));
            }
            if (tag.contains("Origin")) {
                inst.schematicOrigin = NbtUtils.readBlockPos(tag.getCompound("Origin"));
            }
            inst.fightActive = tag.getBoolean("FightActive");
            if (tag.hasUUID("BossUUID")) {
                inst.activeBossUUID = tag.getUUID("BossUUID");
            }
            return inst;
        }
    }

    private final Map<BlockPos, ArenaInstance> arenas = new ConcurrentHashMap<>();
    private static SchematicLoader.SchematicData cachedSchematic = null;

    public ArenaManager() {
    }

    public static ArenaManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ArenaManager::load,
                ArenaManager::new,
                DATA_NAME
        );
    }

    public static ArenaManager load(CompoundTag tag) {
        ArenaManager manager = new ArenaManager();

        // Çoklu arena listesi yükleme
        if (tag.contains("Arenas", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Arenas", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ArenaInstance inst = ArenaInstance.load(list.getCompound(i));
                if (!inst.center.equals(BlockPos.ZERO)) {
                    manager.arenas.put(inst.center, inst);
                }
            }
        }

        // Geriye dönük uyumluluk: Eski tekil format
        if (manager.arenas.isEmpty() && tag.contains("Center")) {
            ArenaInstance legacy = ArenaInstance.load(tag);
            if (!legacy.center.equals(BlockPos.ZERO)) {
                manager.arenas.put(legacy.center, legacy);
            }
        }

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ArenaInstance inst : this.arenas.values()) {
            list.add(inst.save());
        }
        tag.put("Arenas", list);

        // Geriye dönük uyumluluk: En son/yakın arenayı kök tag'e de yaz
        if (!this.arenas.isEmpty()) {
            ArenaInstance first = this.arenas.values().iterator().next();
            tag.putBoolean("HasArena", true);
            tag.put("Center", NbtUtils.writeBlockPos(first.center));
            tag.put("Origin", NbtUtils.writeBlockPos(first.schematicOrigin));
            tag.putBoolean("FightActive", first.fightActive);
            if (first.activeBossUUID != null) {
                tag.putUUID("BossUUID", first.activeBossUUID);
            }
        }
        return tag;
    }

    public boolean hasArena() {
        return !this.arenas.isEmpty();
    }

    public boolean hasAnyArena() {
        return !this.arenas.isEmpty();
    }

    public BlockPos getArenaCenter() {
        if (this.arenas.isEmpty()) return BlockPos.ZERO;
        return this.arenas.values().iterator().next().center;
    }

    public Collection<ArenaInstance> getAllArenas() {
        return Collections.unmodifiableCollection(this.arenas.values());
    }

    public ArenaInstance getNearestArena(BlockPos pos) {
        if (this.arenas.isEmpty() || pos == null) return null;
        ArenaInstance closest = null;
        double bestDistSq = Double.MAX_VALUE;

        for (ArenaInstance inst : this.arenas.values()) {
            double d = inst.center.distSqr(pos);
            if (d < bestDistSq) {
                bestDistSq = d;
                closest = inst;
            }
        }
        return closest;
    }

    public static SchematicLoader.SchematicData getOrLoadSchematic() {
        if (cachedSchematic == null) {
            try (InputStream is = CosmicArmorMod.class.getResourceAsStream("/assets/cosmicarmor/structures/redstone_area.schem")) {
                if (is != null) {
                    cachedSchematic = SchematicLoader.load(is);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cachedSchematic;
    }

    public BlockPos initializeArenaAt(ServerLevel level, BlockPos origin) {
        SchematicLoader.SchematicData data = getOrLoadSchematic();
        if (data == null) return origin;

        BlockPos statueCenter = SchematicLoader.paste(level, origin, data);

        // Zindan koridorlarını ve 4 aşamalı prosedürel mağara ağını inşa et
        net.minecraft.world.level.levelgen.structure.BoundingBox fullBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                origin.getX() - 150, origin.getY() - 15, origin.getZ() - 150,
                origin.getX() + data.width + 150, origin.getY() + 105, origin.getZ() + data.length + 150
        );
        RedstoneDungeonGenerator.generateSurroundingLayers(level, fullBox, origin, data);

        // Doğal su ve sıvıları küre içinden temizle
        cleanLiquidsInSphere(level, statueCenter);

        // Merkeze Kırıktaş Heykeli Spawn Et
        BlockPos statuePos = spawnStatueAtCenter(level, statueCenter);

        ArenaInstance inst = new ArenaInstance(statuePos, origin);
        this.arenas.put(statuePos, inst);
        setDirty();
        return statuePos;
    }

    public void registerGeneratedArena(int originX, int originY, int originZ, BlockPos statuePos) {
        BlockPos origin = new BlockPos(originX, originY, originZ);
        ArenaInstance inst = new ArenaInstance(statuePos, origin);
        this.arenas.put(statuePos, inst);
        setDirty();
    }

    public BlockPos spawnStatueAtCenter(ServerLevel level, BlockPos center) {
        if (center.equals(BlockPos.ZERO)) return center;

        // İç kubbenin zemin yüzeyini bul (en üst katı blok + 1)
        BlockPos groundPos = center;
        for (int dy = 15; dy >= -15; dy--) {
            BlockPos check = center.offset(0, dy, 0);
            if (!level.getBlockState(check).isAir() && level.isEmptyBlock(check.above())) {
                groundPos = check.above();
                break;
            }
        }

        AABB centerBox = new AABB(groundPos).inflate(6.0D);
        for (RedstoneGolemStatueEntity oldStatue : level.getEntitiesOfClass(RedstoneGolemStatueEntity.class, centerBox)) {
            oldStatue.discard();
        }

        RedstoneGolemStatueEntity statue = new RedstoneGolemStatueEntity(ModEntities.REDSTONE_GOLEM_STATUE.get(), level);
        statue.setPos(groundPos.getX() + 0.5D, groundPos.getY(), groundPos.getZ() + 0.5D);
        statue.setYRot(180.0F);
        statue.setPersistenceRequired();
        level.addFreshEntity(statue);
        return groundPos;
    }

    public void onBossFightStarted(RedstoneGolemEntity boss, BlockPos center) {
        ArenaInstance arena = getNearestArena(center);
        if (arena == null) {
            arena = new ArenaInstance(center, center.offset(-55, -2, -56));
            this.arenas.put(center, arena);
        }
        arena.fightActive = true;
        arena.activeBossUUID = boss.getUUID();
        setDirty();
    }

    public void onBossDefeated(ArenaInstance arena) {
        if (arena != null) {
            arena.fightActive = false;
            arena.activeBossUUID = null;
            setDirty();
        }
    }

    public void onBossDefeated() {
        if (!this.arenas.isEmpty()) {
            for (ArenaInstance inst : this.arenas.values()) {
                if (inst.fightActive) {
                    onBossDefeated(inst);
                }
            }
        }
    }

    public void resetAndRepairArena(ServerLevel level) {
        for (ArenaInstance inst : this.arenas.values()) {
            resetAndRepairArena(level, inst);
        }
    }

    public void resetAndRepairArena(ServerLevel level, ArenaInstance arena) {
        if (arena == null) return;
        arena.fightActive = false;

        // Varsa eski bossu temizle (birden fazla kopya oluşmasını kesin önle)
        if (arena.activeBossUUID != null) {
            if (level.getEntity(arena.activeBossUUID) instanceof RedstoneGolemEntity oldBoss) {
                oldBoss.discard();
            }
            arena.activeBossUUID = null;
        }

        // Arenadaki hasarlı blokları orijinal şablondan onar
        SchematicLoader.SchematicData data = getOrLoadSchematic();
        if (data != null && !arena.schematicOrigin.equals(BlockPos.ZERO)) {
            SchematicLoader.paste(level, arena.schematicOrigin, data);
        }

        // İç ve dış küre içindeki doğal su akıntılarını/sızmaları temizle
        cleanLiquidsInSphere(level, arena.center);

        // Heykeli ortaya full canla yeniden dik
        spawnStatueAtCenter(level, arena.center);
        setDirty();

        level.playSound(null, arena.center.getX(), arena.center.getY(), arena.center.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 2.0F, 0.8F);
    }

    /**
     * İç ve dış kürelerin yapısal hacmini doğal su sızıntılarına ve istenmeyen sıvılara karşı korur.
     */
    public static void cleanLiquidsInSphere(ServerLevel level, BlockPos center) {
        if (center.equals(BlockPos.ZERO)) return;
        int r = (int) OUTER_RADIUS;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = -10; dy <= 70; dy++) {
                    mpos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState st = level.getBlockState(mpos);
                    if (st.is(Blocks.WATER) || st.is(Blocks.BUBBLE_COLUMN)) {
                        level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    // --- HER TICK KONTROLLERİ (TEK YÖNLÜ BARİYER, OYUNCU TAKİBİ, BAĞIMSIZ SIFIRLANMA) ---
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) return;

        ArenaManager manager = get(serverLevel);
        if (manager.arenas.isEmpty()) return;

        for (ArenaInstance arena : manager.arenas.values()) {
            if (arena.center.equals(BlockPos.ZERO)) continue;

            BlockPos center = arena.center;
            // 3D küre geometrik merkezi (zemin yüzeyinden 53 blok yukarıda)
            Vec3 sphereCenterVec = new Vec3(center.getX() + 0.5D, center.getY() + 53.0D, center.getZ() + 0.5D);
            Vec3 floorCenterVec = new Vec3(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);

            // 1. Dış Kubbe İçinde Düşman Yaratıkları İtme (İçeri Mob Giremez)
            AABB outerBox = new AABB(center).inflate(OUTER_RADIUS + 10.0D);
            List<Monster> intruders = serverLevel.getEntitiesOfClass(Monster.class, outerBox,
                    m -> !(m instanceof RedstoneGolemEntity));
            for (Monster monster : intruders) {
                double dist = monster.position().distanceTo(sphereCenterVec);
                if (dist < OUTER_RADIUS) {
                    Vec3 away = monster.position().subtract(sphereCenterVec).normalize();
                    if (away.lengthSqr() < 0.001D) away = new Vec3(1, 0, 0);
                    monster.setDeltaMovement(away.scale(0.8D).add(0, 0.2D, 0));
                }
            }

            // 2. Boss Savaşı Aktifken Tek Yönlü Bariyer ve Oyuncu Takibi
            if (arena.fightActive) {
                RedstoneGolemEntity boss = null;
                if (arena.activeBossUUID != null) {
                    if (serverLevel.getEntity(arena.activeBossUUID) instanceof RedstoneGolemEntity g && g.isAlive()) {
                        boss = g;
                    }
                }

                // Boss öldüyse bu arenada savaşı bitir
                if (boss == null || !boss.isAlive()) {
                    manager.onBossDefeated(arena);
                    continue;
                }

                // Arena ve yakın bölgedeki canlı oyuncuları kontrol et
                AABB arenaArea = new AABB(center).inflate(OUTER_RADIUS + 30.0D);
                List<ServerPlayer> alivePlayers = serverLevel.getEntitiesOfClass(ServerPlayer.class, arenaArea,
                        p -> p.isAlive() && !p.isSpectator());

                // BU ARENADA TÜM OYUNCULAR ÖLDÜYSE VEYA KAÇTIYSA: Sıfırla, Onar ve Heykele Dönüştür!
                if (alivePlayers.isEmpty()) {
                    manager.resetAndRepairArena(serverLevel, arena);
                    continue;
                }

                // TEK YÖNLÜ BARİYER MEKANİĞİ:
                // İçerideki dışarı ÇIKAMAZ, ancak dışarıdaki oyuncu İÇERİ GİREBİLİR!
                for (ServerPlayer player : alivePlayers) {
                    double dist = player.position().distanceTo(sphereCenterVec);

                    // Sınır yüzeyinde mi? (İç küre yarıçapı ~48.0)
                    if (dist >= INNER_RADIUS - 1.5D && dist <= INNER_RADIUS + 3.0D) {
                        // Merkezden oyuncuya doğru (dışarı işaret eden) radyal birim vektör
                        Vec3 outwardDir = player.position().subtract(sphereCenterVec).normalize();

                        // Oyuncunun hareket vektörü dışarı doğru mu? (dot product > 0 ise dışarı çıkmaya çalışıyordur)
                        double outwardVelocity = player.getDeltaMovement().dot(outwardDir);

                        // YALNIZCA içeriden dışarı çıkmaya çalışan oyuncu geriye itilir!
                        // Dışarıdan içeri giren oyuncunun outwardVelocity değeri negatiftir, ASLA itilmez!
                        if (outwardVelocity > 0.02D) {
                            Vec3 inwardPush = outwardDir.scale(-0.85D).add(0, 0.14D, 0);
                            player.setDeltaMovement(inwardPush);
                            player.hurtMarked = true;

                            // Bariyer Kıvılcım & Ses Efekti
                            serverLevel.sendParticles(
                                    new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.8F),
                                    player.getX(), player.getY() + 1.0D, player.getZ(),
                                    12, 0.4D, 0.5D, 0.4D, 0.05D
                            );
                            player.playNotifySound(SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.6F);
                        }
                    }
                }
            }
        }
    }

    // 3. İç ve Dış Kubbe İçinde Doğal Mob Spawnını Kesin Olarak Engelle
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ArenaManager manager = get(serverLevel);
        if (manager.arenas.isEmpty()) return;

        if (event.getEntity() instanceof Monster monster && !(monster instanceof RedstoneGolemEntity)) {
            for (ArenaInstance inst : manager.arenas.values()) {
                double distSq = monster.blockPosition().distSqr(inst.center);
                if (distSq <= OUTER_RADIUS * OUTER_RADIUS) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
