package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.item.CosmicArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CosmicArmorModel extends GeoModel<CosmicArmorItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(CosmicArmorMod.MOD_ID,
            "geo/cosmic_armor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(CosmicArmorMod.MOD_ID,
            "textures/armor/cosmic_armor.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(CosmicArmorMod.MOD_ID,
            "animations/cosmic_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(CosmicArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CosmicArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CosmicArmorItem animatable) {
        return ANIMATION;
    }
}
