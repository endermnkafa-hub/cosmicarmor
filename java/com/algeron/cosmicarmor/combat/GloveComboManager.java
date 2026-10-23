package com.algeron.cosmicarmor.combat;

import com.algeron.cosmicarmor.item.CosmicGauntletItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Divergent Fist combo timing, states, hitbox window, and knockback scaling.
 *
 * Combo Rules:
 * - fist1: 0.0 - 0.15s prep, 0.15 - 0.39s punch (active hit), 0.39 - 0.74s recovery. 1x knockback.
 * - fist2: 0.0 - 0.15s prep, 0.15 - 0.39s punch (active hit), 0.39 - 0.74s recovery. 1x knockback.
 * - fist3: 0.0 - 0.15s prep, 0.15 - 0.39s punch (active hit), 0.39 - 0.74s recovery. 2x knockback.
 *
 * Restrictions:
 * - fist1's punch phase (until 0.39s) must finish before fist2 can be triggered.
 * - fist2's punch phase (until 0.39s) must finish before fist3 can be triggered.
 * - fist3 must fully finish (0.74s) + 1.0s cooldown before ANY punch can be thrown.
 * - If 1.0 second passes after fist1 or fist2 without punching, combo resets to fist1.
 */
public final class GloveComboManager {
    // Stage timings in milliseconds
    public static final long PREP_DURATION_MS = 150L;     // 0.15s
    public static final long PUNCH_END_MS = 390L;          // 0.39s (hitbox closes & next punch unlocked)
    public static final long TOTAL_DURATION_MS = 740L;     // 0.74s (full animation length)
    public static final long RESET_TIMEOUT_MS = 2000L;     // 2.0s idle reset to fist1 AFTER previous animation ends
    public static final long FIST3_COOLDOWN_MS = 1000L;    // 1.0s lockout after fist3 finishes

    public static final class ComboState {
        public int currentFist = 0; // 0 = idle, 1 = fist1, 2 = fist2, 3 = fist3
        public long lastPunchTime = 0L;
        public long fist3EndTime = 0L;
        public boolean hasDealtHitInCurrentPunch = false;
        public UUID lockedTargetUUID = null;
    }

    private static final Map<UUID, ComboState> CLIENT_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, ComboState> SERVER_STATES = new ConcurrentHashMap<>();

    public static ComboState getState(UUID uuid, boolean isClient) {
        Map<UUID, ComboState> map = isClient ? CLIENT_STATES : SERVER_STATES;
        return map.computeIfAbsent(uuid, k -> new ComboState());
    }

    /**
     * Checks if the player has Cosmic Gauntlet equipped in Curios and empty main hand.
     */
    public static boolean hasGloveEquippedAndEmptyHand(Player player) {
        if (!player.getMainHandItem().isEmpty()) {
            return false;
        }

        if (ModList.get().isLoaded("curios")) {
            var curioInv = CuriosApi.getCuriosInventory(player);
            if (curioInv.isPresent()) {
                return curioInv.resolve().get().findFirstCurio(stack -> stack.getItem() instanceof CosmicGauntletItem).isPresent();
            }
        }
        return false;
    }

    /**
     * Determines whether the player currently has the Standart Aura or Cosmic Aura glove equipped.
     */
    public static com.algeron.cosmicarmor.vfx.CosmicAuraVFX.AuraType getEquippedGloveAuraType(Player player) {
        if (ModList.get().isLoaded("curios")) {
            var curioInv = CuriosApi.getCuriosInventory(player);
            if (curioInv.isPresent()) {
                var found = curioInv.resolve().get().findFirstCurio(stack -> stack.getItem() instanceof CosmicGauntletItem);
                if (found.isPresent()) {
                    ItemStack stack = found.get().stack();
                    if (stack.getItem() instanceof CosmicGauntletItem gauntlet) {
                        return gauntlet.getAuraType();
                    }
                }
            }
        }
        return com.algeron.cosmicarmor.vfx.CosmicAuraVFX.AuraType.STANDARD;
    }

    /**
     * Damage values strictly for empty hand (1 Heart = 2 HP damage):
     * - Standart Aura: 4 Hearts (8.0), 4 Hearts (8.0), 8 Hearts (16.0)
     * - Cosmic Aura:   20 Hearts (40.0), 20 Hearts (40.0), 40 Hearts (80.0)
     */
    public static float getPunchDamage(Player player, int fistIndex) {
        var auraType = getEquippedGloveAuraType(player);
        if (auraType == com.algeron.cosmicarmor.vfx.CosmicAuraVFX.AuraType.COSMIC) {
            // Cosmic Aura: 20 hearts, 20 hearts, 40 hearts
            return (fistIndex == 3) ? 80.0F : 40.0F;
        } else {
            // Standart Aura: 4 hearts, 4 hearts, 8 hearts
            return (fistIndex == 3) ? 16.0F : 8.0F;
        }
    }

