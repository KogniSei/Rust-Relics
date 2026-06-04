package com.rustrelics.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rustrelics.stage.StageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comando de debug para validar el sistema de stages sin tener que matar bosses.
 *
 *   /rrstage get          -> imprime el stage global actual
 *   /rrstage set <n>      -> fija el stage (requiere permiso de operador)
 *
 * Solo para testing; no es contenido de jugador.
 */
public final class RustRelicsCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rrstage")
                .requires(src -> src.hasPermission(2));

        root.then(Commands.literal("get").executes(ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            int stage = StageManager.getStage(level);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("§c[R&R] §fStage global: §e" + stage), false);
            return stage;
        }));

        root.then(Commands.literal("set")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 5))
                        .executes(ctx -> {
                            int value = IntegerArgumentType.getInteger(ctx, "value");
                            ServerLevel level = ctx.getSource().getLevel();
                            StageManager.setStageDirect(level, value);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§c[R&R] §fStage fijado a §e" + value
                                            + " §7(scoreboard rr_stage sincronizado)"), true);
                            return value;
                        })));

        event.getDispatcher().register(root);
    }
}
