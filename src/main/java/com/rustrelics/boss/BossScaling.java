package com.rustrelics.boss;

import com.rustrelics.RustRelics;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Escalado de jefes por cantidad de jugadores cercanos.
 *
 * Formula: HP_total = HP_base * (1 + 0.40 * (N - 1))
 * donde N es el numero de jugadores en un radio de 80 bloques.
 *
 * Se ejecuta en LOWEST priority para escalar DESPUES de que
 * BossBuffs y WitherBossBuffs hayan puesto sus HP personalizados.
 */
public class BossScaling {

    private static final int SCAN_RADIUS = 80;
    private static final double HP_PER_PLAYER = 0.40;

    private static final Set<String> BOSS_IDS = new HashSet<>(
        Set.of(
            "galosphere:berserker",
            "minecraft:elder_guardian",
            "minecraft:wither",
            "minecraft:ender_dragon",
            "minecraft:warden",
            // Born in Chaos bosses
            "born_in_chaos_v1:spiritof_chaos",
            "born_in_chaos_v1:supreme_bonescaller",
            "born_in_chaos_v1:lord_pumpkinhead",
            "born_in_chaos_v1:lord_the_headless",
            "born_in_chaos_v1:lifestealer",
            "born_in_chaos_v1:krampus",
            "born_in_chaos_v1:nightmare_stalker"
        )
    );

    // IDs de jefes ya escalados en esta sesion. Previene re-escalado en recarga de chunks.
    private static final Set<UUID> scaledBosses = new HashSet<>();

    private BossScaling() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(
            living.getType()
        );
        if (!BOSS_IDS.contains(id.toString())) return;

        if (!scaledBosses.add(living.getUUID())) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        AttributeInstance healthAttr = living.getAttribute(
            Attributes.MAX_HEALTH
        );
        if (healthAttr == null) return;

        double baseHp = healthAttr.getBaseValue();
        List<ServerPlayer> nearby = level.getEntitiesOfClass(
            ServerPlayer.class,
            AABB.ofSize(
                living.position(),
                SCAN_RADIUS * 2,
                SCAN_RADIUS * 2,
                SCAN_RADIUS * 2
            ),
            p -> !p.isSpectator() && p.isAlive()
        );

        int playerCount = Math.max(nearby.size(), 1);
        double multiplier = 1.0 + HP_PER_PLAYER * (playerCount - 1);
        double scaledHp = baseHp * multiplier;

        healthAttr.setBaseValue(scaledHp);
        living.setHealth((float) scaledHp);

        RustRelics.LOGGER.info(
            "[R&R] {} escalado: {} jugadores cerca, HP base {}/{} -> {} (x{})",
            id.getPath(),
            playerCount,
            baseHp,
            scaledHp,
            multiplier
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Limpieza de UUIDs obsoletos cada 5 minutos para evitar memory leak
        if (event.getServer().getTickCount() % 6000 != 0) return;
        scaledBosses.removeIf(uuid -> {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                if (level.getEntity(uuid) != null) return false;
            }
            return true;
        });
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        scaledBosses.remove(event.getEntity().getUUID());
    }
}
