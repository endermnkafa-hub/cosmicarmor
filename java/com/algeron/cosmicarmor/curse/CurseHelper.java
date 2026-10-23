package com.algeron.cosmicarmor.curse;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.entity.RedstoneGolemEntity;
import com.algeron.cosmicarmor.entity.RedstoneGolemTntEntity;
import com.algeron.cosmicarmor.entity.RedstoneLandmineEntity;
import com.algeron.cosmicarmor.entity.RedstoneLaserOrbEntity;
import com.algeron.cosmicarmor.entity.RedstoneMortarTargetEntity;
import com.algeron.cosmicarmor.item.CosmicGauntletItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

public final class CurseHelper {
    public static final String TAG_CURSE_LEVEL = "CosmicCurseLevel";

    // 1. Boss (Kızıltaş Golemi) Lanet Seviyesi = 1 (Başlangıç bossu)
    public static final int REDSTONE_GOLEM_CURSE = 1;
    // 2. Boss Lanet Seviyesi = 100
    public static final int BOSS_2_CURSE = 100;
    // 3. Boss Lanet Seviyesi = 250
    public static final int BOSS_3_CURSE = 250;

    private CurseHelper() {}

    /**
     * Lanet basılabilir eşyalar: Zırhlar, Kılıçlar ve Baltalar
     */
    public static boolean isInfusable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof ArmorItem || item instanceof SwordItem || item instanceof AxeItem || item instanceof CosmicGauntletItem;
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof CosmicGauntletItem;
    }

    /**
     * Zırh veya silah parçasındaki lanet seviyesini döndürür.
     */
    public static int getCurseLevel(ItemStack stack) {
        if (stack.isEmpty() || !isInfusable(stack)) {
            return 0;
        }
        if (!stack.hasTag() || !stack.getTag().contains(TAG_CURSE_LEVEL)) {
            return 0;
        }
        return stack.getTag().getInt(TAG_CURSE_LEVEL);
    }

    /**
     * Zırh veya silaha lanet seviyesi atar.
     */
    public static void setCurseLevel(ItemStack stack, int level) {
        if (stack.isEmpty() || !isInfusable(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt(TAG_CURSE_LEVEL, Math.max(0, level));
    }

    /**
     * Oyuncunun üzerindeki 4 zırh parçasının toplam lanet seviyesi.
     */
    public static int getTotalPlayerCurse(LivingEntity entity) {
        if (entity == null) return 0;
        int total = 0;
        total += getCurseLevel(entity.getItemBySlot(EquipmentSlot.HEAD));
        total += getCurseLevel(entity.getItemBySlot(EquipmentSlot.CHEST));
        total += getCurseLevel(entity.getItemBySlot(EquipmentSlot.LEGS));
        total += getCurseLevel(entity.getItemBySlot(EquipmentSlot.FEET));
        return total;
    }

    /**
     * Dünyanın lanet aşamasına göre tek bir parçaya basılabilecek maksimum lanet:
     * - Seviye 0: 1. Boss kesilmeden önce lanet basılamaz (MAKS 0 - Kilitli)
     * - Seviye 1: 1. Boss kesildikten sonra 2. Boss (100) için parça başı MAKS 70 (%70 yaklaşma kuralı)
     * - Seviye 2: 2. Boss kesildikten sonra 3. Boss (250) için parça başı MAKS 175 (%70 yaklaşma kuralı)
     */
    public static int getMaxCursePerPiece(int worldCurseLevel) {
        switch (worldCurseLevel) {
            case 0: return 0;   // 1. Boss yenilmeden önce lanet basımı kilitlidir
            case 1: return 70;  // 1. Boss yenildikten sonra: 2. Boss (100) için maks 70
            case 2: return 175; // 2. Boss yenildikten sonra: 3. Boss (250) için maks 175
            case 3: return 350;
            default: return 175 + (worldCurseLevel * 100);
        }
    }

    public static int getMaxCursePerPiece(Level level) {
        if (level == null) return 0;
        if (level.isClientSide()) {
            return getMaxCursePerPiece(ClientCurseData.clientWorldCurseLevel);
        } else if (level instanceof ServerLevel serverLevel) {
            return getMaxCursePerPiece(WorldCurseData.get(serverLevel).getCurseLevel());
        }
        return 0;
    }

    /**
     * Lanet yükseltme materyalleri değerleri:
     * - Kızıl Lanet Tozu: +1
     * - Kızıl Lanet Parçası: +3
     * - Kızıl Lanet Kristali: +8
     * - Kızıl Lanet Çekirdeği: +20
     */
    public static int getItemCurseValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(CosmicArmorMod.REDSTONE_CURSE_DUST.get())) return 1;
        if (stack.is(CosmicArmorMod.REDSTONE_CURSE_SHARD.get())) return 3;
        if (stack.is(CosmicArmorMod.REDSTONE_CURSE_CRYSTAL.get())) return 8;
        if (stack.is(CosmicArmorMod.REDSTONE_CURSE_CORE.get())) return 20;
        return 0;
    }

    public static boolean isBossEntity(Entity entity) {
        if (entity instanceof RedstoneGolemEntity) return true;
        if (entity instanceof RedstoneGolemTntEntity) return true;
        if (entity instanceof RedstoneLaserOrbEntity) return true;
        if (entity instanceof RedstoneLandmineEntity) return true;
        if (entity instanceof RedstoneMortarTargetEntity) return true;
        return false;
    }

    public static int getBossCurseLevel(Entity entity) {
        if (entity instanceof RedstoneGolemEntity) return REDSTONE_GOLEM_CURSE;
        if (entity instanceof RedstoneGolemTntEntity) return REDSTONE_GOLEM_CURSE;
        if (entity instanceof RedstoneLaserOrbEntity) return REDSTONE_GOLEM_CURSE;
        if (entity instanceof RedstoneLandmineEntity) return REDSTONE_GOLEM_CURSE;
        if (entity instanceof RedstoneMortarTargetEntity) return REDSTONE_GOLEM_CURSE;
        return 0;
    }

    public static int getEntityCurse(Entity entity) {
        if (entity == null) return 0;
        if (isBossEntity(entity)) return getBossCurseLevel(entity);
        if (entity instanceof LivingEntity living) {
            return living.getPersistentData().getInt("CosmicCurseLevel");
        }
        return 0;
    }
}
