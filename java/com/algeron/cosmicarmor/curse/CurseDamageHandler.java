package com.algeron.cosmicarmor.curse;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseDamageHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    // Vücut dağılım oranları (Kask %15, Göğüslük %40, Pantolon %30, Bot %15)
    private static final float[] ARMOR_WEIGHTS = { 0.15F, 0.40F, 0.30F, 0.15F };

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        // Çevresel ve doğal hasarlar (düşme, yanma, boğulma vb.) KESİNLİKLE lanet mekaniğini tetiklemez!
        if (source.is(DamageTypes.FALL)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.FREEZE)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return;
        }

        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();

        // 1. BOSS VEYA LANETLİ CANAVAR OYUNCUYA SALDIRIYOR (Her zırh parçası ayrı ayrı hesaplanır)
        if (victim instanceof Player player) {
            Entity enemy = null;
            if (attacker != null && (CurseHelper.isBossEntity(attacker) || CurseHelper.getEntityCurse(attacker) > 0)) {
                enemy = attacker;
            } else if (direct != null && (CurseHelper.isBossEntity(direct) || CurseHelper.getEntityCurse(direct) > 0)) {
                enemy = direct;
            }

            if (enemy != null) {
                int enemyCurse = CurseHelper.getEntityCurse(enemy);
                if (enemyCurse > 0) {
                    float multiplier = calculateBossToPlayerMultiplier(player, enemyCurse);
                    event.setAmount(event.getAmount() * multiplier);

                    // Ekranda sadece katlanan ceza varsa (%30 farktan fazlaysa veya eksik parça varsa) uyarı göster
                    if (multiplier > 1.25F) {
                        int extraPercent = Math.round((multiplier - 1.0F) * 100.0F);
                        player.displayClientMessage(
                                Component.literal("§c⚠ Lanet Açığı: §4+%" + extraPercent + " Katlanan Hasar! §8(Eksik/Düşük Lanetli Zırh Parçaları)"),
                                true
                        );
                    }

                    // Parçacık efektleri
                    if (player.level() instanceof ServerLevel serverLevel && multiplier > 1.5F) {
                        serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.05F), 1.6F),
                                player.getX(), player.getY() + 1.0D, player.getZ(),
                                14, 0.35D, 0.6D, 0.35D, 0.05D
                        );
                    }
                }
            }
        }

        // 2. OYUNCU BOSSA VEYA YARATIKLARA SALDIRIYOR
        Player player = null;
        if (attacker instanceof Player p) {
            player = p;
        } else if (direct instanceof Player p) {
            player = p;
        }

        if (player != null) {
            ItemStack weapon = player.getMainHandItem();
            int weaponCurse = CurseHelper.getCurseLevel(weapon);
            int targetCurse = CurseHelper.getEntityCurse(victim);

            if (targetCurse > 0) {
                float weaponMultiplier = calculatePlayerWeaponMultiplier(weaponCurse, targetCurse);
                event.setAmount(event.getAmount() * weaponMultiplier);

                if (targetCurse > 1 && weaponCurse <= 0 && !weapon.isEmpty() && CurseHelper.isWeapon(weapon)) {
                    player.displayClientMessage(
                            Component.literal("§8⚠ Kılıçta Lanet Yok! §c(Hasar: %10) §8- Örste Kılıca Lanet Bas!"),
                            true
                    );
                }
            }
        }
    }

    /**
     * Kılıç / Silah Hasar Çarpanı:
     * - Hedef 1. Boss veya Lanet 1 ise: 0 lanet kılıçla %100 tam hasar.
     * - Hedef 2. Boss (100 Lanet):
     *   - 70 Lanet kılıçla: Aradaki %30 fark kadar vurur -> Tam %70 hasar (0.70x, katlanma cezası yok, boss zor gelir!).
     *   - Fark %30'dan büyükse: Katlanan ceza devreye girer (50 lanet -> 0.10x, 0 lanet -> 0.10x).
     *   - Hedefin lanetinden üstünse: Bonus hasar vurur.
     */
    public static float calculatePlayerWeaponMultiplier(int weaponCurse, int targetCurse) {
        if (targetCurse <= 1) {
            // 1. Boss veya düşük lanetli yaratıklar:
            if (weaponCurse > 1) {
                // Lanetli kılıçla gelirse lanet farkı avantajı
                return 1.0F + (0.01F * weaponCurse);
            }
            return 1.0F; // Normal kılıçla %100 tam hasar
        }

        // Hedefin Laneti > 1 (2. Boss [100] veya Lanetli Yaratıklar):
        if (weaponCurse <= 0) {
            // Lanetsiz kılıç: sadece %10 hasar
            return 0.10F;
        }

        double fark = (double) (targetCurse - weaponCurse) / (double) targetCurse;

        if (fark <= 0.0D) {
            // Oyuncunun laneti hedeften eşit veya üstünse bonus hasar
            double advantage = Math.abs(fark);
            return (float) Math.min(2.0D, 1.0D + (0.80D * advantage));
        }

        // 1. Temel Hasar Farkı: 70 lanette (%30 farkta) tam %70 hasar vurur (1.0 - 0.30 = 0.70x)
        double baseEfficiency = 1.0D - fark;

        // 2. Katlanan Güç Cezası: SADECE fark %30'dan BÜYÜKSE hasarı katlanarak düşürür
        double katlananCeza = 0.0D;
        if (fark > 0.30D) {
            double fazlaFark = fark - 0.30D;
            katlananCeza = (1.5D * fazlaFark) + (2.5D * (fazlaFark * fazlaFark));
        }

        double finalMultiplier = baseEfficiency - katlananCeza;
        return (float) Math.max(0.10D, finalMultiplier);
    }

    /**
     * Her zırh yuvasının bağımsız lanet koruması:
     * - Head (%15), Chest (%40), Legs (%30), Feet (%15)
     * - Giysi yoksa veya laneti 0 ise: O bölge için tam %100 fark (5.16x ceza) sayılır.
     * - Her parçada 70 lanet varsa: 1.24x (Ceza yok, yalnızca %24 temel fark).
     */
    public static float calculateBossToPlayerMultiplier(Player player, int bossCurse) {
        if (bossCurse <= 0) return 1.0F;

        float totalMultiplier = 0.0F;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack piece = player.getItemBySlot(ARMOR_SLOTS[i]);
            int slotCurse = 0;
            if (!piece.isEmpty() && CurseHelper.isInfusable(piece)) {
                slotCurse = CurseHelper.getCurseLevel(piece);
            }
            // Zırh parçası yoksa veya lanet basılmamışsa slotCurse = 0 kabul edilir (%100 fark!)
            float slotMult = calculateSlotMultiplier(slotCurse, bossCurse);
            totalMultiplier += ARMOR_WEIGHTS[i] * slotMult;
        }

        return totalMultiplier;
    }

    /**
     * Birebir Belirlenen Durum Tablosu Formülü:
     * - Hasar Çarpanı = 1.0 + Temel Hasar Farkı + Katlanan Güç Cezası
     *
     * 1) Temel Hasar Farkı = 0.80 * Fark
     *    70 Lanette (%30 farkta): +%24 hasar artışı
     *
     * 2) Katlanan Güç Cezası = 2.0 * (Fark - 0.30) + 4.0 * (Fark - 0.30)^2  (Fark > 0.30 ise)
     *    Fark <= %30 ise: Katlanan ceza 0'dır (işlemez).
     *
     * Durum Tablosu Çıktıları:
     * - 100 Lanet (%0 Fark):   1.0x  (Tam normal hasar)
     * - 70 Lanet  (%30 Fark):  1.24x (Ceza yok, sadece temel fark +%24)
     * - 50 Lanet  (%50 Fark):  1.96x
     * - 20 Lanet  (%80 Fark):  3.64x (Ağır katlanan ceza)
     * - 0 Lanet   (%100 Fark): 5.16x (Tek atar!)
     */
    public static float calculateSlotMultiplier(int slotCurse, int bossCurse) {
        if (bossCurse <= 0) return 1.0F;

        // 1. Boss (Lanet = 1): Erken aşama bossu olduğu için henüz lanetsiz oyuncuya ceza uygulamaz
        if (bossCurse <= 1) {
            if (slotCurse >= 1) {
                // Oyuncu ileride lanetlenmiş zırhla gelirse bossun hasarı kırılır
                return 0.70F;
            }
            return 1.0F; // Lanetsiz oyuncuya normal boss hasarı
        }

        double fark = (double) (bossCurse - slotCurse) / (double) bossCurse;

        if (fark <= 0.0D) {
            double advantage = Math.abs(fark);
            double reduced = 1.0D - (0.8D * advantage);
            return (float) Math.max(0.30D, reduced);
        }

        // 1. Temel Hasar Farkı (Doğrusal)
        double temelFarkHasari = 0.80D * fark;

        // 2. Güç Farkının Büyüklüğünün Bonus Hasarı (Katlanan Ceza)
        double katlananCeza = 0.0D;
        if (fark > 0.30D) {
            double fazlaFark = fark - 0.30D;
            katlananCeza = (2.0D * fazlaFark) + (4.0D * (fazlaFark * fazlaFark));
        }

        return (float) (1.0D + temelFarkHasari + katlananCeza);
    }
}
