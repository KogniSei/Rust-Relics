package com.rustrelics.stage;

import com.rustrelics.RustRelics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class EternalNightManager {

    private static final int CHECK_INTERVAL = 20;
    private static final long NIGHT_START = 18000L;
    private static final long DAY_CYCLE = 24000L;

    private static final long NIGHT_RANGE_START = 18000L;
    private static final long NIGHT_RANGE_END = 23999L;

    private EternalNightManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;

        ServerLevel overworld = server.overworld();
        int stage = StageManager.getStage(overworld);

        if (stage != 4) return;

        long dayTime = overworld.getDayTime() % DAY_CYCLE;

        if (dayTime < NIGHT_RANGE_START || dayTime > NIGHT_RANGE_END) {
            long currentDay = overworld.getDayTime() - dayTime;
            overworld.setDayTime(currentDay + NIGHT_START);
        }
    }

    public static void restoreDayCycle(ServerLevel level) {
        long dayTime = level.getDayTime() % DAY_CYCLE;
        if (dayTime >= NIGHT_RANGE_START && dayTime <= NIGHT_RANGE_END) {
            long currentDay = level.getDayTime() - dayTime;
            level.setDayTime(currentDay + 1000L);
        }
    }
}
