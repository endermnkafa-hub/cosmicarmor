package com.algeron.cosmicarmor.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/**
 * Concentrated Redstone Laser Orb fired by the Redstone Golem.
 * Flies fast in a straight line with glowing particle spirals.
 */
public class RedstoneLaserOrbEntity extends ThrowableItemProjectile {

    private boolean detonated = false;

    public RedstoneLaserOrbEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public RedstoneLaserOrbEntity(Level level, LivingEntity shooter) {
        super(ModEntities.REDSTONE_LASER_ORB.get(), shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.REDSTONE_BLOCK;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();

        // Parçacık izleri
        if (this.level().isClientSide) {
            Vec3 pos = this.position().add(0, 0.3D, 0);
            com.algeron.cosmicarmor.vfx.RedstoneGolemVFX.spawnLaserOrbTrail(this.level(), pos, this.getDeltaMovement());
            this.level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.8F),
                    pos.x, pos.y, pos.z, 0, 0, 0);
        } else {
            // Genişletilmiş vuruş alanı: Merminin hızlı uçuşunda oyuncuları kaçırmasını engeller
            Vec3 motion = this.getDeltaMovement();
            net.minecraft.world.phys.AABB hitBox = this.getBoundingBox().expandTowards(motion).inflate(0.85D, 0.85D, 0.85D);
            java.util.List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != this.getOwner() && e.isAlive() && !(e instanceof RedstoneGolemEntity));

            if (!victims.isEmpty()) {
                hitVictim(victims.get(0));
                return;
            }

            if (this.tickCount > 100) {
                detonate();
            }
        }
    }

    private void hitVictim(LivingEntity target) {
        if (this.detonated) return;
        this.detonated = true;

        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            // Lazer doğrudan isabet hasarı (15.0F - Full elmas sete ~5-6 net hasar / ~3 kalp)
            if (owner instanceof LivingEntity livingOwner) {
                target.hurt(this.damageSources().mobAttack(livingOwner), 15.0F);
            } else {
                target.hurt(this.damageSources().generic(), 15.0F);
            }

            Vec3 motion = this.getDeltaMovement().normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(motion.x * 0.8D, 0.30D, motion.z * 0.8D));
            target.hurtMarked = true;

            detonateImpact();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            Entity target = result.getEntity();
            if (target instanceof LivingEntity living && target != this.getOwner()) {
                hitVictim(living);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            detonate();
        }
    }

    private void detonateImpact() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.8F, 1.4F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, 1.4F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.3D, this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 2.2F),
                    this.getX(), this.getY() + 0.3D, this.getZ(), 30, 0.6, 0.6, 0.6, 0.1);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3D, this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        }
        this.discard();
    }

    private void detonate() {
        if (this.detonated) return;
        this.detonated = true;

        if (!this.level().isClientSide()) {
            // Blok çarpışmasında alan hasarı
            this.level().explode(
                    this.getOwner(),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    2.0F,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.8F, 1.4F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, 1.2F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.3D, this.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 2.2F),
                        this.getX(), this.getY() + 0.3D, this.getZ(), 30, 0.6, 0.6, 0.6, 0.1);
            }
            this.discard();
        }
    }
}
