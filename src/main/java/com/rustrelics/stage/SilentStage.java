package com.rustrelics.stage;

import com.rustrelics.util.Scoreboards;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Calcula un "Stage Silencioso" por jugador basado en su maxHP actual
 * y el mejor material de armadura/herramientas que lleva puesto.
 *
 * El score se escribe al scoreboard {@code rr_silent_stage} por jugador
 * para que InControl, datapacks y otros sistemas lean este valor
 * dinamicamente y escalen la dificultad de la invasion Illager.
 *
 * Formula:
 *   base = floor(player.getMaxHealth() / 10)
 *   tier = materialTier(mejor pieza equipada)
 *   silentStage = clamp(base + tier, 0, 5)
 *
 * Se recalcula cada vez que el jugador cambia de equipo o entra al mundo.
 */
public final class SilentStage {

    private static final String SCOREBOARD_OBJECTIVE = "rr_silent_stage";

    private SilentStage() {}

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

    private static void recalculate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float hp = (float) player.getAttributeValue(Attributes.MAX_HEALTH);

        int bestTier = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR
                    && slot != EquipmentSlot.MAINHAND) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            int t = materialTier(stack);
            if (t > bestTier) bestTier = t;
        }

        int base = (int) Math.floor(hp / 10f);
        int silent = Math.min(5, base + bestTier);
        if (silent < 0) silent = 0;

        writeScoreboard(level, player, silent);
    }

    /** Escribe rr_silent_stage para el jugador. */
    private static void writeScoreboard(ServerLevel level, ServerPlayer player, int value) {
        Scoreboards.set(level.getServer(), SCOREBOARD_OBJECTIVE, "Silent Stage",
                player.getScoreboardName(), value);
    }

    /**
     * Asigna un tier numerico a un item basado en su material.
     * Los valores coinciden con el GDD general de Rust & Relics.
     */
    private static int materialTier(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();

        // Tier 0
        if (path.contains("wooden") || path.contains("leather") || path.contains("stone")) return 0;
        // Tier 1
        if (path.contains("copper") || path.contains("chainmail")) return 1;
        // Tier 2
        if (path.contains("iron") || path.contains("chain")) return 2;
        // Tier 3
        if (path.contains("golden") || path.contains("gold")) return 3;
        // Tier 4
        if (path.contains("diamond") || path.contains("silver") || path.contains("sterling")) return 4;
        // Tier 5
        if (path.contains("netherite")) return 5;
        // Tier 6
        if (path.contains("sanguine")) return 6;
        if (path.contains("necromium")) return 6;

        // Default: tier basado en defensa si es armadura
        return 0;
    }
}
