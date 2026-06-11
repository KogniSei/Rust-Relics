package com.rustrelics.eclipse;

import com.rustrelics.RustRelics;
import com.rustrelics.stage.StageSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Maquina de estados del Eclipse Solar.
 *
 * Activo solo en STAGE 5 (Endgame). En Stage 4 el mundo esta en noche eterna
 * (ver EternalNightManager), que congela el tiempo y dejaria al eclipse sin sus
 * ventanas horarias; recien al matar al Ender Dragon (Stage 5) el ciclo dia/noche
 * vuelve y el eclipse cobra sentido.
 *
 * Cada noche hay 15% de probabilidad de que el amanecer siguiente sea un eclipse:
 * el cielo se oscurece, los mobs hostiles no se queman con el sol, y la atmosfera
 * se vuelve tensa. Dura un dia completo (24000 ticks).
 */
public final class SolarEclipseManager {

    private static final int REQUIRED_STAGE = 5;
    private static final int ECLIPSE_DURATION = 24000;
    private static final int CHECK_INTERVAL = 40;
    private static final int DAWN_WINDOW_START = 23500;
    private static final int DAWN_WINDOW_END = 23999;
    private static final int DUSK_TIME = 13000;
    private static final float ECLIPSE_CHANCE = 0.15f;

    private SolarEclipseManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;

        ServerLevel overworld = server.overworld();
        StageSavedData data = StageSavedData.get(overworld);
        if (data.getStage() < REQUIRED_STAGE) return;

        long dayTime = overworld.getDayTime() % 24000L;
        long eclipseTicks = data.getEclipseTicks();

        // --- Eclipse activo: contar hacia abajo ---
        if (eclipseTicks > 0) {
            long next = Math.max(0, eclipseTicks - CHECK_INTERVAL);
            data.setEclipseTicks(next);

            // Forzar lluvia para que mobs no ardan (isSunBurnTick chequea !isRaining)
            overworld.setWeatherParameters(0, (int) (next + 100), true, false);

            if (next == 0) {
                overworld.setWeatherParameters(999999, 0, false, false);
                server.getPlayerList().broadcastSystemMessage(Component.literal(
                        "§6[Rust & Relics] §fEl sol regresa. El eclipse ha pasado."), false);
                RustRelics.LOGGER.info("[R&R] Eclipse Solar terminado.");
            }
            return;
        }

        // --- No hay eclipse activo: ventana de amanecer marca el flag ---
        if (dayTime >= DAWN_WINDOW_START && dayTime <= DAWN_WINDOW_END) {
            if (!data.isEclipseChecked()) {
                data.setEclipseChecked(true);
                data.setEclipseDawnSent(false);
            }
            return;
        }

        // --- Al atardecer, decidir si manana hay eclipse ---
        if (dayTime >= DUSK_TIME && dayTime < DUSK_TIME + CHECK_INTERVAL) {
            if (!data.isEclipseChecked()) {
                data.setEclipseChecked(true);
                if (overworld.getRandom().nextFloat() < ECLIPSE_CHANCE) {
                    data.setEclipseTicks(ECLIPSE_DURATION);
                    data.setEclipseDawnSent(true);
                    server.getPlayerList().broadcastSystemMessage(Component.literal(
                            "§4[Rust & Relics] §fEl sol se oculta tras un velo antinatural. Mañana no saldra el sol."),
                            false);
                    RustRelics.LOGGER.info("[R&R] Eclipse Solar programado para el amanecer.");
                }
            }
        }

        // Resetear flag al mediodia para el siguiente ciclo
        if (dayTime >= 5900 && dayTime < 6100) {
            data.setEclipseChecked(false);
        }
    }
}
