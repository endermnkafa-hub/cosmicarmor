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
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

public class RedstoneGolemTntEntity extends ThrowableItemProjectile {

    private boolean detonated = false;

    public RedstoneGolemTntEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public RedstoneGolemTntEntity(Level level, LivingEntity shooter) {
        super(ModEntities.REDSTONE_GOLEM_TNT.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.TNT;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.3, this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3, this.getZ(), 0, 0.05, 0);
            this.level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.0F),
                    this.getX(), this.getY() + 0.4, this.getZ(), 0, 0, 0);
        }

        if (!this.level().isClientSide() && this.tickCount > 200) {
            explode();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide()) {
            Entity target = result.getEntity();
            if (target != this.getOwner()) {
                explode();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            explode();
        }
    }

    private void explode() {
        if (this.detonated) return;
        this.detonated = true;

        if (!this.level().isClientSide()) {
            // İlk boss dengesi: Tam elmas sete tek atmayacak dengeli patlama gücü (2.6F)
            this.level().explode(
                    this.getOwner(),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    2.1F,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.9F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.6F),
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        35, 1.0, 1.0, 1.0, 0.1
                );
            }

            this.discard();
        }
    }
}
