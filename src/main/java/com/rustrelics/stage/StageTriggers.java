package com.rustrelics.stage;

import com.rustrelics.RustRelics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

        // Recompensa al matar al Elder Guardian: x2 vida y daño
        if ("minecraft:elder_guardian".equals(idStr)) {
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                awardGuardianBlessing(killer);
            }
        }
    }

    private static final ResourceLocation GUARDIAN_HP_MOD = ResourceLocation.parse(
        "rustrelics:guardian_hp"
    );
    private static final ResourceLocation GUARDIAN_DMG_MOD = ResourceLocation.parse(
        "rustrelics:guardian_dmg"
    );

    private static void awardGuardianBlessing(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(GUARDIAN_HP_MOD) == null) {
            health.addTransientModifier(
                new AttributeModifier(GUARDIAN_HP_MOD, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            );
        }
        AttributeInstance dmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(GUARDIAN_DMG_MOD) == null) {
            dmg.addTransientModifier(
                new AttributeModifier(GUARDIAN_DMG_MOD, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            );
        }
        player.sendSystemMessage(
            Component.literal("§b[Rust & Relics] §fBendición del Guardián: §e§lx1.5 §fvida y daño.")
        );
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
