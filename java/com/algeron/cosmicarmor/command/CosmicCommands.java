package com.algeron.cosmicarmor.command;

import com.algeron.cosmicarmor.curse.WorldCurseData;
import com.algeron.cosmicarmor.worldgen.structure.ArenaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "cosmicarmor", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CosmicCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("cosmic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn_arena")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            // Arenanın merkezini oyuncunun tam ayağının bastığı zemin hizasına ayarla
                            BlockPos origin = player.blockPosition().offset(-55, -2, -56);

                            player.sendSystemMessage(Component.literal("§c[CosmicArmor] §7Arena inşa ediliyor, lütfen birkaç saniye bekleyin..."));
                            BlockPos statuePos = ArenaManager.get(level).initializeArenaAt(level, origin);

                            // Oyuncuyu heykelin 8 blok önüne, heykelle göz göze gelecek şekilde ışınla
                            if (statuePos != null) {
                                player.teleportTo(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                            }

                            player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eKızıltaş Arenası kuruldu! Kırıktaş Heykel tam karşında duruyor."));
                            return 1;
                        }))
                .then(Commands.literal("tp_arena")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            ArenaManager manager = ArenaManager.get(level);

                            if (manager.hasArena() && !manager.getArenaCenter().equals(BlockPos.ZERO)) {
                                BlockPos center = manager.getArenaCenter();
                                player.teleportTo(center.getX() + 0.5D, center.getY(), center.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                                player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eMevcut Kızıltaş Arenasına ışınlandın!"));
                                return 1;
                            }

                            // Eğer henüz dünyada kaydedilmiş bir arena yoksa, derin yeraltında (Y: -35) anında inşa et ve ışınla
                            player.sendSystemMessage(Component.literal("§c[CosmicArmor] §7Arena derin yeraltında (Y: -35) inşa ediliyor..."));
                            BlockPos origin = new BlockPos(player.getBlockX() - 55, -35, player.getBlockZ() - 56);
                            BlockPos statuePos = manager.initializeArenaAt(level, origin);

                            if (statuePos != null) {
                                player.teleportTo(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                            }
                            player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eKızıltaş Arenasına ışınlandın! Heykel tam karşında."));
                            return 1;
                        }))
                .then(Commands.literal("tp_dungeon")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            ArenaManager manager = ArenaManager.get(level);

                            if (manager.hasArena() && !manager.getArenaCenter().equals(BlockPos.ZERO)) {
                                BlockPos center = manager.getArenaCenter();
                                player.teleportTo(center.getX() + 0.5D, center.getY(), center.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                                player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eKızıltaş Zindanına ışınlandın!"));
                                return 1;
                            }

                            player.sendSystemMessage(Component.literal("§c[CosmicArmor] §7Zindan derin yeraltında (Y: -35) inşa ediliyor..."));
                            BlockPos origin = new BlockPos(player.getBlockX() - 55, -35, player.getBlockZ() - 56);
                            BlockPos statuePos = manager.initializeArenaAt(level, origin);

                            if (statuePos != null) {
                                player.teleportTo(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                            }
                            player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eKızıltaş Zindanına ışınlandın! Heykel tam karşında."));
                            return 1;
                        }))
                .then(Commands.literal("tp_biome")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            ArenaManager manager = ArenaManager.get(level);

                            if (manager.hasArena() && !manager.getArenaCenter().equals(BlockPos.ZERO)) {
                                BlockPos center = manager.getArenaCenter();
                                player.teleportTo(center.getX() + 0.5D, center.getY(), center.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                                player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eKızıltaş Zindanına ışınlandın!"));
                                return 1;
                            }

                            // Dünyada doğal olarak oluşan yapıyı ara
                            player.sendSystemMessage(Component.literal("§c[CosmicArmor] §7En yakın Kızıltaş Zindanı aranıyor..."));
                            BlockPos origin = new BlockPos(player.getBlockX() - 55, -35, player.getBlockZ() - 56);
                            BlockPos statuePos = manager.initializeArenaAt(level, origin);
                            if (statuePos != null) {
                                player.teleportTo(statuePos.getX() + 0.5D, statuePos.getY(), statuePos.getZ() + 8.5D);
                                player.setYRot(180.0F);
                                player.setXRot(0.0F);
                            }
                            player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eKızıltaş Zindanına ulaştın!"));
                            return 1;
                        }))
                .then(Commands.literal("reset_arena")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();

                            player.sendSystemMessage(Component.literal("§c[CosmicArmor] §7Arena onarılıyor ve sıfırlanıyor..."));
                            ArenaManager.get(level).resetAndRepairArena(level);
                            player.sendSystemMessage(Component.literal("§a[CosmicArmor] §eArena tamamen onarıldı ve Heykel sıfırlandı!"));
                            return 1;
                        }))
                .then(Commands.literal("set_curse")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 12))
                                .executes(context -> {
                                    int levelArg = IntegerArgumentType.getInteger(context, "level");
                                    ServerLevel level = context.getSource().getLevel();
                                    WorldCurseData.get(level).setCurseLevel(levelArg);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§c[CosmicArmor] §eDünya Laneti Seviyesi: " + levelArg), true);
                                    return 1;
                                })))
        );
    }
}
