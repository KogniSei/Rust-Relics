package com.rustrelics.eclipse;

import com.rustrelics.stage.StageSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Apaga el fuego de los mobs durante el Eclipse Solar para que los hostiles no se
 * quemen con el sol. (Los buffs de combate del eclipse viven en {@link EclipseBuffs}.)
 *
 * Solo corre cuando el eclipse esta activo (eclipseTicks > 0). Restringido al
 * overworld: el eclipse es un fenomeno de la superficie. Itera las entidades
 * cargadas en vez de pedir getEntitiesOfClass sobre el AABB del world border.
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

        for (Entity entity : overworld.getAllEntities()) {
            if (entity instanceof Mob mob && mob.isOnFire()) {
                mob.clearFire();
            }
        }
    }
}
