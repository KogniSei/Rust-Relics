package com.rustrelics.eclipse;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Durante el Eclipse Solar los animales escasean: cancela ~70% de los spawns de
 * criaturas pasivas ({@link MobCategory#CREATURE}) en el overworld.
 */
public final class EclipseSpawnControl {

    private static final float ANIMAL_CANCEL_CHANCE = 0.70f;

    private EclipseSpawnControl() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        ServerLevel level = event.getLevel().getLevel();
        if (!EclipseBuffs.eclipseActive(level)) return;
        if (event.getEntity().getType().getCategory() != MobCategory.CREATURE) return;
        if (level.getRandom().nextFloat() < ANIMAL_CANCEL_CHANCE) {
            event.setSpawnCancelled(true);
        }
    }
}
