package com.rustrelics.silent;

import com.rustrelics.util.Scoreboards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Stage silencioso que mide si el jugador alcanzo 13+ corazones (26 HP max).
 * Escribe 1 en el scoreboard {@code rr_health_stage} cuando se cumple,
 * 0 en caso contrario.
 *
 * Sirve para que InControl y datapacks aumenten el spawn rate general cuando
 * el jugador es lo suficientemente fuerte (13 corazones = no es principiante).
 */
public final class HealthTracker {

    private static final int THIRTEEN_HEARTS = 26;
    private static final String SCOREBOARD = "rr_health_stage";

    private HealthTracker() {}

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        recalculate(player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        recalculate(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        recalculate(player);
    }

    /**
     * Reevalua el health stage del jugador a partir de su vida maxima ACTUAL
     * (incluye modificadores de atributo). Publico para poder recalcular justo
     * despues de aplicar buffs de vida que no pasan por un cambio de equipo
     * (Stage 4, bendicion del Elder Guardian, etc.).
     */
    public static void recalculate(ServerPlayer player) {
        float hp = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        int value = hp >= THIRTEEN_HEARTS ? 1 : 0;
        forceSet(player, value);
    }

    /**
     * Fuerza el valor de health stage para un jugador (debug).
     */
    public static void forceSet(ServerPlayer player, int value) {
        Scoreboards.set(player.serverLevel().getServer(), SCOREBOARD, "Health Stage",
                player.getScoreboardName(), value);
    }
}
