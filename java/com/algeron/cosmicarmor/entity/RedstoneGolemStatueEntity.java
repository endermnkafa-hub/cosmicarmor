package com.algeron.cosmicarmor.entity;

import com.algeron.cosmicarmor.worldgen.structure.ArenaManager;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class RedstoneGolemStatueEntity extends Mob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> DATA_AWAKENING =
            SynchedEntityData.defineId(RedstoneGolemStatueEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AWAKENING_TICKS =
            SynchedEntityData.defineId(RedstoneGolemStatueEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RedstoneGolemStatueEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_AWAKENING, false);
        this.entityData.define(DATA_AWAKENING_TICKS, 0);
    }

    public boolean isAwakening() {
        return this.entityData.get(DATA_AWAKENING);
    }

    public void setAwakening(boolean val) {
        this.entityData.set(DATA_AWAKENING, val);
    }

    public int getAwakeningTicks() {
        return this.entityData.get(DATA_AWAKENING_TICKS);
    }

    public void setAwakeningTicks(int ticks) {
        this.entityData.set(DATA_AWAKENING_TICKS, ticks);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Heykel hasar almaz, ama oyuncu vurduğunda uyanış tetiklenebilir
        if (!this.level().isClientSide && !isAwakening()) {
            if (source.getEntity() instanceof Player) {
                startAwakeningSequence();
            }
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !isAwakening()) {
            startAwakeningSequence();
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public void startAwakeningSequence() {
        if (isAwakening()) return;
        setAwakening(true);
        setAwakeningTicks(0);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);

        if (this.level().isClientSide) {
            if (isAwakening()) {
                int ticks = getAwakeningTicks();
                // Titreyen heykel çatlaklarından kırmızı taş tozları ve kırıktaş parçaları çıkar
                if (this.random.nextFloat() < (0.2F + ticks * 0.006F)) {
                    this.level().addParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                            this.getX() + (this.random.nextDouble() - 0.5D) * 2.0D,
                            this.getY() + 0.5D + this.random.nextDouble() * 2.5D,
                            this.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D,
                            0, 0.05D, 0
                    );
                }
                if (ticks > 30 && this.random.nextFloat() < (ticks * 0.008F)) {
                    this.level().addParticle(
                            new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.6F),
                            this.getX() + (this.random.nextDouble() - 0.5D) * 1.8D,
                            this.getY() + 1.0D + this.random.nextDouble() * 2.0D,
                            this.getZ() + (this.random.nextDouble() - 0.5D) * 1.8D,
                            0, 0.08D, 0
                    );
                }
            }
            return;
        }

        // Sunucu Tarafı
        if (!isAwakening()) {
            // Oyuncu iç küreye (16 blok) girdiğinde uyanış otomatik başlar
            AABB triggerBox = this.getBoundingBox().inflate(16.0D, 12.0D, 16.0D);
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, triggerBox,
                    p -> p.isAlive() && !p.isSpectator());

            if (!nearbyPlayers.isEmpty()) {
                startAwakeningSequence();
            }
        } else {
            int ticks = getAwakeningTicks() + 1;
            setAwakeningTicks(ticks);

            // Periyodik derin taş çatlama ve uyanma sesleri
            if (ticks == 20 || ticks == 50 || ticks == 80) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            if (ticks == 70) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.0F, 0.8F);
            }

            // 110 tick (5.5 saniye) sonunda heykel patlar ve asıl boss doğar!
            if (ticks >= 110) {
                shatterAndSpawnBoss();
            }
        }
    }

    private void shatterAndSpawnBoss() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Devasa Kırıktaş Patlama Efekti
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                    this.getX(), this.getY() + 1.5D, this.getZ(),
                    180, 1.4D, 1.5D, 1.4D, 0.25D
            );
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1.5D, this.getZ(),
                    2, 0, 0, 0, 0
            );
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 2.2F),
                    this.getX(), this.getY() + 1.5D, this.getZ(),
                    80, 1.2D, 1.5D, 1.2D, 0.15D
            );

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.7F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 2.5F, 0.6F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);

            // Asıl Boss Spawn Edilir (Asla despawn olmaz!)
            RedstoneGolemEntity boss = new RedstoneGolemEntity(ModEntities.REDSTONE_GOLEM.get(), this.level());
            boss.setPos(this.getX(), this.getY(), this.getZ());
            boss.setYRot(this.getYRot());
            boss.setPersistenceRequired();
            this.level().addFreshEntity(boss);

            // Arena Yöneticisine savaşın başladığını bildir
            ArenaManager.get(serverLevel).onBossFightStarted(boss, this.blockPosition());

            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("redstone_golem_idle_animation_1"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awakening", isAwakening());
        tag.putInt("AwakeningTicks", getAwakeningTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAwakening(tag.getBoolean("Awakening"));
        setAwakeningTicks(tag.getInt("AwakeningTicks"));
    }
}
