package com.algeron.cosmicarmor.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.UUID;

public class RedstoneMortarTargetEntity extends Entity {
    private int lifetime = 0;
    private UUID ownerUUID;
    private boolean detonated = false;

    public RedstoneMortarTargetEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public RedstoneMortarTargetEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(ModEntities.REDSTONE_MORTAR_TARGET.get(), level);
        this.setPos(x, y, z);
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    public int getLifetime() {
        return this.lifetime;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        this.lifetime++;

        // İstemci ve sunucuda zemin kırmızı hedef halkaları
        if (this.level().isClientSide) {
            double radius = 2.0D;
            int points = 16;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2 * i) / points;
                double rx = Math.cos(angle) * radius;
                double rz = Math.sin(angle) * radius;

                this.level().addParticle(
                        new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.6F),
                        this.getX() + rx, this.getY() + 0.08D, this.getZ() + rz,
                        0, 0.01D, 0
                );
            }

            // Merkez tehlike parçacığı
            this.level().addParticle(
                    ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.1D, this.getZ(),
                    0, 0.05D, 0
            );

            // Gökten gelen ıslık/yaklaşma efekti
            if (this.lifetime == 18) {
                this.level().playLocalSound(this.getX(), this.getY() + 10.0D, this.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.6F, 0.5F, false);
            }
        }

        // 2.0 saniye (40 tick) sonra gökten inen TNT patlaması
        if (!this.level().isClientSide && this.lifetime >= 40) {
            detonate();
        }
    }

    public Entity getOwner() {
        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(this.ownerUUID);
        }
        return null;
    }

    private void detonate() {
        if (this.detonated) return;
        this.detonated = true;

        if (!this.level().isClientSide) {
            Entity owner = getOwner();
            // Dengeli havan patlaması (2.8F): Blok kırmaz, tek atmaz
            this.level().explode(
                    owner,
                    this.getX(),
                    this.getY() + 0.2D,
                    this.getZ(),
                    2.2F,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            // Düşme hasarını ölümcül olmaktan çıkar: Havaya fırlatma hızını sınırla
            java.util.List<LivingEntity> caught = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0D),
                    e -> e.isAlive() && !(e instanceof RedstoneGolemEntity));
            for (LivingEntity entity : caught) {
                Vec3 curMotion = entity.getDeltaMovement();
                if (curMotion.y > 0.42D) {
                    entity.setDeltaMovement(curMotion.x, 0.42D, curMotion.z);
                    entity.hurtMarked = true;
                }
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.2F, 0.9F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.6, 0.5, 0.6, 0.08);
            }

            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifetime = tag.getInt("Lifetime");
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }
}
