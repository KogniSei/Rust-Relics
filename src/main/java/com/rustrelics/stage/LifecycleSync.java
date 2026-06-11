package com.rustrelics.stage;

import com.rustrelics.RustRelics;
import com.rustrelics.bloodmoon.BloodMoonManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Rehydrates scoreboard mirrors from SavedData when a world is opened.
 */
public final class LifecycleSync {

    private LifecycleSync() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        StageManager.syncScoreboard(server);
        BloodMoonManager.syncScoreboard(server);
        RustRelics.LOGGER.info("[R&R] Scoreboard mirrors synchronized from SavedData.");
    }
}
