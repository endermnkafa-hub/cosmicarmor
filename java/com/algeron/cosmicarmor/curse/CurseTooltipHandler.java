package com.algeron.cosmicarmor.curse;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // 1. Zırh Tooltip Bilgileri
        if (stack.getItem() instanceof ArmorItem) {
            int curse = CurseHelper.getCurseLevel(stack);
            int maxCap = CurseHelper.getMaxCursePerPiece(ClientCurseData.clientWorldCurseLevel);

            if (curse > 0) {
                event.getToolTip().add(Component.empty());
                event.getToolTip().add(Component.literal("§c✦ Kızıl Lanet: §e" + curse + " §7/ §c" + maxCap)
                        .append(curse >= maxCap ? Component.literal(" §6(MAKS)").withStyle(ChatFormatting.BOLD) : Component.empty()));

                if (Screen.hasShiftDown()) {
                    event.getToolTip().add(Component.literal("§7  • Bossların yüksek lanet hasarını bu parça için kırar."));
                    event.getToolTip().add(Component.literal("§7  • Bu parçanın laneti: §e" + curse + " §7/ §c" + maxCap));
                    if (curse >= maxCap) {
                        event.getToolTip().add(Component.literal("§c  ⚠ Dünya şu an daha yüksek bir lanete hazır değil!"));
                    } else {
                        event.getToolTip().add(Component.literal("§8  • Örs üzerinde lanet materyalleri ile yükseltilir."));
                    }
                } else {
                    event.getToolTip().add(Component.literal("§8[Detaylar için Shift'e basılı tutun]"));
                }
            } else {
                if (Screen.hasShiftDown()) {
                    event.getToolTip().add(Component.empty());
                    if (maxCap <= 0) {
                        event.getToolTip().add(Component.literal("§8✦ Kızıl Lanet: §7Kilitli §8(1. Boss Kızıltaş Golemi kesilince açılır)"));
                    } else {
                        event.getToolTip().add(Component.literal("§8✦ Kızıl Lanet: §70 §8/ §7" + maxCap));
                        event.getToolTip().add(Component.literal("§8  • Örs üzerinde Lanet Tozu veya Parçası basılabilir."));
                    }
                }
            }
        }

        // 2. Silah Tooltip Bilgileri (Kılıç ve Balta)
        if (CurseHelper.isWeapon(stack)) {
            int curse = CurseHelper.getCurseLevel(stack);
            int maxCap = CurseHelper.getMaxCursePerPiece(ClientCurseData.clientWorldCurseLevel);

            if (curse > 0) {
                event.getToolTip().add(Component.empty());
                event.getToolTip().add(Component.literal("§c✦ Kızıl Lanet (Silah): §e" + curse + " §7/ §c" + maxCap)
                        .append(curse >= maxCap ? Component.literal(" §6(MAKS)").withStyle(ChatFormatting.BOLD) : Component.empty()));

                if (Screen.hasShiftDown()) {
                    float efficiency = CurseDamageHandler.calculatePlayerWeaponMultiplier(curse, 100);
                    int percent = Math.round(efficiency * 100.0F);
                    event.getToolTip().add(Component.literal("§7  • 2. Boss Hasar Verimi: §e%" + percent + " §8(Fark kadar işler)"));
                    event.getToolTip().add(Component.literal("§7  • Lanetsiz silahlar 2. Boss'a sadece %10 hasar vurabilir."));
                    if (curse >= maxCap) {
                        event.getToolTip().add(Component.literal("§c  ⚠ Dünya şu an daha yüksek bir silah lanetine hazır değil!"));
                    } else {
                        event.getToolTip().add(Component.literal("§8  • Örs üzerinde lanet materyalleri ile yükseltilir."));
                    }
                } else {
                    event.getToolTip().add(Component.literal("§8[Detaylar için Shift'e basılı tutun]"));
                }
            } else {
                if (Screen.hasShiftDown()) {
                    event.getToolTip().add(Component.empty());
                    if (maxCap <= 0) {
                        event.getToolTip().add(Component.literal("§8✦ Kızıl Lanet: §7Kilitli §8(1. Boss kesilince açılır)"));
                        event.getToolTip().add(Component.literal("§7  • 1. Boss'a karşı kılıcın normal tam hasar vurur."));
                    } else {
                        event.getToolTip().add(Component.literal("§8✦ Kızıl Lanet: §c0 §8/ §7" + maxCap + " §4(Lanetsiz)"));
                        event.getToolTip().add(Component.literal("§c  ⚠ 2. Boss'a karşı lanetsiz kılıç sadece %10 hasar vurur!"));
                        event.getToolTip().add(Component.literal("§8  • Örs üzerinde Lanet materyalleri basılabilir."));
                    }
                }
            }
        }

        // 3. Lanet Materyalleri Tooltip Bilgileri
        int materialCurse = CurseHelper.getItemCurseValue(stack);
        if (materialCurse > 0) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.literal("§c✦ Kızıl Lanet Materyali"));
            event.getToolTip().add(Component.literal("§7  Parça Başına: §a+" + materialCurse + " Seviye"));
            event.getToolTip().add(Component.literal("§8  Örs üzerinde Zırh, Kılıç veya Baltaya basılabilir."));
            if (Screen.hasShiftDown()) {
                int maxCap = CurseHelper.getMaxCursePerPiece(ClientCurseData.clientWorldCurseLevel);
                if (maxCap <= 0) {
                    event.getToolTip().add(Component.literal("§7  Mevcut Dünya Sınırı: §cKilitli §7(1. Boss'tan sonra açılır)"));
                } else {
                    event.getToolTip().add(Component.literal("§7  Mevcut Dünya Sınırı: §e" + maxCap + " §7Seviye / Parça"));
                }
            }
        }

        // 4. Nötr Lanet Yükseltici (Katalizör)
        if (stack.is(CosmicArmorMod.NEUTRAL_CURSE_CATALYST.get())) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.literal("§d✦ Nötr Lanet Yükseltici"));
            event.getToolTip().add(Component.literal("§7  Lanet materyallerini bir üst kademeye dönüştürür."));
            event.getToolTip().add(Component.literal("§8  • Çalışma masasında 8 adet lanet maddesinin ortasına konur."));
            event.getToolTip().add(Component.literal("§8  • Usta (Master) Rahip köylüler ile takas edilebilir."));
        }
    }
}
