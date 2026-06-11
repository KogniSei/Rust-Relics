package com.rustrelics.bloodmoon;

import com.rustrelics.RustRelics;
import com.rustrelics.stage.StageSavedData;
import com.rustrelics.util.Scoreboards;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Maquina de estados de la Luna de Sangre. Porta la deteccion nocturna de
 * blood_moon.js. Evento global y barato: una sola comprobacion cada 40 ticks
 * sobre el overworld, NO un bucle por jugador.
 *
 * Activacion: cae la noche (13000) + fase lunar 0 (luna llena) + 30% prob.
 * Desactivacion: amanecer (23000).
 * Flags de una-vez-por-noche persistidos en {@link StageSavedData}.
 */
public final class BloodMoonManager {

    private static final String BM_OBJECTIVE = "rr_bloodmoon";
    private static final String WORLD_HOLDER = "$world";

    private static final int NIGHT_START = 13000;
    private static final int NIGHT_END = 23000;
    private static final int WINDOW = 200;
    private static final float CHANCE = 0.30f;

    private BloodMoonManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 40 != 0) {
            return;
        }

        ServerLevel overworld = server.overworld();
        StageSavedData data = StageSavedData.get(overworld);
        if (data.getStage() < 1) {
            return; // luna de sangre solo en Stage 1+
        }

        long absTime = overworld.getDayTime();
        long dayTime = absTime % 24000L;
        long dayCount = absTime / 24000L;
        long moonPhase = dayCount % 8L; // 0 = luna llena

        boolean active = data.getBloodmoon() == 1;

        // -- Cae la noche: verificacion unica --
        if (dayTime >= NIGHT_START && dayTime < NIGHT_START + WINDOW) {
            if (!data.isBmChecked()) {
                data.setBmChecked(true);
                data.setBmDawnSent(false);
                if (
                    moonPhase == 0L &&
                    overworld.getRandom().nextFloat() < CHANCE
                ) {
                    data.setBloodmoon(1);
                    mirror(server, 1);
                    server
                        .getPlayerList()
                        .broadcastSystemMessage(
                            Component.literal(
                                "§4[Rust & Relics] §fLa luna enrojece. Esta noche sera larga."
                            ),
                            false
                        );
                    RustRelics.LOGGER.info(
                        "[R&R] Luna de Sangre activada (dia {}).",
                        dayCount
                    );
                }
            }
        }

        // -- Amanecer --
        if (dayTime >= NIGHT_END && dayTime < NIGHT_END + WINDOW) {
            if (!data.isBmDawnSent()) {
                data.setBmDawnSent(true);
                if (active) {
                    data.setBloodmoon(0);
                    mirror(server, 0);
                    server
                        .getPlayerList()
                        .broadcastSystemMessage(
                            Component.literal(
                                "§6[Rust & Relics] §fEl amanecer llega. Sobreviviste."
                            ),
                            false
                        );
                    RustRelics.LOGGER.info(
                        "[R&R] Luna de Sangre desactivada (amanecer)."
                    );
                }
            }
        }

        // -- Mediodia: resetear flag para la proxima noche --
        if (dayTime >= 5900 && dayTime < 6100) {
            data.setBmChecked(false);
        }
    }

    /** Reescribe el scoreboard desde SavedData al abrir un mundo ya progresado. */
    public static void syncScoreboard(MinecraftServer server) {
        mirror(server, StageSavedData.get(server.overworld()).getBloodmoon());
    }

    /** Espejo de solo escritura del flag al scoreboard rr_bloodmoon (compat datapacks). */
    private static void mirror(MinecraftServer server, int value) {
        Scoreboards.set(server, BM_OBJECTIVE, "R&R Blood Moon", WORLD_HOLDER, value);
    }
}
