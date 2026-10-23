package com.algeron.cosmicarmor;

import com.algeron.cosmicarmor.block.ModBlockEntities;
import com.algeron.cosmicarmor.block.ModBlocks;
import com.algeron.cosmicarmor.client.CosmicGauntletCurioRenderer;
import com.algeron.cosmicarmor.client.LeapingBlockRenderer;
import com.algeron.cosmicarmor.client.RedstoneGolemRenderer;
import com.algeron.cosmicarmor.client.RedstoneGolemTntRenderer;
import com.algeron.cosmicarmor.client.RedstoneLandmineBlockRenderer;
import com.algeron.cosmicarmor.client.RedstoneLandmineRenderer;
import com.algeron.cosmicarmor.client.RedstoneLaserOrbRenderer;
import com.algeron.cosmicarmor.client.RedstoneMortarTargetRenderer;
import com.algeron.cosmicarmor.entity.ModEntities;
import com.algeron.cosmicarmor.entity.RedstoneGolemEntity;
import com.algeron.cosmicarmor.item.CosmicArmorItem;
import com.algeron.cosmicarmor.item.CosmicGauntletItem;
import com.algeron.cosmicarmor.vfx.CosmicAuraVFX;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import software.bernie.geckolib.GeckoLib;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod(CosmicArmorMod.MOD_ID)
public final class CosmicArmorMod {
    public static final String MOD_ID = "cosmicarmor";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    // Armor Pieces
    public static final RegistryObject<Item> COSMIC_HELMET = ITEMS.register(
            "cosmic_helmet", () -> new CosmicArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> COSMIC_CHESTPLATE = ITEMS.register(
            "cosmic_chestplate", () -> new CosmicArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> COSMIC_LEGGINGS = ITEMS.register(
            "cosmic_leggings", () -> new CosmicArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> COSMIC_BOOTS = ITEMS.register(
            "cosmic_boots", () -> new CosmicArmorItem(ArmorItem.Type.BOOTS));

    // Auras / Gloves
    public static final RegistryObject<Item> STANDART_AURA = ITEMS.register(
            "standart_aura", () -> new CosmicGauntletItem(CosmicAuraVFX.AuraType.STANDARD));
    public static final RegistryObject<Item> COSMIC_AURA = ITEMS.register(
            "cosmic_aura", () -> new CosmicGauntletItem(CosmicAuraVFX.AuraType.COSMIC));

    // Boss Spawn Egg
    public static final RegistryObject<Item> REDSTONE_GOLEM_SPAWN_EGG = ITEMS.register(
            "redstone_golem_spawn_egg", () -> new ForgeSpawnEggItem(
                    ModEntities.REDSTONE_GOLEM,
                    0x4A1414,
                    0xEE2222,
                    new Item.Properties()
            ));

    // Redstone Landmine Block Item
    public static final RegistryObject<Item> REDSTONE_LANDMINE_ITEM = ITEMS.register(
            "redstone_landmine", () -> new BlockItem(
                    ModBlocks.REDSTONE_LANDMINE.get(),
                    new Item.Properties()
            ));

    // Lanet 1 - Kızıl Rezonans Parçası (Eski uyumluluk)
    public static final RegistryObject<Item> REDSTONE_RESONANCE_SHARD = ITEMS.register(
            "redstone_resonance_shard", () -> new Item(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)
            ));

    // 4 Kademeli Lanet Materyalleri
    public static final RegistryObject<Item> REDSTONE_CURSE_DUST = ITEMS.register(
            "redstone_curse_dust", () -> new Item(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.COMMON)
            ));
    public static final RegistryObject<Item> REDSTONE_CURSE_SHARD = ITEMS.register(
            "redstone_curse_shard", () -> new Item(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)
            ));
    public static final RegistryObject<Item> REDSTONE_CURSE_CRYSTAL = ITEMS.register(
            "redstone_curse_crystal", () -> new Item(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)
            ));
    public static final RegistryObject<Item> REDSTONE_CURSE_CORE = ITEMS.register(
            "redstone_curse_core", () -> new Item(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)
            ));

    // Rahip Köylü Son Takası - Nötr Lanet Yükseltici (Katalizör)
    public static final RegistryObject<Item> NEUTRAL_CURSE_CATALYST = ITEMS.register(
            "neutral_curse_catalyst", () -> new Item(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)
            ));

    public CosmicArmorMod() {
        GeckoLib.initialize();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ITEMS.register(modBus);
        ModEntities.register(modBus);
        com.algeron.cosmicarmor.worldgen.biome.ModBiomes.register(modBus);
        com.algeron.cosmicarmor.worldgen.structure.ModStructures.register(modBus);
        com.algeron.cosmicarmor.sound.ModSounds.register(modBus);

        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.REDSTONE_GOLEM.get(), RedstoneGolemEntity.createAttributes().build());
        event.put(ModEntities.REDSTONE_GOLEM_STATUE.get(), com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity.createAttributes().build());
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.REDSTONE_GOLEM.get(), RedstoneGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.REDSTONE_GOLEM_STATUE.get(), com.algeron.cosmicarmor.client.RedstoneGolemStatueRenderer::new);
        event.registerEntityRenderer(ModEntities.REDSTONE_GOLEM_TNT.get(), RedstoneGolemTntRenderer::new);
        event.registerEntityRenderer(ModEntities.LEAPING_BLOCK.get(), LeapingBlockRenderer::new);
        event.registerEntityRenderer(ModEntities.REDSTONE_LASER_ORB.get(), RedstoneLaserOrbRenderer::new);
        event.registerEntityRenderer(ModEntities.REDSTONE_LANDMINE.get(), RedstoneLandmineRenderer::new);
        event.registerEntityRenderer(ModEntities.REDSTONE_MORTAR_TARGET.get(), RedstoneMortarTargetRenderer::new);

        // GeckoLib Animated Block Entity Renderer
        event.registerBlockEntityRenderer(ModBlockEntities.REDSTONE_LANDMINE.get(),
                context -> new RedstoneLandmineBlockRenderer());
    }

    private void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.algeron.cosmicarmor.network.CosmicNetwork.register();
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("playeranimator")) {
                com.algeron.cosmicarmor.client.GloveAnimationClient.init();
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
                CuriosClientHelper.registerRenderer();
            }
        });
    }

    private static final class CuriosClientHelper {
        private static void registerRenderer() {
            CuriosRendererRegistry.register(STANDART_AURA.get(), CosmicGauntletCurioRenderer::new);
            CuriosRendererRegistry.register(COSMIC_AURA.get(), CosmicGauntletCurioRenderer::new);
        }
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(COSMIC_HELMET);
            event.accept(COSMIC_CHESTPLATE);
            event.accept(COSMIC_LEGGINGS);
            event.accept(COSMIC_BOOTS);
            event.accept(STANDART_AURA);
            event.accept(COSMIC_AURA);
            event.accept(REDSTONE_GOLEM_SPAWN_EGG);
            event.accept(REDSTONE_LANDMINE_ITEM);
            event.accept(REDSTONE_CURSE_DUST);
            event.accept(REDSTONE_CURSE_SHARD);
            event.accept(REDSTONE_CURSE_CRYSTAL);
            event.accept(REDSTONE_CURSE_CORE);
            event.accept(NEUTRAL_CURSE_CATALYST);
        }
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(REDSTONE_CURSE_DUST);
            event.accept(REDSTONE_CURSE_SHARD);
            event.accept(REDSTONE_CURSE_CRYSTAL);
            event.accept(REDSTONE_CURSE_CORE);
            event.accept(NEUTRAL_CURSE_CATALYST);
            event.accept(REDSTONE_RESONANCE_SHARD);
        }
        if (event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
            event.accept(REDSTONE_LANDMINE_ITEM);
        }
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(REDSTONE_GOLEM_SPAWN_EGG);
        }
    }
}
