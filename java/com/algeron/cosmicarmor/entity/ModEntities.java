package com.algeron.cosmicarmor.entity;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CosmicArmorMod.MOD_ID);

    public static final RegistryObject<EntityType<RedstoneGolemEntity>> REDSTONE_GOLEM =
            ENTITY_TYPES.register("redstone_golem", () ->
                    EntityType.Builder.of(RedstoneGolemEntity::new, MobCategory.MONSTER)
                            .sized(2.4F, 3.4F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_golem").toString()));

    public static final RegistryObject<EntityType<RedstoneGolemTntEntity>> REDSTONE_GOLEM_TNT =
            ENTITY_TYPES.register("redstone_golem_tnt", () ->
                    EntityType.Builder.<RedstoneGolemTntEntity>of(RedstoneGolemTntEntity::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_golem_tnt").toString()));

    public static final RegistryObject<EntityType<LeapingGroundBlockEntity>> LEAPING_BLOCK =
            ENTITY_TYPES.register("leaping_block", () ->
                    EntityType.Builder.<LeapingGroundBlockEntity>of(LeapingGroundBlockEntity::new, MobCategory.MISC)
                            .sized(0.98F, 0.98F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "leaping_block").toString()));

    public static final RegistryObject<EntityType<RedstoneLaserOrbEntity>> REDSTONE_LASER_ORB =
            ENTITY_TYPES.register("redstone_laser_orb", () ->
                    EntityType.Builder.<RedstoneLaserOrbEntity>of(RedstoneLaserOrbEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_laser_orb").toString()));

    public static final RegistryObject<EntityType<RedstoneLandmineEntity>> REDSTONE_LANDMINE =
            ENTITY_TYPES.register("redstone_landmine", () ->
                    EntityType.Builder.<RedstoneLandmineEntity>of(RedstoneLandmineEntity::new, MobCategory.MISC)
                            .sized(0.65F, 0.4F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_landmine").toString()));

    public static final RegistryObject<EntityType<RedstoneMortarTargetEntity>> REDSTONE_MORTAR_TARGET =
            ENTITY_TYPES.register("redstone_mortar_target", () ->
                    EntityType.Builder.<RedstoneMortarTargetEntity>of(RedstoneMortarTargetEntity::new, MobCategory.MISC)
                            .sized(1.0F, 0.2F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_mortar_target").toString()));

    public static final RegistryObject<EntityType<RedstoneGolemStatueEntity>> REDSTONE_GOLEM_STATUE =
            ENTITY_TYPES.register("redstone_golem_statue", () ->
                    EntityType.Builder.of(RedstoneGolemStatueEntity::new, MobCategory.MISC)
                            .sized(2.4F, 3.4F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(CosmicArmorMod.MOD_ID, "redstone_golem_statue").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
