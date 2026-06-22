package com.rustrelics.boss;

import com.rustrelics.RustRelics;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Escalado de jefes por cantidad de jugadores cercanos.
 *
 * Formula: HP_total = HP_base * (1 + 0.40 * (N - 1))
 * donde N es el numero de jugadores en un radio de 80 bloques.
 *
 * Se ejecuta en LOWEST priority para escalar DESPUES de que
 * BossBuffs y WitherBossBuffs hayan puesto sus HP personalizados.
 *
 * El escalado se aplica como un AttributeModifier PERMANENTE (no muta la base):
 * asi nunca corrompe el HP base del jefe, se guarda con la entidad y, gracias a
 * la guarda {@code getModifier(SCALE_MOD) != null}, NUNCA se re-escala — ni en
 * recarga de chunk ni tras reiniciar el servidor (antes setBaseValue componia el
 * HP en cada arranque si el jefe sobrevivia).
 */
public class BossScaling {

    private static final int SCAN_RADIUS = 80;
    private static final double HP_PER_PLAYER = 0.40;

    private static final ResourceLocation SCALE_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "boss_player_scaling");

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

    private BossScaling() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(
            living.getType()
        );
        if (!BOSS_IDS.contains(id.toString())) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        AttributeInstance healthAttr = living.getAttribute(
            Attributes.MAX_HEALTH
        );
        if (healthAttr == null) return;
        // Ya escalado (el modificador permanente se guarda con la entidad).
        if (healthAttr.getModifier(SCALE_MOD) != null) return;

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
        // ADD_MULTIPLIED_BASE: suma (bonus * base) al HP. bonus = 0.40 * (N - 1).
        double bonus = HP_PER_PLAYER * (playerCount - 1);
        if (bonus <= 0) return; // 1 jugador (o ninguno): sin escalado

        healthAttr.addPermanentModifier(new AttributeModifier(
                SCALE_MOD, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        living.setHealth(living.getMaxHealth());

        RustRelics.LOGGER.info(
            "[R&R] {} escalado: {} jugadores cerca, +{}% HP (x{}).",
            id.getPath(),
            playerCount,
            (int) Math.round(bonus * 100),
            1.0 + bonus
        );
    }

}
