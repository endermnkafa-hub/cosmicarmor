package com.algeron.cosmicarmor.combat;

import com.algeron.cosmicarmor.client.GloveAnimationClient;
import com.algeron.cosmicarmor.network.CosmicNetwork;
import com.algeron.cosmicarmor.sound.ModSounds;
import com.algeron.cosmicarmor.vfx.CosmicAuraVFX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler that intercepts punches, blocks vanilla hand swing and vanilla attacks,
 * enforces combo progression / cooldowns, opens the active hitbox during [0.15s - 0.39s],
 * executes strikes with wide and reliable melee cone targeting, applies custom damage & knockback,
 * and inflicts a 0.5-second hit stun on targets.
 */
@Mod.EventBusSubscriber(modid = com.algeron.cosmicarmor.CosmicArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GloveCombatHandler {

    private static final Map<UUID, UUID> RECENT_ATTACKERS = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> STUNNED_UNTIL = new ConcurrentHashMap<>();

    public static void applyServerStun(LivingEntity target, long durationMs) {
        if (target == null) return;
        STUNNED_UNTIL.put(target.getUUID(), System.currentTimeMillis() + durationMs);

        // 10 ticks = 0.5 seconds
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 255, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 255, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 10, false, false, false));

        target.setDeltaMovement(0.0, Math.min(target.getDeltaMovement().y, 0.0), 0.0);
        target.hasImpulse = true;

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    public static void applyClientStun(LivingEntity target, long durationMs) {
        if (target == null) return;
        STUNNED_UNTIL.put(target.getUUID(), System.currentTimeMillis() + durationMs);
        target.setDeltaMovement(0.0, Math.min(target.getDeltaMovement().y, 0.0), 0.0);
        target.hasImpulse = true;
    }

    public static boolean isStunned(LivingEntity entity) {
        if (entity == null) return false;
        Long until = STUNNED_UNTIL.get(entity.getUUID());
        if (until != null) {
            if (System.currentTimeMillis() < until) {
                return true;
            } else {
                STUNNED_UNTIL.remove(entity.getUUID());
            }
        }
        return false;
    }

    /**
     * Completely blocks vanilla attack/mining clicks when glove is equipped with empty hand.
     * Fires the custom fist combo with exact vanilla crosshair attack hitbox.
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && isStunned(mc.player)) {
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (mc.player != null && GloveComboManager.hasGloveEquippedAndEmptyHand(mc.player)) {
                // Completely suppress vanilla attack & arm swing!
                event.setSwingHand(false);
                event.setCanceled(true);

                int nextFist = GloveComboManager.getNextFistIndex(mc.player, true);
                if (nextFist > 0) {
                    handleClientPunch(mc.player, nextFist);
                    // Exact vanilla punch hitbox: only the entity directly under the crosshair!
                    Entity target = getVanillaCrosshairTarget();
                    if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                        livingTarget.invulnerableTime = 0;
                        RECENT_ATTACKERS.put(livingTarget.getUUID(), mc.player.getUUID());
                        mc.gameMode.attack(mc.player, livingTarget);
                    }
                }
            }
        }
    }

    /**
     * Intercepts left-click empty space punch on the client side.
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (isStunned(player)) {
            event.setCanceled(true);
            return;
        }
        if (player.level().isClientSide && player instanceof LocalPlayer localPlayer) {
            if (GloveComboManager.hasGloveEquippedAndEmptyHand(localPlayer)) {
                event.setCanceled(true);
                int nextFist = GloveComboManager.getNextFistIndex(localPlayer, true);
                if (nextFist > 0) {
                    handleClientPunch(localPlayer, nextFist);
                    Entity target = getVanillaCrosshairTarget();
                    if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                        livingTarget.invulnerableTime = 0;
                        RECENT_ATTACKERS.put(livingTarget.getUUID(), localPlayer.getUUID());
                        Minecraft.getInstance().gameMode.attack(localPlayer, livingTarget);
                    }
                }
            }
        }
    }

    /**
     * Intercepts left-click on a block to prevent vanilla mining/punching when glove is active.
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (isStunned(player)) {
            event.setCanceled(true);
            return;
        }
        if (player.level().isClientSide && player instanceof LocalPlayer localPlayer) {
            if (GloveComboManager.hasGloveEquippedAndEmptyHand(localPlayer)) {
                event.setCanceled(true);
                int nextFist = GloveComboManager.getNextFistIndex(localPlayer, true);
                if (nextFist > 0) {
                    handleClientPunch(localPlayer, nextFist);
                    Entity target = getVanillaCrosshairTarget();
                    if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                        livingTarget.invulnerableTime = 0;
                        RECENT_ATTACKERS.put(livingTarget.getUUID(), localPlayer.getUUID());
                        Minecraft.getInstance().gameMode.attack(localPlayer, livingTarget);
                    }
                }
            }
        }
    }

    /**
     * Intercepts attack entity event (when punching a mob/player directly).
     * Never cancels valid attacks.
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (isStunned(player)) {
            event.setCanceled(true);
            return;
        }
        if (GloveComboManager.hasGloveEquippedAndEmptyHand(player)) {
            if (event.getTarget() instanceof LivingEntity livingTarget) {
                livingTarget.invulnerableTime = 0;
                RECENT_ATTACKERS.put(livingTarget.getUUID(), player.getUUID());
            }
        }
    }

    /**
     * Applies custom combo punch damage when fighting empty-handed with an aura glove equipped:
     * - Standart Aura: 4 Hearts (8.0 HP), 4 Hearts (8.0 HP), 8 Hearts (16.0 HP)
     * - Cosmic Aura:   20 Hearts (40.0 HP), 20 Hearts (40.0 HP), 40 Hearts (80.0 HP)
     *
     * In addition, hits apply a 0.5-second (10 ticks) hit stun on the target.
     *
     * Sound & VFX Rules strictly on hitting a target:
     * - Standart Aura: 1st, 2nd, 3rd punch -> plays "fist" sound.
     * - Cosmic Aura:   1st, 2nd punch      -> plays "fist" sound.
     * - Cosmic Aura:   3rd punch           -> plays "bf" sound + erupts Black Flash VFX from target!
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Stunned entities cannot deal damage
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isStunned(attacker)) {
            event.setCanceled(true);
            return;
        }

        if (event.getSource().getEntity() instanceof Player player) {
            if (GloveComboManager.hasGloveEquippedAndEmptyHand(player)) {
                LivingEntity target = event.getEntity();
                GloveComboManager.ComboState state = GloveComboManager.getState(player.getUUID(), player.level().isClientSide);
                int fist = (state.currentFist > 0) ? state.currentFist : 1;
                float damage = GloveComboManager.getPunchDamage(player, fist);
                event.setAmount(damage);

                // Reset invulnerableTime so rapid combo strikes always connect
                target.invulnerableTime = 0;

                // Apply 0.5-second (10 ticks / 500ms) stun on all glove punches (Standard & Cosmic)!
                applyServerStun(target, 500);

                // Sound & VFX trigger strictly on hit!
                var auraType = GloveComboManager.getEquippedGloveAuraType(player);
                boolean isCosmic = (auraType == CosmicAuraVFX.AuraType.COSMIC);

                double tx = target.getX();
                double ty = target.getY() + target.getBbHeight() * 0.5D;
                double tz = target.getZ();

                if (isCosmic && fist == 3) {
                    // Cosmic Aura 3rd punch = Black Flash sound + Black Flash VFX erupting from target!
                    player.level().playSound(null, tx, ty, tz, ModSounds.BF.get(), SoundSource.PLAYERS, 2.8F, 1.0F);
                    CosmicNetwork.CHANNEL.send(
                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                            new CosmicNetwork.HitEffectPacket(tx, ty, tz, fist, true, target.getId()));
                } else {
                    // Standard Aura (fists 1, 2, 3) or Cosmic Aura (fists 1, 2) = Fist sound + impact sparks!
                    player.level().playSound(null, tx, ty, tz, ModSounds.FIST.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
                    CosmicNetwork.CHANNEL.send(
                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                            new CosmicNetwork.HitEffectPacket(tx, ty, tz, fist, false, target.getId()));
                }
            }
        }
    }

    /**
     * Scales knockback: fist1 & fist2 = 1.0x normal, fist3 = 2.0x normal!
     */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        UUID attackerUUID = RECENT_ATTACKERS.remove(target.getUUID());
        if (attackerUUID != null) {
            GloveComboManager.ComboState state = GloveComboManager.getState(attackerUUID, false);
            if (state.currentFist == 3) {
                event.setStrength(event.getStrength() * 2.0F);
            }
        } else {
            LivingEntity attacker = target.getLastHurtByMob();
            if (attacker instanceof Player player && GloveComboManager.hasGloveEquippedAndEmptyHand(player)) {
                GloveComboManager.ComboState state = GloveComboManager.getState(player.getUUID(), player.level().isClientSide);
                if (state.currentFist == 3) {
                    event.setStrength(event.getStrength() * 2.0F);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClientPunch(LocalPlayer player, int nextFist) {
        if (!GloveComboManager.hasGloveEquippedAndEmptyHand(player)) return;

        // Record punch locally
        GloveComboManager.recordPunch(player, nextFist, true);
        // Play keyframe animation
        GloveAnimationClient.playFistAnimation(player, nextFist);
        // Inform server to record state and broadcast animation to other players
        CosmicNetwork.CHANNEL.sendToServer(new CosmicNetwork.PunchTriggerPacket(nextFist));
    }

    /**
     * Exact vanilla attack hitbox: targets strictly the entity currently under the player's crosshair in front of them.
     * Enemies behind the player or out of crosshair aim will NEVER be hit.
     */
    @OnlyIn(Dist.CLIENT)
    public static Entity getVanillaCrosshairTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        if (mc.hitResult instanceof EntityHitResult eHit && eHit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }
}
