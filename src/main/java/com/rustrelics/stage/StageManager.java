package com.rustrelics.stage;

import com.rustrelics.util.Scoreboards;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Logica de progresion de stage global. Equivalente nativo de stage_helper.js.
 *
 * Fuente de verdad: {@link StageSavedData}. Espejo de compat: scoreboard
 * rr_stage del fake player "$world", escrito en cada cambio para que datapacks
 * y los scripts KubeJS restantes lo lean sin cambios.
 */
public final class StageManager {

    public static final String SCOREBOARD_OBJECTIVE = "rr_stage";
    private static final String WORLD_HOLDER = "$world";

    private static final String PREFIX = "§c[Rust & Relics] ";

    // Mensajes de avance, indexados por (targetStage - 1). Identicos a stage_helper.js.
    private static final String[] ADVANCE_MESSAGES = {
        "§6§lUn nuevo peligro despierta en las tierras...", // 0 -> 1
        "§9§lLas aguas profundas responden al llamado.", // 1 -> 2
        "§4§lEl Nether se ha abierto. Nada volvera a ser igual.", // 2 -> 3
        "§5§lUn terrible poder ha resucitado. Resistid.", // 3 -> 4
        "§b§lEl Ender Dragon ha caido. Ha comenzado el fin.", // 4 -> 5
    };

    private StageManager() {}

    /** Lee el stage global actual (0 si no inicializado). */
    public static int getStage(ServerLevel level) {
        return StageSavedData.get(level).getStage();
    }

    public static boolean isStageAtLeast(ServerLevel level, int minStage) {
        return getStage(level) >= minStage;
    }

    /**
     * Sube el stage global a {@code targetStage} SOLO si es mayor al actual.
     * Idempotente: la primera llamada gana. Actualiza SavedData + scoreboard y
     * notifica a todos los jugadores online.
     *
     * @return true si el stage efectivamente cambio.
     */
    public static boolean advanceStage(ServerLevel level, int targetStage) {
        MinecraftServer server = level.getServer();
        StageSavedData data = StageSavedData.get(level);
        int current = data.getStage();
        if (targetStage <= current) {
            return false;
        }

        data.setStage(targetStage);
        mirrorToScoreboard(server, targetStage);
        broadcastAdvance(server, current, targetStage);
        // Si es Stage 4+, aplicar buffs permanentes a todos los jugadores online
        if (targetStage >= 4) {
            Stage4PlayerBuffs.applyToAll(level);
        }
        // Si es Stage 5+, restaurar el ciclo diurno (fin de la noche eterna)
        if (targetStage >= 5) {
            EternalNightManager.restoreDayCycle(level);
        }
        return true;
    }

    /** Fija un stage absoluto (comando de debug). Puede bajar. No hace broadcast de avance. */
    public static void setStageDirect(ServerLevel level, int value) {
        StageSavedData.get(level).setStage(value);
        mirrorToScoreboard(level.getServer(), value);
        // Para que /rrstage set 4 sea testeable: aplica los buffs de Stage 4 ya.
        if (value >= 4) {
            Stage4PlayerBuffs.applyToAll(level);
        }
    }

    /** Reescribe el scoreboard desde SavedData al abrir un mundo ya progresado. */
    public static void syncScoreboard(MinecraftServer server) {
        mirrorToScoreboard(server, StageSavedData.get(server.overworld()).getStage());
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private static void broadcastAdvance(MinecraftServer server, int from, int to) {
        int idx = to - 1;
        String detail = (idx >= 0 && idx < ADVANCE_MESSAGES.length)
            ? ADVANCE_MESSAGES[idx]
            : "§fMundo avanzado al Stage " + to;
        String line = "§7— Stage Global: §e" + from + " §7→ §6" + to + " §7—";

        server.getPlayerList().broadcastSystemMessage(Component.literal(PREFIX + detail), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal(line), false);
    }

    /** Espejo de solo escritura al scoreboard vanilla (compat KubeJS/datapacks). */
    private static void mirrorToScoreboard(MinecraftServer server, int value) {
        Scoreboards.set(server, SCOREBOARD_OBJECTIVE, "R&R Stage", WORLD_HOLDER, value);
    }
}
