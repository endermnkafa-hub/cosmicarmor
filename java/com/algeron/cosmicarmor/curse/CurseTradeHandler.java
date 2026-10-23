package com.algeron.cosmicarmor.curse;

import com.algeron.cosmicarmor.CosmicArmorMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CurseTradeHandler {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        // Rahip köylünün (Cleric) son seviye (Master - Seviye 5) takası
        if (event.getType() == VillagerProfession.CLERIC) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            List<VillagerTrades.ItemListing> masterTrades = trades.get(5);
            if (masterTrades != null) {
                // 8x Kızıl Lanet Tozu -> 1x Nötr Lanet Yükseltici
                masterTrades.add((trader, rand) -> new MerchantOffer(
                        new ItemStack(CosmicArmorMod.REDSTONE_CURSE_DUST.get(), 8),
                        new ItemStack(CosmicArmorMod.NEUTRAL_CURSE_CATALYST.get(), 1),
                        16, // Maksimum kullanım
                        30, // Köylü XP ödülü
                        0.05F // Fiyat esnekliği
                ));
            }
        }
    }
}
