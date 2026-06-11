package com.rustrelics.eclipse;

import com.rustrelics.stage.StageSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Evita que los mobs se quemen con el sol durante un Eclipse Solar.
 *
 * Solo corre cuando el eclipse esta activo (eclipseTicks > 0), asi que el coste
 * en estado normal es un solo chequeo de int. Restringido al overworld: el
 * eclipse es un fenomeno de la superficie; apagar fuego en Nether/End quitaria
 * fuego legitimo (lava) y seria trabajo desperdiciado.
 */
public final class EclipseMobEffects {

    private static final int INTERVAL = 20;

    private EclipseMobEffects() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) return;

        ServerLevel overworld = server.overworld();
        if (StageSavedData.get(overworld).getEclipseTicks() <= 0) return;

        for (Entity entity : overworld.getEntities().getAll()) {
            if (entity instanceof Mob mob && mob.isOnFire()) {
                mob.clearFire();
            }
        }
    }
}
