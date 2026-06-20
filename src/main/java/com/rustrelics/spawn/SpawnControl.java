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
            "takesapillage:archer", "takesapillage:skirmisher", "takesapillage:legioner",
            // ================================================================
            // Born in Chaos — TODOS los mobs bloqueados en Stage 0
            // (incluyendo bosses; los bosses se desbloquean en stages superiores)
            // ================================================================
            // Normales
            "born_in_chaos_v1:baby_skeleton", "born_in_chaos_v1:baby_spider",
            "born_in_chaos_v1:barrel_zombie", "born_in_chaos_v1:bloody_gadfly",
            "born_in_chaos_v1:bone_imp", "born_in_chaos_v1:bonescaller",
            "born_in_chaos_v1:corpse_fish", "born_in_chaos_v1:corpse_fly",
            "born_in_chaos_v1:dark_vortex", "born_in_chaos_v1:decaying_zombie",
            "born_in_chaos_v1:decrepit_skeleton", "born_in_chaos_v1:diamond_termite",
            "born_in_chaos_v1:dire_hound_leader", "born_in_chaos_v1:door_knight",
            "born_in_chaos_v1:dread_hound", "born_in_chaos_v1:fallen_chaos_knight",
            "born_in_chaos_v1:felsteed", "born_in_chaos_v1:firelight",
            "born_in_chaos_v1:glutton_fish", "born_in_chaos_v1:infernal_spirit",
            "born_in_chaos_v1:krampus", "born_in_chaos_v1:krampus_henchman",
            "born_in_chaos_v1:lifestealer", "born_in_chaos_v1:lifestealer_true_form",
            "born_in_chaos_v1:lord_pumpkinhead", "born_in_chaos_v1:lord_pumpkinhead_head",
            "born_in_chaos_v1:lord_pumpkinhead_withouta_horse",
            "born_in_chaos_v1:lord_the_headless", "born_in_chaos_v1:lords_felsteed",
            "born_in_chaos_v1:maggot", "born_in_chaos_v1:missionary_raider",
            "born_in_chaos_v1:missioner", "born_in_chaos_v1:mother_spider",
            "born_in_chaos_v1:mr_pumpkin", "born_in_chaos_v1:mrs_pumpkin",
            "born_in_chaos_v1:nightmare_stalker", "born_in_chaos_v1:phantom_creeper",
            "born_in_chaos_v1:pumpkin_bruiser", "born_in_chaos_v1:pumpkin_dunce",
            "born_in_chaos_v1:pumpkin_spirit", "born_in_chaos_v1:pumpkinhead",
            "born_in_chaos_v1:restless_spirit", "born_in_chaos_v1:scarlet_persecutor",
            "born_in_chaos_v1:seared_spirit", "born_in_chaos_v1:senor_pumpkin",
            "born_in_chaos_v1:siamese_skeletons", "born_in_chaos_v1:sir_pumpkinhead",
            "born_in_chaos_v1:sir_pumpkinhead_without_horse",
            "born_in_chaos_v1:sir_the_headless", "born_in_chaos_v1:skeleton_demoman",
            "born_in_chaos_v1:skeleton_thrasher", "born_in_chaos_v1:spirit_guide",
            "born_in_chaos_v1:spirit_guide_assistant",
            "born_in_chaos_v1:spiritof_chaos", "born_in_chaos_v1:supreme_bonescaller",
            "born_in_chaos_v1:supreme_bonescaller_stage_2",
            "born_in_chaos_v1:swarmer", "born_in_chaos_v1:thornshell_crab",
            "born_in_chaos_v1:zombie_bruiser", "born_in_chaos_v1:zombie_clown",
            "born_in_chaos_v1:zombie_fisherman", "born_in_chaos_v1:zombie_lumberjack",
            // not_despawn variants (tratados como normales)
            "born_in_chaos_v1:bonescaller_not_despawn",
            "born_in_chaos_v1:decaying_zombie_not_despawn",
            "born_in_chaos_v1:door_knight_not_despawn",
            "born_in_chaos_v1:dread_hound_not_despawn",
            "born_in_chaos_v1:firelight_not_despawn",
            "born_in_chaos_v1:seared_spirit_not_despawn",
            "born_in_chaos_v1:skeleton_thrasher_not_despawn",
            "born_in_chaos_v1:supreme_bonescaller_not_despawn",
            "born_in_chaos_v1:zombie_clown_not_despawn");

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
