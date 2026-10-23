package com.algeron.cosmicarmor.combat;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.item.CosmicArmorItem;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CosmicArmorEventHandler {

    public static boolean isWearingFullCosmicArmor(LivingEntity entity) {
        if (entity == null) return false;
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof CosmicArmorItem
                && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CosmicArmorItem
                && entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof CosmicArmorItem
                && entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof CosmicArmorItem;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isWearingFullCosmicArmor(entity)) return;

        // Çevresel bağışıklıklar: Düşme, yanma, lav, boğulma, blok içi sıkışma
        if (event.getSource().is(DamageTypes.FALL)
                || event.getSource().is(DamageTypes.IN_FIRE)
                || event.getSource().is(DamageTypes.ON_FIRE)
                || event.getSource().is(DamageTypes.LAVA)
                || event.getSource().is(DamageTypes.HOT_FLOOR)
                || event.getSource().is(DamageTypes.IN_WALL)
                || event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
            event.setCanceled(true);
            return;
        }

        // Boss saldırıları (Kızıltaş Golemi ve mermileri/tuzakları) düz %95 emilime takılmaz;
        // Güç dengesi ve hasar ölçeklemesi Lanet Sistemi (CurseDamageHandler) tarafından yönetilir.
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        net.minecraft.world.entity.Entity direct = event.getSource().getDirectEntity();
        if (com.algeron.cosmicarmor.curse.CurseHelper.isBossEntity(attacker)
                || com.algeron.cosmicarmor.curse.CurseHelper.isBossEntity(direct)) {
            return;
        }

        // 12. Boss düzeyinde saf hasar emilimi (%95 hasar azaltımı - HUD kalabalığı veya ekstra kalp yok)
        float reduced = event.getAmount() * 0.05F;
        event.setAmount(Math.max(0.5F, reduced));
    }
}
