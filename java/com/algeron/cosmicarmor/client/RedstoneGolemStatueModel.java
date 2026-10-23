package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.entity.RedstoneGolemStatueEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RedstoneGolemStatueModel extends GeoModel<RedstoneGolemStatueEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "geo/redstone_golem.geo.json");
    private static final ResourceLocation STATUE_TEXTURE =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "textures/entity/redstone_golem_statue.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "animations/redstone_golem.animation.json");

    @Override
    public ResourceLocation getModelResource(RedstoneGolemStatueEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RedstoneGolemStatueEntity animatable) {
        return STATUE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RedstoneGolemStatueEntity animatable) {
        return ANIMATION;
    }
}
