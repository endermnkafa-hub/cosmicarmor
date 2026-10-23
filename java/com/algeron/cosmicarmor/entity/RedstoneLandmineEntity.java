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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class RedstoneLandmineEntity extends Entity {
    private int lifetime = 0;
    private UUID ownerUUID;
    private boolean detonated = false;

    public RedstoneLandmineEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public RedstoneLandmineEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner, Vec3 launchVelocity) {
        this(ModEntities.REDSTONE_LANDMINE.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(launchVelocity);
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
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

        // Yerçekimi uygula
        if (!this.onGround()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.95D, motion.y - 0.04D, motion.z * 0.95D);
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide) {
                net.minecraft.core.BlockPos bPos = this.blockPosition();
                if (this.level().isEmptyBlock(bPos) || this.level().getBlockState(bPos).canBeReplaced()) {
                    this.level().setBlockAndUpdate(bPos, com.algeron.cosmicarmor.block.ModBlocks.REDSTONE_LANDMINE.get().defaultBlockState());
                    if (this.level().getBlockEntity(bPos) instanceof com.algeron.cosmicarmor.block.RedstoneLandmineBlockEntity mine) {
                        mine.setBossSpawned(true);
                    }
                    this.discard();
                    return;
                }
            }
        }

        // Parçacık efektleri
        if (this.level().isClientSide) {
            if (this.random.nextFloat() < 0.4F) {
                this.level().addParticle(
                        new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.4F),
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        this.getY() + 0.2D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        0, 0.04D, 0
                );
            }
        }

        if (!this.level().isClientSide) {
            // Tıklama / aktiflik uyarısı sesi (her 15 tickte bir)
            if (this.lifetime % 15 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.HOSTILE, 0.6F, 1.8F + (this.lifetime / 100.0F));
            }

            // Tetiklenme kontrolü (koyulduktan 8 tick sonra aktifleşir)
            if (this.lifetime > 8) {
                AABB triggerBox = new AABB(this.getX() - 1.25D, this.getY() - 0.5D, this.getZ() - 1.25D,
                        this.getX() + 1.25D, this.getY() + 1.0D, this.getZ() + 1.25D);

                List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, triggerBox,
                        e -> e.isAlive() && !(e instanceof RedstoneGolemEntity));

                if (!victims.isEmpty()) {
                    detonate();
                    return;
                }
            }

            // 5 saniye (100 tick) sonra kendiliğinden zincirleme patlar
            if (this.lifetime >= 100) {
                detonate();
            }
        }
    }

    public Entity getOwner() {
        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(this.ownerUUID);
        }
        return null;
    }

    public void detonate() {
        if (this.detonated) return;
        this.detonated = true;

        if (!this.level().isClientSide) {
            Entity owner = getOwner();
            // Blokları kırmayan dengeli patlama (2.2F)
            this.level().explode(
                    owner,
                    this.getX(),
                    this.getY() + 0.15D,
                    this.getZ(),
                    2.2F,
                    false,
                    Level.ExplosionInteraction.NONE
            );

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.35F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2D, this.getZ(), 2, 0.2, 0.2, 0.2, 0);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.8F),
                        this.getX(), this.getY() + 0.25D, this.getZ(), 24, 0.6, 0.4, 0.6, 0.08);
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
