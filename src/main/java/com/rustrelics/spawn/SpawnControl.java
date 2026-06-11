package com.rustrelics.spawn;

import com.rustrelics.stage.StageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.Set;

/**
 * Control de spawns por stage. Porta stage_spawn_control.js.
 *
 * Stage 0 bloquea a las facciones avanzadas (Hominid, Illager Invasion,
 * Raids Enhanced, It Takes a Pillage). IDs verificados contra los .jar (ver
 * memoria rr-stage-system). Referenciados por string → soft-dep safe.
 *
 * Limitacion heredada: Pillager Caravans usa su propio spawner y no pasa por
 * este evento; se ajusta por su config, no aqui.
 */
public final class SpawnControl {

    private static final Set<String> STAGE0_BLOCKED = Set.of(
            // Hominid
            "hominid:bellman", "hominid:famished", "hominid:fossilized",
            "hominid:incendiary", "hominid:juggernaut", "hominid:mellified", "hominid:vampire",
            // Illager Invasion
            "illagerinvasion:alchemist", "illagerinvasion:archivist", "illagerinvasion:basher",
            "illagerinvasion:firecaller", "illagerinvasion:inquisitor", "illagerinvasion:invoker",
            "illagerinvasion:marauder", "illagerinvasion:necromancer", "illagerinvasion:provoker",
            "illagerinvasion:sorcerer", "illagerinvasion:surrendered",
            // Raids: Enhanced
            "raidsenhanced:zapper", "raidsenhanced:golem_of_last_resort",
            "raidsenhanced:raid_blimp", "raidsenhanced:raid_drill",
            // It Takes a Pillage
            "takesapillage:archer", "takesapillage:skirmisher", "takesapillage:legioner");

    private SpawnControl() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        ServerLevel level = event.getLevel().getLevel();
        if (StageManager.getStage(level) != 0) {
            return; // solo se bloquea en Stage 0
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        if (STAGE0_BLOCKED.contains(id)) {
            event.setSpawnCancelled(true);
        }
    }
}