    /**
     * Checks if the player can perform a punch right now, and if so, returns which fist index (1, 2, or 3).
     * Returns 0 if punching is currently locked.
     */
    public static int getNextFistIndex(Player player, boolean isClient) {
        long now = System.currentTimeMillis();
        ComboState state = getState(player.getUUID(), isClient);

        // Check fist3 cooldown lockout (fist3 end time + 1.0s)
        if (state.fist3EndTime > 0L) {
            if (now < state.fist3EndTime + FIST3_COOLDOWN_MS) {
                return 0; // Locked out!
            } else {
                state.fist3EndTime = 0L;
                state.currentFist = 0;
            }
        }

        // If currently in fist3 animation, cannot punch until fully complete (0.74s)
        if (state.currentFist == 3) {
            long elapsed = now - state.lastPunchTime;
            if (elapsed < TOTAL_DURATION_MS) {
                return 0;
            }
            // If completed, transition into cooldown
            state.fist3EndTime = state.lastPunchTime + TOTAL_DURATION_MS;
            state.currentFist = 0;
            return 0;
        }

        // If currently idle
        if (state.currentFist == 0) {
            return 1;
        }

        long elapsed = now - state.lastPunchTime;

        // The 2-second timeout starts AFTER the current animation completes (TOTAL_DURATION_MS)
        long timeSinceAnimFinished = elapsed - TOTAL_DURATION_MS;
        if (timeSinceAnimFinished > RESET_TIMEOUT_MS) {
            return 1; // 2 seconds passed after anim ended -> reset combo to fist1!
        }

        // If fist1 is ongoing:
        if (state.currentFist == 1) {
            if (elapsed < PUNCH_END_MS) {
                return 0; // Fist 1 punch phase is not finished yet!
            }
            return 2; // Fist 1 punch phase finished -> advance to fist2!
        }

        // If fist2 is ongoing:
        if (state.currentFist == 2) {
            if (elapsed < PUNCH_END_MS) {
                return 0; // Fist 2 punch phase is not finished yet!
            }
            return 3; // Fist 2 punch phase finished -> advance to fist3!
        }

        return 1;
    }

    /**
     * Records that a punch has started for this player.
     */
    public static void recordPunch(Player player, int fistIndex, boolean isClient) {
        long now = System.currentTimeMillis();
        ComboState state = getState(player.getUUID(), isClient);
        state.currentFist = fistIndex;
        state.lastPunchTime = now;
        state.hasDealtHitInCurrentPunch = false;
        state.lockedTargetUUID = null;
        if (fistIndex == 3) {
            state.fist3EndTime = now + TOTAL_DURATION_MS;
        } else {
            state.fist3EndTime = 0L;
        }
    }

    /**
     * Checks if current punch is in the active hit window [0.15s, 0.39s].
     */
    public static boolean isHitboxActive(Player player, boolean isClient) {
        ComboState state = getState(player.getUUID(), isClient);
        if (state.currentFist == 0) return false;
        long elapsed = System.currentTimeMillis() - state.lastPunchTime;
        return elapsed >= PREP_DURATION_MS && elapsed <= PUNCH_END_MS;
    }

    /**
     * Gets the current punching fist (1, 2, 3) if an animation is currently active, or 0 if idle.
     */
    public static int getActivePunchFist(Player player, boolean isClient) {
        ComboState state = getState(player.getUUID(), isClient);
        if (state.currentFist == 0) return 0;
        long elapsed = System.currentTimeMillis() - state.lastPunchTime;
        if (elapsed < TOTAL_DURATION_MS) {
            return state.currentFist;
        }
        return 0;
    }

    /**
     * Gets progress ratio of current punch in [0.0, 1.0] representing the punch extension (0->1) and retraction (1->0).
     */
    public static float getPunchExtension(Player player, boolean isClient) {
        ComboState state = getState(player.getUUID(), isClient);
        if (state.currentFist == 0) return 0.0f;
        long elapsed = System.currentTimeMillis() - state.lastPunchTime;
        if (elapsed >= TOTAL_DURATION_MS) return 0.0f;

        if (elapsed < PREP_DURATION_MS) {
            // Wind-up: slightly pull back (-0.1 to 0)
            return -(float) elapsed / (float) PREP_DURATION_MS * 0.15f;
        } else if (elapsed <= PUNCH_END_MS) {
            // Rapid thrust forward: reaches 1.0 peak
            float t = (float) (elapsed - PREP_DURATION_MS) / (float) (PUNCH_END_MS - PREP_DURATION_MS);
            return (float) Math.sin(t * Math.PI * 0.5); // 0.0 -> 1.0
        } else {
            // Retract back to rest: 1.0 -> 0.0
            float t = (float) (elapsed - PUNCH_END_MS) / (float) (TOTAL_DURATION_MS - PUNCH_END_MS);
            return 1.0f - (float) Math.sin(t * Math.PI * 0.5); // 1.0 -> 0.0
        }
    }
}
