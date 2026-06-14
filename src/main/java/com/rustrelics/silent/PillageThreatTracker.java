package com.rustrelics.silent;

import com.rustrelics.attachment.ModAttachments;
import com.rustrelics.util.Scoreboards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Stage silencioso que escala la agresividad de los pillagers segun cuantos
 * ha matado el jugador. Cada 15 kills sube un nivel (0-5).
 *
 * Escribe el nivel actual en el scoreboard {@code rr_pillage_threat}
 * y el contador bruto en {@code rr_pillage_kills}.
 *
 * Niveles:
 *   0 = 0-14 kills  — sin cambio
 *   1 = 15-29       — mas spawns
 *   2 = 30-44       — pillagers con armadura parcial
 *   3 = 45-59       — invasiones mas frecuentes
 *   4 = 60-89       — pillagers con encantamientos
 *   5 = 90+         — maxima agresividad
 */
public final class PillageThreatTracker {

    private static final String THREAT_SCOREBOARD = "rr_pillage_threat";
    private static final String KILLS_SCOREBOARD = "rr_pillage_kills";
    private static final int KILLS_PER_LEVEL = 15;
    private static final int MAX_LEVEL = 5;

    private PillageThreatTracker() {}

    @SubscribeEvent
    public static void onRaiderDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Raider)) return;
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) return;

        int currentKills = player.getData(ModAttachments.PILLAGER_KILLS);
        int newKills = currentKills + 1;
        player.setData(ModAttachments.PILLAGER_KILLS, newKills);

        writeScores(player, newKills);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int kills = player.getData(ModAttachments.PILLAGER_KILLS);
        if (kills > 0) writeScores(player, kills);
    }

    private static void writeScores(ServerPlayer player, int kills) {
        int threat = calcThreat(kills);
        Scoreboards.set(player.serverLevel().getServer(), KILLS_SCOREBOARD,
                "Pillager Kills", player.getScoreboardName(), kills);
        Scoreboards.set(player.serverLevel().getServer(), THREAT_SCOREBOARD,
                "Pillage Threat", player.getScoreboardName(), threat);
    }

    public static int calcThreat(int kills) {
        int level = kills / KILLS_PER_LEVEL;
        return Math.min(level, MAX_LEVEL);
    }
}
