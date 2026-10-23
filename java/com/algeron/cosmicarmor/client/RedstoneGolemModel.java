package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.entity.RedstoneGolemEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class RedstoneGolemModel extends GeoModel<RedstoneGolemEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "geo/redstone_golem.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "textures/entity/redstone_golem.png");
    private static final ResourceLocation OVERCLOCK_TEXTURE =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "textures/entity/redstone_golem_overclock.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(CosmicArmorMod.MOD_ID, "animations/redstone_golem.animation.json");

    @Override
    public ResourceLocation getModelResource(RedstoneGolemEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RedstoneGolemEntity animatable) {
        if (animatable.isOverclocked()) {
            return OVERCLOCK_TEXTURE;
        }
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RedstoneGolemEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(RedstoneGolemEntity animatable, long instanceId, AnimationState<RedstoneGolemEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Boşta veya yürürken kafanın oyuncuyu takip etmesi
        if (animatable.getAttackState() == RedstoneGolemEntity.STATE_IDLE) {
            CoreGeoBone head = getAnimationProcessor().getBone("redstone_golem_head");
            if (head != null) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                    head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
                }
            }
        }
    }
}
