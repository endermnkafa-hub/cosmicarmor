package com.algeron.cosmicarmor.sound;

import com.algeron.cosmicarmor.CosmicArmorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CosmicArmorMod.MOD_ID);

    public static final RegistryObject<SoundEvent> FIST =
            SOUND_EVENTS.register("fist", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CosmicArmorMod.MOD_ID, "fist")));

    public static final RegistryObject<SoundEvent> BF =
            SOUND_EVENTS.register("bf", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CosmicArmorMod.MOD_ID, "bf")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
