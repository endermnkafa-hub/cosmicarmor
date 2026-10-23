package com.algeron.cosmicarmor.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class LeapingGroundBlockEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_BLOCK_STATE_ID =
            SynchedEntityData.defineId(LeapingGroundBlockEntity.class, EntityDataSerializers.INT);

    private BlockState blockState = Blocks.DIRT.defaultBlockState();
    private BlockPos originalPos = BlockPos.ZERO;
    private double startY;
    private int lifetime = 0;
    private boolean landed = false;

    public LeapingGroundBlockEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public LeapingGroundBlockEntity(Level level, BlockPos pos, BlockState state, double upwardVelocity) {
        this(ModEntities.LEAPING_BLOCK.get(), level);
        this.originalPos = pos.immutable();
        this.blockState = state;
        this.startY = pos.getY();
        this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        this.setDeltaMovement(0.0D, upwardVelocity, 0.0D);
        this.entityData.set(DATA_BLOCK_STATE_ID, Block.getId(state));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK_STATE_ID, Block.getId(Blocks.DIRT.defaultBlockState()));
    }

    public BlockState getBlockState() {
        if (this.level().isClientSide) {
            return Block.stateById(this.entityData.get(DATA_BLOCK_STATE_ID));
        }
        return this.blockState;
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
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, motion.y - 0.048D, 0.0D);
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Parçacık efekti
        if (this.level().isClientSide && this.random.nextBoolean()) {
            this.level().addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()),
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.4D,
                    this.getY() + 0.25D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.4D,
                    0, 0.05D, 0
            );
        }

        // Yere geri iniş kontrolü (başlangıç Y seviyesine düştüğünde veya 26 tick sonra)
        if (!this.level().isClientSide) {
            if ((this.getDeltaMovement().y <= 0 && this.getY() <= this.startY + 0.1D) || this.lifetime >= 26) {
                landAndRestore();
            }
        }
    }

    private void landAndRestore() {
        if (this.landed) return;
        this.landed = true;

        if (!this.level().isClientSide && this.originalPos != null && !this.originalPos.equals(BlockPos.ZERO)) {
            // Bloğu tam olarak orijinal konumuna geri yerleştir
            this.level().setBlock(this.originalPos, this.blockState, 3);

            // Bloğun yere oturma sesi
            this.level().playSound(null, this.originalPos,
                    this.blockState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.3F, 0.9F);

            // İniş toz parçacıkları
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, this.blockState),
                        this.originalPos.getX() + 0.5D, this.originalPos.getY() + 1.05D, this.originalPos.getZ() + 0.5D,
                        14, 0.35D, 0.2D, 0.35D, 0.12D
                );
            }
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        // Güvenlik ağı: Varlık silinirse bloğu kesinlikle geri oluştur, dünya delinmesin
        if (!this.landed && !this.level().isClientSide && this.originalPos != null && !this.originalPos.equals(BlockPos.ZERO)) {
            this.level().setBlock(this.originalPos, this.blockState, 3);
            this.landed = true;
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OriginX")) {
            this.originalPos = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
            this.startY = tag.getDouble("StartY");
        }
        if (tag.contains("BlockStateId")) {
            this.blockState = Block.stateById(tag.getInt("BlockStateId"));
            this.entityData.set(DATA_BLOCK_STATE_ID, tag.getInt("BlockStateId"));
        }
        this.lifetime = tag.getInt("Lifetime");
        this.landed = tag.getBoolean("Landed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.originalPos != null) {
            tag.putInt("OriginX", this.originalPos.getX());
            tag.putInt("OriginY", this.originalPos.getY());
            tag.putInt("OriginZ", this.originalPos.getZ());
            tag.putDouble("StartY", this.startY);
        }
        tag.putInt("BlockStateId", Block.getId(this.blockState));
        tag.putInt("Lifetime", this.lifetime);
        tag.putBoolean("Landed", this.landed);
    }
}
