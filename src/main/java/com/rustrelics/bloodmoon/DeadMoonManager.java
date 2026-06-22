package com.rustrelics.bloodmoon;

import com.rustrelics.RustRelics;
import com.rustrelics.stage.StageSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DeadMoonManager {

    private static final int NIGHT_START = 13000;
    private static final int NIGHT_END = 23000;
    private static final int WINDOW = 200;

    private DeadMoonManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 40 != 0) return;

        ServerLevel overworld = server.overworld();
        StageSavedData data = StageSavedData.get(overworld);

        long absTime = overworld.getDayTime();
        long dayTime = absTime % 24000L;
        long dayCount = absTime / 24000L;
        long moonPhase = dayCount % 8L;

        boolean active = data.getDeadmoon() == 1;

        // -- Cae la noche: verificacion unica --
        if (dayTime >= NIGHT_START && dayTime < NIGHT_START + WINDOW) {
            if (!data.isDmChecked()) {
                data.setDmChecked(true);
                data.setDmDawnSent(false);
                if (moonPhase == 4L) {
                    data.setDeadmoon(1);
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§8[Rust & Relics] §fLa luna muerta emerge... los mobs se agitan."),
                        false
                    );
                    RustRelics.LOGGER.info("[R&R] Luna muerta activada (dia {}).", dayCount);
                }
            }
        }

        // -- Amanecer --
        if (dayTime >= NIGHT_END && dayTime < NIGHT_END + WINDOW) {
            if (!data.isDmDawnSent()) {
                data.setDmDawnSent(true);
                if (active) {
                    data.setDeadmoon(0);
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§7[Rust & Relics] §fLa luna muerta se desvanece con el alba."),
                        false
                    );
                    RustRelics.LOGGER.info("[R&R] Luna muerta desactivada (amanecer).");
                }
            }
        }

        // -- Mediodia: resetear flag --
        if (dayTime >= 5900 && dayTime < 6100) {
            data.setDmChecked(false);
        }
    }

    public static boolean isActive(ServerLevel level) {
        return StageSavedData.get(level).getDeadmoon() == 1;
    }
}
