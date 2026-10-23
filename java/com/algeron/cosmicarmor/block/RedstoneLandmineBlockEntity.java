package com.algeron.cosmicarmor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class RedstoneLandmineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation DOWN_ANIM = RawAnimation.begin().thenPlay("redstone_golem_landmine_down");

    private boolean isTriggered = false;
    private int triggerTicks = 0;
    private int lifetime = 0;
    private boolean bossSpawned = false;

    public RedstoneLandmineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_LANDMINE.get(), pos, state);
    }

    public void setBossSpawned(boolean bossSpawned) {
        this.bossSpawned = bossSpawned;
        setChanged();
    }

    public void triggerDetonation() {
        if (this.isTriggered) return;
        this.isTriggered = true;
        this.triggerTicks = 0;
        setChanged();

        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.8F);
            this.level.playSound(null, this.worldPosition, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.7F, 1.7F);

            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RedstoneLandmineBlockEntity entity) {
        entity.lifetime++;

        if (level.isClientSide) {
            if (level.random.nextFloat() < 0.25F) {
                level.addParticle(
                        new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.2F),
                        pos.getX() + 0.35 + level.random.nextDouble() * 0.3,
                        pos.getY() + 0.22,
                        pos.getZ() + 0.35 + level.random.nextDouble() * 0.3,
                        0, 0.02, 0
                );
            }
        } else {
            if (entity.isTriggered) {
                entity.triggerTicks++;
                // 0-0.08s is ~2 ticks. At 2 ticks, detonate!
                if (entity.triggerTicks >= 2) {
                    entity.detonate(level, pos);
                }
            } else if (entity.bossSpawned && entity.lifetime >= 100) {
                // Boss-spawned mines detonate after 5 seconds (100 ticks) if not stepped on
                entity.detonate(level, pos);
            }
        }
    }

    private void detonate(Level level, BlockPos pos) {
        level.removeBlock(pos, false);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.2D;
        double z = pos.getZ() + 0.5D;

        // Blokları patlatmadan dengeli güçle (2.2F) patlar (Elmas sete ~3-4 kalp hasar)
        level.explode(null, x, y, z, 2.2F, false, Level.ExplosionInteraction.NONE);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.35F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.1D, z, 2, 0.2, 0.2, 0.2, 0);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 2.0F),
                    x, y + 0.2D, z, 20, 0.5, 0.3, 0.5, 0.08);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animPredicate));
    }

    private PlayState animPredicate(AnimationState<RedstoneLandmineBlockEntity> state) {
        if (this.isTriggered) {
            return state.setAndContinue(DOWN_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Triggered", this.isTriggered);
        tag.putInt("TriggerTicks", this.triggerTicks);
        tag.putInt("Lifetime", this.lifetime);
        tag.putBoolean("BossSpawned", this.bossSpawned);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.isTriggered = tag.getBoolean("Triggered");
        this.triggerTicks = tag.getInt("TriggerTicks");
        this.lifetime = tag.getInt("Lifetime");
        this.bossSpawned = tag.getBoolean("BossSpawned");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
