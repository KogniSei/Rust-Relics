package com.rustrelics.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rustrelics.bloodmoon.BloodMoonManager;
import com.rustrelics.bloodmoon.DeadMoonManager;
import com.rustrelics.eclipse.SolarEclipseManager;
import com.rustrelics.silent.HealthTracker;
import com.rustrelics.silent.PillageThreatTracker;
import com.rustrelics.stage.SilentStage;
import com.rustrelics.stage.StageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de debug para validar el sistema de stages, eventos y silent stages
 * sin tener que jugar naturalmente.
 *
 * Requiere permiso de operador (nivel 2).
 */
public final class RustRelicsCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(
            "rrstage"
        ).requires(src -> src.hasPermission(2));

        // ================================================================
        // Stage global
        // ================================================================
        root.then(
            Commands.literal("get").executes(ctx -> {
                ServerLevel level = ctx.getSource().getLevel();
                int stage = StageManager.getStage(level);
                ctx.getSource().sendSuccess(
                    () -> Component.literal("§c[R&R] §fStage global: §e" + stage),
                    false
                );
                return stage;
            })
        );

        root.then(
            Commands.literal("set").then(
                Commands.argument(
                    "value",
                    IntegerArgumentType.integer(0, 5)
                ).executes(ctx -> {
                    int value = IntegerArgumentType.getInteger(ctx, "value");
                    ServerLevel level = ctx.getSource().getLevel();
                    StageManager.setStageDirect(level, value);
                    ctx.getSource().sendSuccess(
                        () -> Component.literal(
                            "§c[R&R] §fStage fijado a §e" +
                                value +
                                " §7(scoreboard rr_stage sincronizado)"
                        ),
                        true
                    );
                    return value;
                })
            )
        );

        // ================================================================
        // Eventos: Luna Palida
        // ================================================================
        root.then(
            Commands.literal("bloodmoon").then(
                Commands.argument("active", BoolArgumentType.bool())
                    .executes(ctx -> {
                        boolean active = BoolArgumentType.getBool(ctx, "active");
                        BloodMoonManager.forceSet(ctx.getSource().getServer(), active);
                        return 1;
                    })
            )
        );

        // ================================================================
        // Eventos: Luna Muerta
        // ================================================================
        root.then(
            Commands.literal("deadmoon").then(
                Commands.argument("active", BoolArgumentType.bool())
                    .executes(ctx -> {
                        boolean active = BoolArgumentType.getBool(ctx, "active");
                        DeadMoonManager.forceSet(ctx.getSource().getServer(), active);
                        return 1;
                    })
            )
        );

        // ================================================================
        // Eventos: Eclipse Solar
        // ================================================================
        root.then(
            Commands.literal("eclipse").then(
                Commands.argument("active", BoolArgumentType.bool())
                    .executes(ctx -> {
                        boolean active = BoolArgumentType.getBool(ctx, "active");
                        SolarEclipseManager.forceSet(ctx.getSource().getServer(), active);
                        return 1;
                    })
            )
        );

        // ================================================================
        // Silent Stage (rr_silent_stage)
        // ================================================================
        root.then(
            Commands.literal("silent").then(
                Commands.literal("get").then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            int val = SilentStage.readScoreboard(player);
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                    "§c[R&R] §fSilent Stage de §e" + player.getScoreboardName() +
                                        "§f: §e" + val),
                                false
                            );
                            return val;
                        })
                ).then(
                    Commands.literal("set").then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("value", IntegerArgumentType.integer(0, 5))
                                    .executes(ctx -> {
                                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                        int value = IntegerArgumentType.getInteger(ctx, "value");
                                        SilentStage.writeScoreboard(player.serverLevel(), player, value);
                                        ctx.getSource().sendSuccess(
                                            () -> Component.literal(
                                                "§c[R&R] §fSilent Stage de §e" +
                                                    player.getScoreboardName() +
                                                    "§f fijado a §e" + value),
                                            true
                                        );
                                        return value;
                                    })
                            )
                    )
                )
            )
        );

        // ================================================================
        // Health Stage (rr_health_stage)
        // ================================================================
        root.then(
            Commands.literal("health").then(
                Commands.literal("get").then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            // Leer directamente del scoreboard
                            var obj = ctx.getSource().getServer().getScoreboard()
                                    .getObjective("rr_health_stage");
                            int val;
                            if (obj == null) {
                                val = 0;
                            } else {
                                var access = ctx.getSource().getServer().getScoreboard()
                                        .getOrCreatePlayerScore(
                                                net.minecraft.world.scores.ScoreHolder.forNameOnly(
                                                        player.getScoreboardName()), obj);
                                val = access.get();
                            }
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                    "§c[R&R] §fHealth Stage de §e" + player.getScoreboardName() +
                                        "§f: §e" + val),
                                false
                            );
                            return val;
                        })
                ).then(
                    Commands.literal("set").then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("value", IntegerArgumentType.integer(0, 1))
                                    .executes(ctx -> {
                                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                        int value = IntegerArgumentType.getInteger(ctx, "value");
                                        HealthTracker.forceSet(player, value);
                                        ctx.getSource().sendSuccess(
                                            () -> Component.literal(
                                                "§c[R&R] §fHealth Stage de §e" +
                                                    player.getScoreboardName() +
                                                    "§f fijado a §e" + value),
                                            true
                                        );
                                        return value;
                                    })
                            )
                    )
                )
            )
        );

        // ================================================================
        // Pillage Threat (rr_pillage_threat / rr_pillage_kills)
        // ================================================================
        root.then(
            Commands.literal("pillage").then(
                Commands.literal("get").then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            int kills = player.getData(com.rustrelics.attachment.ModAttachments.PILLAGER_KILLS);
                            int threat = PillageThreatTracker.calcThreat(kills);
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                    "§c[R&R] §fPillage de §e" + player.getScoreboardName() +
                                        "§f: §e" + kills + " §fkills §7/ §e" + threat + " §fthreat"),
                                false
                            );
                            return kills;
                        })
                ).then(
                    Commands.literal("set").then(
                        Commands.argument("player", EntityArgument.player())
                            .then(
                                Commands.argument("kills", IntegerArgumentType.integer(0, 200))
                                    .executes(ctx -> {
                                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                        int kills = IntegerArgumentType.getInteger(ctx, "kills");
                                        PillageThreatTracker.forceSet(player, kills);
                                        int threat = PillageThreatTracker.calcThreat(kills);
                                        ctx.getSource().sendSuccess(
                                            () -> Component.literal(
                                                "§c[R&R] §fPillage de §e" +
                                                    player.getScoreboardName() +
                                                    "§f fijado a §e" + kills +
                                                    " §fkills (threat §e" + threat + "§f)"),
                                            true
                                        );
                                        return kills;
                                    })
                            )
                    )
                )
            )
        );

        event.getDispatcher().register(root);
    }
}
