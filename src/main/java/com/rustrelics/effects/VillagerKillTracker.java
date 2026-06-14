package com.rustrelics.effects;

import com.rustrelics.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class VillagerKillTracker {

    private VillagerKillTracker() {}

    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) return;

        int current = player.getData(ModAttachments.VILLAGER_KILLS);
        player.setData(ModAttachments.VILLAGER_KILLS, current + 1);
    }
}
