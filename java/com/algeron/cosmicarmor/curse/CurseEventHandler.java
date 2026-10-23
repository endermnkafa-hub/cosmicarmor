package com.algeron.cosmicarmor.curse;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.network.CosmicNetwork;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseEventHandler {

    private static final String TAG_CURSE_BUFFED = "CosmicCurseBuffed";

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            int curseLevel = WorldCurseData.get(serverLevel).getCurseLevel();
            boolean inLayer2 = false;
            var arenaManager = com.algeron.cosmicarmor.worldgen.structure.ArenaManager.get(serverLevel);
            var nearest = arenaManager.getNearestArena(monster.blockPosition());
            if (nearest != null && !nearest.center.equals(net.minecraft.core.BlockPos.ZERO)) {
                double dist = Math.sqrt(monster.blockPosition().distSqr(nearest.center));
                // 2. Katman: Kürenin dışındaki tuzaklı mağaralar (Mesafe: 54 - 85 blok)
                if (dist >= 54.0D && dist <= 85.0D) {
                    inLayer2 = true;
                }
            }

            // Lanet 1 tüm dünyada aktifse VEYA 2. Katman içinde doğduysa lanetlenir
            if ((curseLevel >= 1 || inLayer2) && !monster.getPersistentData().getBoolean(TAG_CURSE_BUFFED)) {
                monster.getPersistentData().putBoolean(TAG_CURSE_BUFFED, true);

                // Canavarlar 0 ile 150 arasında çan eğrisi (Gaussian) ile doğar.
                // Ortalama 75, standart sapma 25: 75 civarı en yüksek olasılık, 0 ve 150 aşırı nadir!
                double gaussian = monster.getRandom().nextGaussian();
                int monsterCurse = (int) Math.round(75.0D + (gaussian * 25.0D));
                monsterCurse = Math.max(0, Math.min(150, monsterCurse));
                monster.getPersistentData().putInt("CosmicCurseLevel", monsterCurse);

                // Kullanıcı İsteği: Pasif can ve hasar artışı tamamen kaldırıldı!
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        // Lanetli yaratıkların etrafında lanet seviyesine göre kızıltaş kıvılcımları süzülür
        if (monster.getPersistentData().getBoolean(TAG_CURSE_BUFFED)) {
            int curse = monster.getPersistentData().getInt("CosmicCurseLevel");
            float particleChance = 0.02F + (curse / 150.0F) * 0.06F;
            if (monster.getRandom().nextFloat() < particleChance && monster.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.0F),
                        monster.getX() + (monster.getRandom().nextDouble() - 0.5D) * 0.7D,
                        monster.getY() + 0.3D + monster.getRandom().nextDouble() * 1.2D,
                        monster.getZ() + (monster.getRandom().nextDouble() - 0.5D) * 0.7D,
                        1, 0, 0.02D, 0, 0.01D
                );
            }
        }
    }

    @SubscribeEvent
    public static void onMonsterDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        if (monster.level() instanceof ServerLevel serverLevel) {
            int curseLevel = WorldCurseData.get(serverLevel).getCurseLevel();
            boolean isBuffed = monster.getPersistentData().getBoolean(TAG_CURSE_BUFFED);

            // Sadece lanetli yaratıklar veya dünya laneti aktifken oyuncu tarafından öldürülen canavarlar
            if ((curseLevel >= 1 || isBuffed) && event.isRecentlyHit()) {
                int monsterCurse = monster.getPersistentData().getInt("CosmicCurseLevel");

                // Temel olasılık: 75 lanet için %2.5 (0.025F)
                // Lanet arttıkça doğrusal olarak artar: (monsterCurse / 75.0F) * 0.025F
                float baseChance = (monsterCurse / 75.0F) * 0.025F;
                // Ganimet (Looting) seviyesi başına +%1.0 şans artışı
                float lootingBonus = event.getLootingLevel() * 0.010F;
                float totalChance = baseChance + lootingBonus;

                if (monster.getRandom().nextFloat() < totalChance) {
                    int amount = 1;
                    // Yüksek lanetli (120+) yaratıklarda veya yüksek looting ile 2 adet düşme şansı
                    if (monsterCurse >= 120 && monster.getRandom().nextFloat() < 0.25F) {
                        amount = 2;
                    }
                    ItemStack dropStack = new ItemStack(CosmicArmorMod.REDSTONE_CURSE_DUST.get(), amount);
                    ItemEntity itemEntity = new ItemEntity(
                            serverLevel,
                            monster.getX(),
                            monster.getY() + 0.5D,
                            monster.getZ(),
                            dropStack
                    );
                    itemEntity.setDefaultPickUpDelay();
                    event.getDrops().add(itemEntity);
                }
            }
        }
    }

    // --- İstemci Senkronizasyon Olayları ---

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncCurseToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncCurseToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncCurseToPlayer(player);
        }
    }

    private static void syncCurseToPlayer(ServerPlayer player) {
        int curseLevel = WorldCurseData.get(player.serverLevel()).getCurseLevel();
        CosmicNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CosmicNetwork.SyncWorldCursePacket(curseLevel)
        );
    }
}
