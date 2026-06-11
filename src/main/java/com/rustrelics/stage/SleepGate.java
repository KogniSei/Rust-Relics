package com.rustrelics.stage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

/**
 * Bloqueo de sueno durante la noche eterna (Stage 4).
 *
 * "La oscuridad no descansa": al matar al Wither el mundo entra en noche eterna
 * y los jugadores no pueden dormir. El sueno se restaura en Stage 5.
 *
 * Usa {@link CanPlayerSleepEvent} (NeoForge 1.21.1) — dispara al evaluar si el
 * jugador puede dormir; fijar un problema cancela el sueno.
 */
public final class SleepGate {

    private static final String BLOCK_MESSAGE =
            "§4[Rust & Relics] §fLa oscuridad no descansa. Tampoco tú.";

    private SleepGate() {}

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        if (StageManager.getStage(level) == 4) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(Component.literal(BLOCK_MESSAGE), true);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.3f, 1.0f);
        }
    }
}
