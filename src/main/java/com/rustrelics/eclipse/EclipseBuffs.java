package com.rustrelics.eclipse;

import com.rustrelics.stage.StageSavedData;
import com.rustrelics.util.SpawnMarker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Buffs de los enemigos durante el Eclipse Solar: +15% daño y +15% velocidad,
 * SIN vida extra (el eclipse es ofensivo, no esponjoso).
 *
 * Mismo patrón que {@link com.rustrelics.bloodmoon.BloodMoonBuffs}: modificadores
 * permanentes retirables, aplicados al spawn (a enemigos) y revertidos al terminar
 * el eclipse ({@link #revertAll}) o de forma perezosa al recargar el mob.
 *
 * Objetivo: {@link Enemy} (monstruos e illagers), no jugadores/animales/aldeanos.
 */
public final class EclipseBuffs {

    private static final String BUFFED_TAG = "rr_eclipse_buffed";
    private static final double DAMAGE_BONUS = 0.15;
    private static final double SPEED_BONUS = 0.15;

    private static final ResourceLocation ECLIPSE_DMG =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "eclipse_damage");
    private static final ResourceLocation ECLIPSE_SPEED =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "eclipse_speed");

    private EclipseBuffs() {}

    public static boolean eclipseActive(ServerLevel level) {
        return StageSavedData.get(level).getEclipseTicks() > 0;
    }

    private static boolean isBuffed(LivingEntity e) {
        return e.getPersistentData().getBoolean(BUFFED_TAG);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        // Mob extra del spawner: este aplica el buff directamente, no aqui.
        if (SpawnMarker.isExtra(living)) return;

        if (!eclipseActive(level)) {
            if (isBuffed(living)) removeBuffs(living);
            return;
        }
        if (!(living instanceof Enemy)) return;
        if (isBuffed(living)) return;
        applyBuffs(level, living);
    }

    /** Aplica +15% daño y +15% velocidad (idempotente). Usado por onEntityJoin y el spawner. */
    public static void applyBuffs(ServerLevel level, LivingEntity living) {
        living.getPersistentData().putBoolean(BUFFED_TAG, true);

        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(ECLIPSE_DMG) == null) {
            dmg.addPermanentModifier(new AttributeModifier(
                    ECLIPSE_DMG, DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(ECLIPSE_SPEED) == null) {
            speed.addPermanentModifier(new AttributeModifier(
                    ECLIPSE_SPEED, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void removeBuffs(LivingEntity living) {
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(ECLIPSE_DMG);
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(ECLIPSE_SPEED);
        living.getPersistentData().putBoolean(BUFFED_TAG, false);
    }

    /** Barrido al terminar el eclipse: revierte los buffs de todos los mobs cargados. */
    public static void revertAll(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && isBuffed(living)) {
                removeBuffs(living);
            }
        }
    }
}
