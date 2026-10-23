package com.algeron.cosmicarmor.network;

import com.algeron.cosmicarmor.CosmicArmorMod;
import com.algeron.cosmicarmor.combat.GloveComboManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

public final class CosmicNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CosmicArmorMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(PunchTriggerPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PunchTriggerPacket::encode)
                .decoder(PunchTriggerPacket::decode)
                .consumerMainThread(PunchTriggerPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncAnimationPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAnimationPacket::encode)
                .decoder(SyncAnimationPacket::decode)
                .consumerMainThread(SyncAnimationPacket::handle)
                .add();

        CHANNEL.messageBuilder(HitEffectPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HitEffectPacket::encode)
                .decoder(HitEffectPacket::decode)
                .consumerMainThread(HitEffectPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncWorldCursePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWorldCursePacket::encode)
                .decoder(SyncWorldCursePacket::decode)
                .consumerMainThread(SyncWorldCursePacket::handle)
                .add();
    }

    /**
     * Sent from client to server when player clicks to punch.
     */
    public static class PunchTriggerPacket {
        private final int fistIndex;

        public PunchTriggerPacket(int fistIndex) {
            this.fistIndex = fistIndex;
        }

        public static void encode(PunchTriggerPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.fistIndex);
        }

        public static PunchTriggerPacket decode(FriendlyByteBuf buf) {
            return new PunchTriggerPacket(buf.readInt());
        }

        public static void handle(PunchTriggerPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null && GloveComboManager.hasGloveEquippedAndEmptyHand(player)) {
                    GloveComboManager.recordPunch(player, msg.fistIndex, false);
                    // Broadcast animation to other tracking clients (local player already plays it immediately)
                    CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player),
                            new SyncAnimationPacket(player.getUUID(), msg.fistIndex));
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * Sent from server to client so other players (and self) play the keyframe animation.
     */
    public static class SyncAnimationPacket {
        private final UUID playerUUID;
        private final int fistIndex;

        public SyncAnimationPacket(UUID playerUUID, int fistIndex) {
            this.playerUUID = playerUUID;
            this.fistIndex = fistIndex;
        }

        public static void encode(SyncAnimationPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.playerUUID);
            buf.writeInt(msg.fistIndex);
        }

        public static SyncAnimationPacket decode(FriendlyByteBuf buf) {
            return new SyncAnimationPacket(buf.readUUID(), buf.readInt());
        }

        public static void handle(SyncAnimationPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                // Client side execution
                com.algeron.cosmicarmor.client.GloveAnimationClient.handleSyncPacket(msg.playerUUID, msg.fistIndex);
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * Sent from server to tracking clients to trigger hit visual effects (including Black Flash) and stun.
     */
    public static class HitEffectPacket {
        private final double x;
        private final double y;
        private final double z;
        private final int fistIndex;
        private final boolean isCosmic;
        private final int targetEntityId;

        public HitEffectPacket(double x, double y, double z, int fistIndex, boolean isCosmic, int targetEntityId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.fistIndex = fistIndex;
            this.isCosmic = isCosmic;
            this.targetEntityId = targetEntityId;
        }

        public static void encode(HitEffectPacket msg, FriendlyByteBuf buf) {
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
            buf.writeInt(msg.fistIndex);
            buf.writeBoolean(msg.isCosmic);
            buf.writeInt(msg.targetEntityId);
        }

        public static HitEffectPacket decode(FriendlyByteBuf buf) {
            return new HitEffectPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readBoolean(), buf.readInt());
        }

        public static void handle(HitEffectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                com.algeron.cosmicarmor.vfx.CosmicAuraVFX.handleHitEffectPacket(msg.x, msg.y, msg.z, msg.fistIndex, msg.isCosmic, msg.targetEntityId);
            });
            ctx.setPacketHandled(true);
        }
    }

    /**
     * Dünyanın mevcut lanet kademesini istemciye (client) senkronize eder.
     */
    public static class SyncWorldCursePacket {
        private final int worldCurseLevel;

        public SyncWorldCursePacket(int worldCurseLevel) {
            this.worldCurseLevel = worldCurseLevel;
        }

        public static void encode(SyncWorldCursePacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.worldCurseLevel);
        }

        public static SyncWorldCursePacket decode(FriendlyByteBuf buf) {
            return new SyncWorldCursePacket(buf.readInt());
        }

        public static void handle(SyncWorldCursePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                com.algeron.cosmicarmor.curse.ClientCurseData.clientWorldCurseLevel = msg.worldCurseLevel;
            });
            ctx.setPacketHandled(true);
        }
    }
}
