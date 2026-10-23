package com.algeron.cosmicarmor.curse;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseInfusionHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) return;
        if (!CurseHelper.isInfusable(left)) return;

        int curseValue = CurseHelper.getItemCurseValue(right);
        if (curseValue <= 0) return;

        Player player = event.getPlayer();
        Level level = player != null ? player.level() : null;
        int maxCurse = CurseHelper.getMaxCursePerPiece(level);
        int currentCurse = CurseHelper.getCurseLevel(left);

        if (currentCurse >= maxCurse) {
            // Dünya sınırına ulaşıldı veya henüz kilitli
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                if (maxCurse <= 0) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("§cLanet basımı henüz kilitli! §7(Önce 1. Boss Kızıltaş Golemi'ni yenmelisin!)"),
                            true // actionbar
                    );
                } else {
                    serverPlayer.sendSystemMessage(
                            Component.literal("§cDünya bundan üst seviye bir lanete henüz hazır değil! §7(Maksimum: " + maxCurse + ")"),
                            true // actionbar
                    );
                }
            }
            return;
        }

        // 1 adet materyal tüketerek zırhın lanet seviyesini arttır
        int newCurse = Math.min(maxCurse, currentCurse + curseValue);

        ItemStack output = left.copy();
        CurseHelper.setCurseLevel(output, newCurse);

        event.setOutput(output);
        // Makul XP maliyeti: 1 ila 4 seviye
        event.setCost(Math.max(1, Math.min(4, (newCurse - currentCurse) / 8 + 1)));
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty() || !CurseHelper.isInfusable(output)) return;

        int curse = CurseHelper.getCurseLevel(output);
        if (curse > 0 && event.getEntity() instanceof ServerPlayer player) {
            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.85F
            );
        }
    }
}
