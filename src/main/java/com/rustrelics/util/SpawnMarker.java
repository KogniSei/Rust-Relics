package com.rustrelics.util;

import net.minecraft.world.entity.Entity;

/**
 * Marca COMPARTIDA para los mobs "extra" que generan los sistemas de buff
 * (luna pálida, luna muerta, hardmode).
 *
 * Cada sistema tenia antes su propio tag (rr_bm_buffed, rr_dm_buffed, ...), lo
 * que cortaba la recursion dentro de un sistema pero NO entre sistemas: con dos
 * eventos activos a la vez, un extra de un sistema disparaba el spawn del otro y
 * viceversa hasta provocar un StackOverflow (mob-bomb cruzada).
 *
 * Con un unico tag compartido, cualquier mob que YA es un extra es ignorado por
 * TODOS los sistemas de spawn: nunca se multiplica de nuevo, sin importar cuantos
 * eventos esten activos simultaneamente.
 */
public final class SpawnMarker {

    private static final String EXTRA_TAG = "rr_spawned_extra";

    private SpawnMarker() {}

    /** true si la entidad fue generada como "extra" por alguno de los sistemas de buff. */
    public static boolean isExtra(Entity entity) {
        return entity.getPersistentData().getBoolean(EXTRA_TAG);
    }

    /** Marca la entidad como "extra" para que ningun sistema de buff la multiplique. */
    public static void markExtra(Entity entity) {
        entity.getPersistentData().putBoolean(EXTRA_TAG, true);
    }
}
