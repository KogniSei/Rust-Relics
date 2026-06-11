package com.rustrelics.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Helper para espejar valores al scoreboard vanilla (compat datapacks/KubeJS).
 *
 * Centraliza el patron "obtener-o-crear objetivo DUMMY + fijar score" que antes
 * estaba copiado en StageManager, BloodMoonManager y SilentStage.
 */
public final class Scoreboards {

    private Scoreboards() {
    }

    /**
     * Fija {@code value} para {@code holder} en el objetivo {@code objectiveName},
     * creando el objetivo (DUMMY, render INTEGER) la primera vez.
     *
     * @param holder nombre del score holder ("$world" global, o el username del jugador)
     */
    public static void set(MinecraftServer server, String objectiveName, String displayName,
                           String holder, int value) {
        Scoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective(objectiveName);
        if (obj == null) {
            obj = sb.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                    Component.literal(displayName), ObjectiveCriteria.RenderType.INTEGER, false, null);
        }
        sb.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), obj).set(value);
    }
}
