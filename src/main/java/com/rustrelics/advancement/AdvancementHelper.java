package com.rustrelics.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementHelper {

    private AdvancementHelper() {}

    public static void grant(ServerPlayer player, String advancementPath, String criterion) {
        if (player == null) return;
        AdvancementHolder adv = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("kubejs", advancementPath));
        if (adv != null) {
            var progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                progress.grantProgress(criterion);
            }
        }
    }
}
