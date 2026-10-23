package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.block.RedstoneLandmineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RedstoneLandmineModel extends GeoModel<RedstoneLandmineBlockEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "geo/redstone_landmine.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "textures/entity/redstone_golem.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "animations/redstone_landmine.animation.json");

    @Override
    public ResourceLocation getModelResource(RedstoneLandmineBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RedstoneLandmineBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RedstoneLandmineBlockEntity animatable) {
        return ANIMATION;
    }
}
