package com.algeron.cosmicarmor.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class RedstoneGolemEntity extends Monster implements GeoEntity {

    public static final int STATE_IDLE = 0;
    public static final int STATE_ATTACK_RIGHT = 1;
    public static final int STATE_SUPER_HEAVY = 2;
    public static final int STATE_THROW_TNT = 3;
    public static final int STATE_RUSH = 4;
    public static final int STATE_HAND_LASER = 5;
    public static final int STATE_LANDMINES = 6;
    public static final int STATE_MORTAR_BARRAGE = 7;

    private static final EntityDataAccessor<Integer> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(RedstoneGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICK =
            SynchedEntityData.defineId(RedstoneGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_OVERCLOCKED =
            SynchedEntityData.defineId(RedstoneGolemEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_TARGET_YAW =
            SynchedEntityData.defineId(RedstoneGolemEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.cosmicarmor.redstone_golem").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true);

    // Cooldowns
    private int attackCooldown = 20;
    private int actionCooldown = 30;
    private int heavyAttackCooldown = 40;
    private int tntThrowCooldown = 30;
    private int rushCooldown = 60;
    private int laserCooldown = 70;
    private int landminesCooldown = 50;
    private int mortarCooldown = 90;

    private Vec3 rushDirection = Vec3.ZERO;
    private int lastUsedAttackState = STATE_IDLE;

    public RedstoneGolemEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 500;
        this.setMaxUpStep(1.25F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ATTACK_STATE, STATE_IDLE);
        this.entityData.define(DATA_ATTACK_TICK, 0);
        this.entityData.define(DATA_OVERCLOCKED, false);
        this.entityData.define(DATA_TARGET_YAW, 0.0F);
    }

    public int getAttackState() {
        return this.entityData.get(DATA_ATTACK_STATE);
    }

    public void setAttackState(int state) {
        if (state != STATE_IDLE) {
            this.lastUsedAttackState = state;
        }
        this.entityData.set(DATA_ATTACK_STATE, state);
    }

    public int getLastUsedAttackState() {
        return this.lastUsedAttackState;
    }

    public void setLastUsedAttackState(int state) {
        this.lastUsedAttackState = state;
    }

    public int getAttackTick() {
        return this.entityData.get(DATA_ATTACK_TICK);
    }

    public void setAttackTick(int tick) {
        this.entityData.set(DATA_ATTACK_TICK, tick);
    }

    public boolean isOverclocked() {
        return this.entityData.get(DATA_OVERCLOCKED);
    }

    public void setOverclocked(boolean val) {
        this.entityData.set(DATA_OVERCLOCKED, val);
    }

    public int getActionCooldown() {
        return this.actionCooldown;
    }

    public void setActionCooldown(int cd) {
        this.actionCooldown = cd;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RedstoneGolemCombatGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            // Pasif kırmızı taş ve duman parçacıkları
            if (this.random.nextFloat() < (isOverclocked() ? 0.75F : 0.35F)) {
                this.level().addParticle(
                        new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.2F),
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.8D,
                        this.getY() + 1.2D + (this.random.nextDouble() - 0.5D) * 1.2D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.8D,
                        0.0D, 0.05D, 0.0D
                );
            }
            if (isOverclocked() && this.random.nextFloat() < 0.45F) {
                this.level().addParticle(
                        ParticleTypes.FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        this.getY() + 2.0D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        0.0D, 0.04D, 0.0D
                );
            }

            // İstemcide saldırı ve kilitlenme anında gövde açısını eşitle
            if (getAttackState() != STATE_IDLE) {
                float syncedYaw = this.entityData.get(DATA_TARGET_YAW);
                this.setYRot(syncedYaw);
                this.setYHeadRot(syncedYaw);
                this.setYBodyRot(syncedYaw);
                this.yRotO = syncedYaw;
                this.yHeadRotO = syncedYaw;
                this.yBodyRotO = syncedYaw;

                // El lazeri içe çekilen kızıltaş enerji parçacıkları (Lodestone)
                if (getAttackState() == STATE_HAND_LASER) {
                    int cTick = getAttackTick();
                    Vec3 forward = this.getLookAngle();
                    Vec3 handPos = this.position().add(forward.scale(1.5D)).add(0, 2.0D, 0);
                    if (cTick >= 10 && cTick < 25) {
                        com.algeron.cosmicarmor.vfx.RedstoneGolemVFX.spawnLaserCharge(this.level(), handPos, cTick);
                    } else if (cTick == 25) {
                        com.algeron.cosmicarmor.vfx.RedstoneGolemVFX.spawnLaserFire(this.level(), handPos, forward);
                    }
                }
            }
            return;
        }

        // Sunucu tarafı bekleme süreleri (Overclock modunda 2 kat hızlı geri sayar)
        if (this.actionCooldown > 0) this.actionCooldown--;
        int cdDec = isOverclocked() ? 2 : 1;
        if (this.heavyAttackCooldown > 0) this.heavyAttackCooldown -= cdDec;
        if (this.tntThrowCooldown > 0) this.tntThrowCooldown -= cdDec;
        if (this.attackCooldown > 0) this.attackCooldown -= cdDec;
        if (this.rushCooldown > 0) this.rushCooldown -= cdDec;
        if (this.laserCooldown > 0) this.laserCooldown -= cdDec;
        if (this.landminesCooldown > 0) this.landminesCooldown -= cdDec;
        if (this.mortarCooldown > 0) this.mortarCooldown -= cdDec;

        int state = getAttackState();
        if (state != STATE_IDLE) {
            int currentTick = getAttackTick() + 1;
            setAttackTick(currentTick);

            // Saldırı anında hedefe dönme ve durma kontrolleri
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                boolean canTurn = switch (state) {
                    case STATE_THROW_TNT -> currentTick <= 14;
                    case STATE_ATTACK_RIGHT -> currentTick <= 18;
                    case STATE_SUPER_HEAVY -> currentTick <= 25; // 0-25 tick döner, 25-31 yön kilitlenir (kaçma payı!)
                    case STATE_RUSH -> currentTick <= 15; // 0-15 tick hedefe kitlenir, 15-20 pozisyonda kalır
                    case STATE_HAND_LASER -> currentTick <= 20; // 0-20 tick hedefe kitlenir, 20-25 ateşleme hattı kilitlenir
                    case STATE_LANDMINES -> currentTick <= 14;
                    case STATE_MORTAR_BARRAGE -> currentTick <= 14;
                    default -> false;
                };

                if (canTurn) {
                    faceTarget(target, 45.0F);
                }
            }
            this.getNavigation().stop();

            switch (state) {
                case STATE_ATTACK_RIGHT:
                    // 0-0.92s (0-18 tick) hazırlanış, 19. tick (0.92s-1.17s) vuruş, 35. tick bitiş
                    if (currentTick == 19) {
                        executeRightAttack();
                    }
                    if (currentTick >= 35) {
                        resetAttack();
                    }
                    break;

                case STATE_SUPER_HEAVY:
                    // 0-0.65s kollar kalkar, 13-27 tick kollar havada bekler (kızıltaş enerjisi toplar), 31. tick yere vuruş, 44. tick bitiş
                    if (currentTick < 31) {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.2F), 1.6F),
                                    this.getX() + (this.random.nextDouble() - 0.5D) * 2.4D,
                                    this.getY() + 3.0D + (this.random.nextDouble() - 0.5D) * 0.8D,
                                    this.getZ() + (this.random.nextDouble() - 0.5D) * 2.4D,
                                    5, 0.2, 0.2, 0.2, 0.05
                            );
                        }
                    } else if (currentTick == 31) {
                        executeSuperHeavyAttack();
                    }
                    if (currentTick >= 44) {
                        resetAttack();
                    }
                    break;

                case STATE_THROW_TNT:
                    // 0-0.63s geriliş, 17. tick fırlatış, 35. tick bitiş
                    if (currentTick == 17) {
                        executeThrowTnt();
                    }
                    if (currentTick >= 35) {
                        resetAttack();
                    }
                    break;

                case STATE_RUSH:
                    // 0-0.75s (0-15 tick) oyuncuya kilitleniş
                    // 15-20 tick kilitlendiği pozisyonda kalma (dumanla hücum sinyali verir)
                    if (currentTick == 15) {
                        this.rushDirection = this.getLookAngle().normalize();
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.8F, 0.6F);
                    }
                    if (currentTick >= 15 && currentTick < 20) {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                    this.getX(), this.getY() + 0.2D, this.getZ(), 4, 0.5, 0.1, 0.5, 0.02);
                        }
                    }
                    // 20-36 tick atılma (16 tick boyunca 1.40D hızla ~22 blok kateder - oyuncuya kesinlikle ulaşır!)
                    if (currentTick >= 20 && currentTick <= 36) {
                        if (currentTick == 20) {
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                    SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 2.5F, 0.8F);
                        }
                        this.setDeltaMovement(this.rushDirection.x * 1.40D, this.getDeltaMovement().y, this.rushDirection.z * 1.40D);

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                                    this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0.2, 0.2, 0.2, 0);
                            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.8F),
                                    this.getX(), this.getY() + 0.5D, this.getZ(), 8, 0.4, 0.3, 0.4, 0.05);
                        }

                        // Toslama Hasarı (20.0F - Full elmas sete ~8 hasar / 4 kalp)
                        AABB rushBox = this.getBoundingBox().inflate(1.1D, 0.3D, 1.1D);
                        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, rushBox,
                                e -> e != this && e.isAlive() && !e.isAlliedTo(this));
                        for (LivingEntity victim : victims) {
                            victim.hurt(this.damageSources().mobAttack(this), 15.0F);
                            victim.setDeltaMovement(this.rushDirection.scale(1.15D).add(0, 0.45D, 0));
                            victim.hurtMarked = true;
                        }
                    }
                    // Toparlanma
                    if (currentTick >= 48) {
                        resetAttack();
                    }
                    break;

                case STATE_HAND_LASER:
                    // 0-0.5 (0-10 tick) elleri birleştirme
                    // 0.5-1.25 (10-25 tick) hedefe kilitleniş (20. tickte yön kilitlenir, parçacıklar Lodestone ile istemcide çekilir)
                    if (currentTick == 10) {
                        this.level().playSound(null, this.getX(), this.getY() + 2.0D, this.getZ(),
                                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.0F, 1.2F);
                    }
                    // 1.25 (25. tick) lazeri / enerji küresini fırlatma
                    if (currentTick == 25) {
                        executeHandLaser();
                    }
                    // 1.54-2.25 toparlanış
                    if (currentTick >= 45) {
                        resetAttack();
                    }
                    break;

                case STATE_LANDMINES:
                    // 0-0.5 ayağı geriş, 0.5-0.83 bekleme, 17. tick (0.83-1.13) tekme vurma
                    if (currentTick == 17) {
                        executeLandmines();
                    }
                    // 1.13-1.88 (23-38 tick) toparlanış
                    if (currentTick >= 38) {
                        resetAttack();
                    }
                    break;

                case STATE_MORTAR_BARRAGE:
                    // 0-0.5 geriliş, 0.5-1.0 bekleme, 20. tick (1.0-1.29) fırlatma
                    if (currentTick == 20) {
                        executeMortarBarrage();
                    }
                    // 1.29-2.0 (26-40 tick) toparlanış
                    if (currentTick >= 40) {
                        resetAttack();
                    }
                    break;
            }
        }
    }

    private void executeRightAttack() {
        Vec3 forward = this.getLookAngle();
        Vec3 strikePos = this.position().add(forward.scale(2.0D)).add(0, 1.4D, 0);

        this.level().playSound(null, strikePos.x, strikePos.y, strikePos.z,
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 2.0F, 0.7F);
        this.level().playSound(null, strikePos.x, strikePos.y, strikePos.z,
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.8F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, strikePos.x, strikePos.y, strikePos.z, 2, 0.3, 0.2, 0.3, 0.0);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.6F),
                    strikePos.x, strikePos.y, strikePos.z, 25, 0.8, 0.6, 0.8, 0.05);
        }

        AABB attackBox = new AABB(strikePos.x - 2.4, strikePos.y - 1.5, strikePos.z - 2.4,
                strikePos.x + 2.4, strikePos.y + 1.5, strikePos.z + 2.4);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, attackBox,
                e -> e != this && e.isAlive() && !e.isAlliedTo(this));

        for (LivingEntity target : targets) {
            target.hurt(this.damageSources().mobAttack(this), 12.0F);
            target.setDeltaMovement(target.getDeltaMovement().add(forward.x * 0.75D, 0.40D, forward.z * 0.75D));
            target.hurtMarked = true;
        }
    }

    private void executeSuperHeavyAttack() {
        Vec3 forward = this.getLookAngle();
        Vec3 slamPos = this.position().add(forward.scale(1.8D));

        this.level().playSound(null, slamPos.x, slamPos.y, slamPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 0.65F);
        this.level().playSound(null, slamPos.x, slamPos.y, slamPos.z,
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.6F);

        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 36; i++) {
                double angle = Math.toRadians(i * 10);
                double rx = Math.cos(angle);
                double rz = Math.sin(angle);
                for (double dist = 1.5; dist <= 6.5; dist += 1.5) {
                    serverLevel.sendParticles(
                            ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            slamPos.x + rx * dist, slamPos.y + 0.1, slamPos.z + rz * dist,
                            1, 0, 0.08, 0, 0.02
                    );
                    serverLevel.sendParticles(
                            new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.15F), 1.8F),
                            slamPos.x + rx * dist, slamPos.y + 0.2, slamPos.z + rz * dist,
                            1, 0, 0.05, 0, 0.02
                    );
                }
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, slamPos.x, slamPos.y + 0.5, slamPos.z, 5, 1.2, 0.5, 1.2, 0.1);

            // Zemin bloklarını havaya fırlatma
            BlockPos centerPos = BlockPos.containing(slamPos);
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq >= 2.0D && distSq <= 22.0D) {
                        if (this.random.nextFloat() < 0.65F) {
                            BlockPos ground = null;
                            for (int dy = 1; dy >= -2; dy--) {
                                BlockPos check = centerPos.offset(dx, dy, dz);
                                BlockState state = this.level().getBlockState(check);
                                if (!state.isAir() && state.isSolid() && !state.hasBlockEntity() && state.getDestroySpeed(this.level(), check) >= 0) {
                                    if (this.level().isEmptyBlock(check.above()) || !this.level().getBlockState(check.above()).isSolid()) {
                                        ground = check;
                                        break;
                                    }
                                }
                            }

                            if (ground != null) {
                                BlockState state = this.level().getBlockState(ground);
                                double dist = Math.sqrt(distSq);
                                double upwardVel = 0.36D + (5.0D - dist) * 0.08D + this.random.nextDouble() * 0.12D;

                                this.level().setBlock(ground, Blocks.AIR.defaultBlockState(), 3);
                                LeapingGroundBlockEntity leapingBlock = new LeapingGroundBlockEntity(this.level(), ground, state, upwardVel);
                                this.level().addFreshEntity(leapingBlock);
                            }
                        }
                    }
                }
            }
        }

        AABB shockwaveAABB = new AABB(slamPos.x - 6.5, slamPos.y - 2.0, slamPos.z - 6.5,
                slamPos.x + 6.5, slamPos.y + 3.0, slamPos.z + 6.5);

        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, shockwaveAABB,
                e -> e != this && e.isAlive() && !e.isAlliedTo(this));

        for (LivingEntity victim : victims) {
            double dist = victim.position().distanceTo(slamPos);
            if (dist <= 3.5D) {
                // Doğrudan isabet: 18.0F
                victim.hurt(this.damageSources().mobAttack(this), 18.0F);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.65D, 0));
            } else {
                // Dış şok dalgası: 9.0F
                victim.hurt(this.damageSources().mobAttack(this), 9.0F);
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.40D, 0));
            }
            victim.hurtMarked = true;
        }
    }

    public void faceTarget(LivingEntity target, float maxTurn) {
        if (target == null || !target.isAlive()) return;
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = target.getEyeY() - this.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horiz) * (180.0D / Math.PI)));

        float newYaw = Mth.approachDegrees(this.getYRot(), targetYaw, maxTurn);
        float newPitch = Mth.approachDegrees(this.getXRot(), targetPitch, maxTurn);

        this.setYRot(newYaw);
        this.setYHeadRot(newYaw);
        this.setYBodyRot(newYaw);
        this.setXRot(newPitch);

        this.yRotO = newYaw;
        this.yHeadRotO = newYaw;
        this.yBodyRotO = newYaw;
        this.xRotO = newPitch;

        this.entityData.set(DATA_TARGET_YAW, newYaw);
    }

    private void executeThrowTnt() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        faceTarget(target, 90.0F);

        Vec3 forward = this.getLookAngle();
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 handPos = this.position().add(0, 2.4D, 0).subtract(right.scale(1.4D)).add(forward.scale(1.2D));

        RedstoneGolemTntEntity tnt = new RedstoneGolemTntEntity(this.level(), this);
        tnt.setPos(handPos.x, handPos.y, handPos.z);

        double dx = target.getX() - handPos.x;
        double dy = target.getY(0.5D) - handPos.y;
        double dz = target.getZ() - handPos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        double arc = Math.min(horizDist * 0.12D, 3.5D);
        tnt.shoot(dx, dy + arc, dz, 1.15F, 0.6F);

        this.level().addFreshEntity(tnt);
        this.level().playSound(null, handPos.x, handPos.y, handPos.z,
                SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.8F, 1.0F);
        this.level().playSound(null, handPos.x, handPos.y, handPos.z,
                SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.8F, 0.7F);
    }

    private void executeHandLaser() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        faceTarget(target, 90.0F);

        Vec3 forward = this.getLookAngle();
        Vec3 handPos = this.position().add(forward.scale(1.6D)).add(0, 2.0D, 0);

        RedstoneLaserOrbEntity orb = new RedstoneLaserOrbEntity(this.level(), this);
        orb.setPos(handPos.x, handPos.y, handPos.z);

        double dx = target.getX() - handPos.x;
        double dy = (target.getY() + target.getEyeHeight() * 0.5D) - handPos.y;
        double dz = target.getZ() - handPos.z;

        orb.shoot(dx, dy, dz, 1.45F, 0.3F);
        this.level().addFreshEntity(orb);

        this.level().playSound(null, handPos.x, handPos.y, handPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 1.4F);
        this.level().playSound(null, handPos.x, handPos.y, handPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.5F);
    }

    private void executeLandmines() {
        Vec3 pos = this.position();
        this.level().playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.65F);
        this.level().playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, 1.2F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 2.2F),
                    pos.x, pos.y + 0.1D, pos.z, 40, 1.5, 0.2, 1.5, 0.1);
        }

        // Orta mesafeye (4.5 - 9.5 blok) rastgele açılarda 4-5 mayın saçar (Asla dibine veya hep aynı yere koymaz!)
        int count = 4 + this.random.nextInt(2);
        double baseAngle = this.random.nextDouble() * 360.0D;
        for (int i = 0; i < count; i++) {
            double angleDeg = baseAngle + (i * (360.0D / count)) + (this.random.nextDouble() - 0.5D) * 30.0D;
            double rad = Math.toRadians(angleDeg);
            double dist = 4.5D + this.random.nextDouble() * 5.0D;
            int targetX = Mth.floor(pos.x + Math.cos(rad) * dist);
            int targetZ = Mth.floor(pos.z + Math.sin(rad) * dist);
            int centerY = Mth.floor(pos.y);

            BlockPos placePos = null;
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos groundCandidate = new BlockPos(targetX, centerY + dy, targetZ);
                BlockPos aboveCandidate = groundCandidate.above();
                if (this.level().getBlockState(groundCandidate).isSolidRender(this.level(), groundCandidate)
                        && (this.level().isEmptyBlock(aboveCandidate) || this.level().getBlockState(aboveCandidate).canBeReplaced())) {
                    placePos = aboveCandidate;
                    break;
                }
            }

            if (placePos != null && !this.level().isClientSide) {
                this.level().setBlockAndUpdate(placePos, com.algeron.cosmicarmor.block.ModBlocks.REDSTONE_LANDMINE.get().defaultBlockState());
                if (this.level().getBlockEntity(placePos) instanceof com.algeron.cosmicarmor.block.RedstoneLandmineBlockEntity mine) {
                    mine.setBossSpawned(true);
                }
            } else {
                double speedFactor = 0.25D + dist * 0.035D;
                double vx = Math.cos(rad) * speedFactor;
                double vz = Math.sin(rad) * speedFactor;
                double vy = 0.32D + this.random.nextDouble() * 0.12D;

                RedstoneLandmineEntity mine = new RedstoneLandmineEntity(
                        this.level(),
                        pos.x + Math.cos(rad) * 1.5D,
                        pos.y + 0.6D,
                        pos.z + Math.sin(rad) * 1.5D,
                        this,
                        new Vec3(vx, vy, vz)
                );
                this.level().addFreshEntity(mine);
            }
        }
    }

    private void executeMortarBarrage() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        Vec3 pos = this.position();
        this.level().playSound(null, pos.x, pos.y + 2.5D, pos.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 2.5F, 0.6F);
        this.level().playSound(null, pos.x, pos.y + 2.5D, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.4F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 3.2D, pos.z, 20, 0.4, 0.8, 0.4, 0.15);
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 3.2D, pos.z, 25, 0.5, 0.6, 0.5, 0.1);
        }

        Vec3 tPos = target.position();
        Vec3 tMotion = target.getDeltaMovement();
        Vec3[] targets = new Vec3[]{
                tPos,
                tPos.add(tMotion.x * 12.0D, 0, tMotion.z * 12.0D).add(this.random.nextDouble() - 0.5D, 0, this.random.nextDouble() - 0.5D),
                tPos.add((this.random.nextDouble() - 0.5D) * 4.5D, 0, (this.random.nextDouble() - 0.5D) * 4.5D)
        };

        for (Vec3 t : targets) {
            RedstoneMortarTargetEntity marker = new RedstoneMortarTargetEntity(this.level(), t.x, t.y, t.z, this);
            this.level().addFreshEntity(marker);
        }
    }

    private void resetAttack() {
        setAttackState(STATE_IDLE);
        setAttackTick(0);
        this.attackCooldown = isOverclocked() ? 6 : 10;
        this.actionCooldown = isOverclocked() ? 6 + this.random.nextInt(6) : 10 + this.random.nextInt(8);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        // Overclock kontrolü (Can %45 veya altı)
        if (!isOverclocked() && this.getHealth() <= this.getMaxHealth() * 0.45F) {
            triggerOverclock();
        }
    }

    private void triggerOverclock() {
        setOverclocked(true);
        this.bossEvent.setName(
                Component.translatable("entity.cosmicarmor.redstone_golem")
                        .append(Component.literal(" [AŞIRI YÜKLEME]").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.5F, 0.7F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5F, 0.9F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 1.8D, this.getZ(), 35, 1.0, 1.0, 1.0, 0.2);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.5D, this.getZ(), 25, 0.8, 1.0, 0.8, 0.1);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.8D, this.getZ(), 40, 1.0, 1.0, 1.0, 0.15);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            com.algeron.cosmicarmor.curse.WorldCurseData curseData = com.algeron.cosmicarmor.curse.WorldCurseData.get(serverLevel);
            if (curseData.getCurseLevel() < 1) {
                curseData.setCurseLevel(1);

                // Tüm oyunculara dünya lanet seviyesini senkronize et
                com.algeron.cosmicarmor.network.CosmicNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                        new com.algeron.cosmicarmor.network.CosmicNetwork.SyncWorldCursePacket(1)
                );

                Component curseMsg = Component.literal("§c§l[DÜNYA LANETİ - 1. KADEME] §r§4Kızıl Makinenin Çekirdeği Dağıldı... Kızıl Rezonans yeryüzüne ve tüm canavarlara yayılıyor!");
                for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(curseMsg);
                    player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 1.0F, 0.65F);
                    player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 1.0F, 0.8F);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Lanet materyali ödülleri (Boss zaferi - dengelenmiş)
        this.spawnAtLocation(new ItemStack(com.algeron.cosmicarmor.CosmicArmorMod.REDSTONE_CURSE_CRYSTAL.get(), 1 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(com.algeron.cosmicarmor.CosmicArmorMod.REDSTONE_CURSE_SHARD.get(), 2 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(com.algeron.cosmicarmor.CosmicArmorMod.REDSTONE_CURSE_DUST.get(), 4 + this.random.nextInt(5)));
        this.spawnAtLocation(new ItemStack(com.algeron.cosmicarmor.CosmicArmorMod.NEUTRAL_CURSE_CATALYST.get(), 1));

        this.spawnAtLocation(new ItemStack(Items.REDSTONE_BLOCK, 4 + this.random.nextInt(3)));
        this.spawnAtLocation(new ItemStack(Items.IRON_BLOCK, 2 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(Items.TNT, 3 + this.random.nextInt(3)));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Boss asla kendi saldırılarından (TNT, Lazer, Mayın, Havan vb.) hasar almaz!
        if (source.getEntity() == this || source.getDirectEntity() == this) {
            return false;
        }

        // Kendi mermilerinden ve varlıklarından doğrudan hasar almaz
        Entity direct = source.getDirectEntity();
        if (direct instanceof RedstoneGolemTntEntity
                || direct instanceof RedstoneLaserOrbEntity
                || direct instanceof RedstoneLandmineEntity
                || direct instanceof RedstoneMortarTargetEntity) {
            return false;
        }

        // Kendi ve sahipsiz mod patlamalarından (mayın, havan, lazer, tnt) tam bağışıklık
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            Entity attacker = source.getEntity();
            if (attacker == null || attacker == this || attacker instanceof RedstoneGolemEntity) {
                return false;
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.2F, 0.75F);
    }

    public int getHeavyAttackCooldown() {
        return heavyAttackCooldown;
    }

    public void setHeavyAttackCooldown(int cd) {
        this.heavyAttackCooldown = cd;
    }

    public int getTntThrowCooldown() {
        return tntThrowCooldown;
    }

    public void setTntThrowCooldown(int cd) {
        this.tntThrowCooldown = cd;
    }

    public int getMeleeCooldown() {
        return attackCooldown;
    }

    public void setMeleeCooldown(int cd) {
        this.attackCooldown = cd;
    }

    public int getRushCooldown() {
        return rushCooldown;
    }

    public void setRushCooldown(int cd) {
        this.rushCooldown = cd;
    }

    public int getLaserCooldown() {
        return laserCooldown;
    }

    public void setLaserCooldown(int cd) {
        this.laserCooldown = cd;
    }

    public int getLandminesCooldown() {
        return landminesCooldown;
    }

    public void setLandminesCooldown(int cd) {
        this.landminesCooldown = cd;
    }

    public int getMortarCooldown() {
        return mortarCooldown;
    }

    public void setMortarCooldown(int cd) {
        this.mortarCooldown = cd;
    }

    private int prevClientAttackState = STATE_IDLE;

    // --- GECKOLIB ENTEGRASYONU ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
        controllers.add(new AnimationController<>(this, "attack", 1, this::attackPredicate));
    }

    private PlayState movementPredicate(AnimationState<RedstoneGolemEntity> state) {
        if (this.getAttackState() != STATE_IDLE) {
            return PlayState.STOP;
        }
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("redstone_golem_walk_animation_1"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("redstone_golem_idle_animation_1"));
    }

    private PlayState attackPredicate(AnimationState<RedstoneGolemEntity> state) {
        int attack = this.getAttackState();

        if (attack != this.prevClientAttackState) {
            this.prevClientAttackState = attack;
            state.getController().forceAnimationReset();
        }

        if (attack == STATE_ATTACK_RIGHT) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_attack_right"));
        } else if (attack == STATE_SUPER_HEAVY) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_super_heavy_attack"));
        } else if (attack == STATE_THROW_TNT) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_throwing_tnt"));
        } else if (attack == STATE_RUSH) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_rush"));
        } else if (attack == STATE_HAND_LASER) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_hand_laser"));
        } else if (attack == STATE_LANDMINES) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_redstone_landmines"));
        } else if (attack == STATE_MORTAR_BARRAGE) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("redstone_golem_mortar_barrage"));
        }

        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- YAPAY ZEKA SALDIRI HEDEFİ VE SEÇİMİ ---
    private static class RedstoneGolemCombatGoal extends Goal {
        private final RedstoneGolemEntity golem;

        public RedstoneGolemCombatGoal(RedstoneGolemEntity golem) {
            this.golem = golem;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();
            if (target == null || !target.isAlive()) return;

            if (this.golem.getAttackState() != STATE_IDLE) {
                this.golem.getNavigation().stop();
                return;
            }

            double speed = this.golem.isOverclocked() ? 1.35D : 1.15D;
            this.golem.faceTarget(target, 20.0F);
            this.golem.getNavigation().moveTo(target, speed);

            double dist = this.golem.distanceTo(target);
            int last = this.golem.getLastUsedAttackState();

            // =========================================================================
            // 1. YAKIN MESAFE (dist <= 3.8 blok):
            // - attack right: Yakın mesafenin birincil temel saldırısı (seri yumruklar)
            // - heavy attack: Yakında TERCİH EDİLİR (Zemin şoku + dev hasar, az can bırakır)
            // - landmines / throwing tnt / rush / hand laser / mortar: KESİNLİKLE YASAK!
            // =========================================================================
            if (dist <= 3.8D) {
                boolean canHeavy = this.golem.getHeavyAttackCooldown() <= 0 && last != STATE_SUPER_HEAVY;
                boolean canPunch = this.golem.getMeleeCooldown() <= 0 && last != STATE_ATTACK_RIGHT;

                // Ağır vuruş hazır ve son kullanılan değilse öncelikle tercih et
                if (canHeavy && (last == STATE_ATTACK_RIGHT || this.golem.getRandom().nextFloat() < 0.50F)) {
                    this.golem.faceTarget(target, 80.0F);
                    this.golem.setAttackState(STATE_SUPER_HEAVY);
                    this.golem.setAttackTick(0);
                    this.golem.setHeavyAttackCooldown(55 + this.golem.getRandom().nextInt(20));
                    return;
                }

                if (canPunch) {
                    this.golem.faceTarget(target, 80.0F);
                    this.golem.setAttackState(STATE_ATTACK_RIGHT);
                    this.golem.setAttackTick(0);
                    this.golem.setMeleeCooldown(16 + this.golem.getRandom().nextInt(8));
                    return;
                }

                // Bekleme süresi bittiyse temel yumruğa geri dön
                if (this.golem.getMeleeCooldown() <= 0) {
                    this.golem.faceTarget(target, 80.0F);
                    this.golem.setAttackState(STATE_ATTACK_RIGHT);
                    this.golem.setAttackTick(0);
                    this.golem.setMeleeCooldown(16 + this.golem.getRandom().nextInt(8));
                    return;
                }
                return;
            }

            // Beceri sonrası ufak nefes alma payı (orta ve uzak özel yetenekleri sınırlar)
            if (this.golem.getActionCooldown() > 0) {
                return;
            }

            // =========================================================================
            // 2. ORTA MESAFE (3.8 < dist <= 11.0 blok):
            // - rush: (5.5 - 11.0 blok) mesafeyi kapatıp toslamak için YÜKSEK ÖNCELİK
            // - landmines: (4.5 - 10.5 blok) tekme atıp 4-5 mayın saçar (yakına asla koymaz)
            // - mortar barrage: (5.0 - 11.0 blok) gökten TNT yağdırır
            // - hand laser: (6.0 - 11.0 blok) lazer küresi atar
            // - heavy attack: (3.8 - 6.5 blok) şok dalgasıyla vurur
            // - throwing tnt: YASAK! (Sadece uzak mesafede kullanılır)
            // =========================================================================
            if (dist > 3.8D && dist <= 11.0D) {
                java.util.List<Integer> candidates = new java.util.ArrayList<>();

                if (dist >= 5.5D && this.golem.getRushCooldown() <= 0 && last != STATE_RUSH) {
                    candidates.add(STATE_RUSH);
                    candidates.add(STATE_RUSH); // Hücumun orta mesafede seçilme şansını artırıyoruz
                }
                if (dist >= 4.5D && dist <= 10.5D && this.golem.getLandminesCooldown() <= 0 && last != STATE_LANDMINES) {
                    candidates.add(STATE_LANDMINES);
                }
                if (dist >= 5.0D && this.golem.getMortarCooldown() <= 0 && last != STATE_MORTAR_BARRAGE) {
                    candidates.add(STATE_MORTAR_BARRAGE);
                }
                if (dist >= 6.0D && this.golem.getLaserCooldown() <= 0 && last != STATE_HAND_LASER) {
                    candidates.add(STATE_HAND_LASER);
                }
                if (dist <= 6.5D && this.golem.getHeavyAttackCooldown() <= 0 && last != STATE_SUPER_HEAVY) {
                    candidates.add(STATE_SUPER_HEAVY);
                }

                if (!candidates.isEmpty()) {
                    int chosen = candidates.get(this.golem.getRandom().nextInt(candidates.size()));
                    this.golem.faceTarget(target, 80.0F);
                    this.golem.setAttackState(chosen);
                    this.golem.setAttackTick(0);

                    switch (chosen) {
                        case STATE_RUSH -> this.golem.setRushCooldown(50 + this.golem.getRandom().nextInt(25));
                        case STATE_LANDMINES -> this.golem.setLandminesCooldown(60 + this.golem.getRandom().nextInt(25));
                        case STATE_MORTAR_BARRAGE -> this.golem.setMortarCooldown(65 + this.golem.getRandom().nextInt(25));
                        case STATE_HAND_LASER -> this.golem.setLaserCooldown(50 + this.golem.getRandom().nextInt(25));
                        case STATE_SUPER_HEAVY -> this.golem.setHeavyAttackCooldown(60 + this.golem.getRandom().nextInt(25));
                    }
                    return;
                }
            }

            // =========================================================================
            // 3. UZAK MESAFE (dist > 11.0 blok up to 32.0 blok):
            // - rush: (11.0 - 24.0 blok) uzaktan hedefe doğru fırtına gibi atılır!
            // - hand laser: (11.0 - 28.0 blok) lazer küresi fırlatır!
            // - throwing tnt: (11.0 - 32.0 blok) TNT fırlatır (uzun bekleme süreli, tek atmaz)
            // - attack right / heavy attack / landmines / mortar: YASAK!
            // =========================================================================
            if (dist > 11.0D && dist <= 32.0D) {
                java.util.List<Integer> candidates = new java.util.ArrayList<>();

                if (dist <= 24.0D && this.golem.getRushCooldown() <= 0 && last != STATE_RUSH) {
                    candidates.add(STATE_RUSH);
                    candidates.add(STATE_RUSH); // Uzaktan hücum etme isteğini artır
                }
                if (dist <= 28.0D && this.golem.getLaserCooldown() <= 0 && last != STATE_HAND_LASER) {
                    candidates.add(STATE_HAND_LASER);
                }
                if (this.golem.getTntThrowCooldown() <= 0 && last != STATE_THROW_TNT) {
                    candidates.add(STATE_THROW_TNT);
                }

                if (!candidates.isEmpty()) {
                    int chosen = candidates.get(this.golem.getRandom().nextInt(candidates.size()));
                    this.golem.faceTarget(target, 80.0F);
                    this.golem.setAttackState(chosen);
                    this.golem.setAttackTick(0);

                    switch (chosen) {
                        case STATE_RUSH -> this.golem.setRushCooldown(50 + this.golem.getRandom().nextInt(25));
                        case STATE_HAND_LASER -> this.golem.setLaserCooldown(50 + this.golem.getRandom().nextInt(25));
                        case STATE_THROW_TNT -> this.golem.setTntThrowCooldown(80 + this.golem.getRandom().nextInt(30)); // 4 - 5.5s bekleme süresi
                    }
                    return;
                }
            }
        }
    }
}
