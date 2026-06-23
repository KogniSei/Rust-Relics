package com.rustrelics.advancement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class WitherSummonTrigger {

    private WitherSummonTrigger() {}

    @SubscribeEvent
    public static void onWitherSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof WitherBoss)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerPlayer nearest = (ServerPlayer) level.getNearestPlayer(event.getEntity(), 32.0);
        if (nearest != null) {
            AdvancementHelper.grant(nearest, "lo_desataste", "wither_summoned");
        }
    }
}
