package com.rustrelics.advancement;

import com.rustrelics.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class VillagerKillTrigger {

    private VillagerKillTrigger() {}

    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!event.getEntity().getType().equals(EntityType.VILLAGER)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        int kills = player.getData(ModAttachments.VILLAGER_KILLS);
        if (kills >= 10) {
            AdvancementHelper.grant(player, "secretos/conciencia_pesada", "10_villagers");
        }
    }
}
