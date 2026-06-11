package com.rustrelics.stage;

import com.rustrelics.RustRelics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;

/**
 * Detonantes de avance de stage. Equivalente nativo de stage_events.js + portal.
 *
 * Bosses (LivingDeathEvent): la muerte de cierto boss sube el stage global.
 * Nether (PlayerChangedDimensionEvent): entrar al Nether activa Hardmode (stage 3).
 *
 * advanceStage es idempotente — no importa cuantos jugadores lo disparen, gana
 * la primera. Boss de Galosphere referenciado por ID; si el mod falta, nunca
 * coincide y no pasa nada.
 */
public final class StageTriggers {

    /** Mapa boss-id -> stage objetivo. */
    private static final Map<String, Integer> BOSS_STAGE = Map.of(
            "galosphere:berserker", 1,
            "minecraft:elder_guardian", 2,
            "minecraft:wither", 4,
            "minecraft:ender_dragon", 5);

    private StageTriggers() {
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        String idStr = id.toString();

        // Stage Secreto: Warden — no avanza stage, solo marca flag silencioso
        if ("minecraft:warden".equals(idStr)) {
            StageSavedData data = StageSavedData.get(level);
            if (!data.isWardenSlain()) {
                data.setWardenSlain(true);
                RustRelics.LOGGER.info("[R&R] Warden derrotado. Bendicion silenciosa concedida.");
            }
            return;
        }

        Integer target = BOSS_STAGE.get(idStr);
        if (target == null) {
            return;
        }

        boolean changed = StageManager.advanceStage(level, target);
        if (changed) {
            // Mensaje tematico extra del Berserker (paridad con stage_events.js).
            if (target == 1) {
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§6[Rust & Relics] §fThe world has changed."), false);
            }
            RustRelics.LOGGER.info("[R&R] Stage avanzado a {} por muerte de {}.", target, id);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        // Entrar al Nether activa Hardmode (stage 3). PlayerChangedDimensionEvent
        // dispara exacto al cruzar — sin polling de bloque nether_portal.
        if (event.getTo() == Level.NETHER) {
            if (StageManager.advanceStage(level, 3)) {
                RustRelics.LOGGER.info("[R&R] Stage avanzado a 3 (entrada al Nether).");
            }
        }
    }
}
