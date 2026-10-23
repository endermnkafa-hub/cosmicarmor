package com.algeron.cosmicarmor.client;

import com.algeron.cosmicarmor.CosmicArmorMod;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.*;

/**
 * Manages player-animator animation registration, loading, and playback for fist1, fist2, and fist3.
 */
public final class GloveAnimationClient {
    private static final Map<UUID, ModifierLayer<KeyframeAnimationPlayer>> ANIMATION_LAYERS = new HashMap<>();
    private static final Map<String, KeyframeAnimation> LOADED_ANIMATIONS = new HashMap<>();

    public static void init() {
        // Register animation layer on player creation
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, stack) -> {
            ModifierLayer<KeyframeAnimationPlayer> layer = new ModifierLayer<>();
            ANIMATION_LAYERS.put(player.getUUID(), layer);
            stack.addAnimLayer(1000, layer);
        });
    }

    /**
     * Loads the animations directly from assets/cosmicarmor/animations/player_glove.animation.json
     * or player_animation/player_glove.json if needed.
     */
    public static void loadAnimations(ResourceManager resourceManager) {
        LOADED_ANIMATIONS.clear();
        String[] possiblePaths = {
                "animations/player_glove.animation.json",
                "player_animation/player_glove.json"
        };

        for (String path : possiblePaths) {
            ResourceLocation loc = new ResourceLocation(CosmicArmorMod.MOD_ID, path);
            Optional<Resource> res = resourceManager.getResource(loc);
            if (res.isPresent()) {
                try (InputStream stream = res.get().open()) {
                    List<KeyframeAnimation> anims = AnimationSerializing.deserializeAnimation(stream);
                    for (KeyframeAnimation a : anims) {
                        String name = (String) a.extraData.get("name");
                        if (name != null) {
                            LOADED_ANIMATIONS.put(name.toLowerCase(Locale.ROOT), a);
                        }
                    }
                    if (!LOADED_ANIMATIONS.isEmpty()) {
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static KeyframeAnimation getAnimation(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        KeyframeAnimation anim = LOADED_ANIMATIONS.get(key);
        if (anim != null) {
            return anim;
        }

        // Check PlayerAnimationRegistry fallback
        ResourceLocation regLoc = new ResourceLocation(CosmicArmorMod.MOD_ID, key);
        return PlayerAnimationRegistry.getAnimation(regLoc);
    }

    /**
     * Plays fist1, fist2, or fist3 on the given player.
     */
    public static void playFistAnimation(AbstractClientPlayer player, int fistIndex) {
        if (fistIndex < 1 || fistIndex > 3) return;

        KeyframeAnimation anim = getAnimation("fist" + fistIndex);
        if (anim == null) {
            // Lazy load fallback if not loaded during resource reload
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager() != null) {
                loadAnimations(mc.getResourceManager());
                anim = getAnimation("fist" + fistIndex);
            }
        }

        if (anim == null) return;

        ModifierLayer<KeyframeAnimationPlayer> layer = ANIMATION_LAYERS.get(player.getUUID());
        if (layer == null) {
            try {
                AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
                layer = new ModifierLayer<>();
                ANIMATION_LAYERS.put(player.getUUID(), layer);
                stack.addAnimLayer(1000, layer);
            } catch (Exception ignored) {
                return;
            }
        }

        KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(anim);
        // Set first-person mode to THIRD_PERSON_MODEL so both arms (left and right) and upper body movements
        // from player_glove.animation.json are properly rendered in first-person perspective!
        animPlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        animPlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, false, false));

        // Use 2-tick fade transition so the model smoothly glides into the punch rather than snapping!
        layer.replaceAnimationWithFade(
                dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier.standardFadeIn(
                        2, dev.kosmx.playerAnim.core.util.Ease.INOUTSINE
                ),
                animPlayer
        );
    }

    public static void handleSyncPacket(UUID playerUUID, int fistIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var player = mc.level.getPlayerByUUID(playerUUID);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                playFistAnimation(clientPlayer, fistIndex);
            }
        }
    }
}
