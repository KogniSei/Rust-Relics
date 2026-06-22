package com.rustrelics.bloodmoon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Efectos de la Luna Muerta sobre los mobs hostiles.
 *
 * Durante la luna muerta, los mobs hostiles reciben:
 *   - +11% vida y +11% daño (AttributeModifier retirable, no corrompe la base)
 *   - 2 mobs extra al aparecer (spawn rate +200%)
 *
 * Como en la luna pálida, los buffs de stats son temporales: se aplican como
 * modificadores PERMANENTES (se guardan con la entidad para sobrevivir la recarga
 * de chunk) y se RETIRAN al amanecer (via {@link #revertAll}) o de forma perezosa
 * si el mob reaparece cuando la luna ya termino.
 */
public final class DeadMoonBuffs {

    private static final String BUFFED_TAG = "rr_dm_buffed";
    private static final double HEALTH_MULT = 1.11;
    private static final double DAMAGE_MULT = 1.11;

    private static final ResourceLocation DM_HEALTH_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "deadmoon_health");
    private static final ResourceLocation DM_DAMAGE_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "deadmoon_damage");

    private static boolean isBuffed(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(BUFFED_TAG);
    }

    private static void markBuffed(LivingEntity entity) {
        entity.getPersistentData().putBoolean(BUFFED_TAG, true);
    }

    private DeadMoonBuffs() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof ServerPlayer) return;

        // Luna ya terminada: revertir un mob marcado en una luna anterior.
        if (!DeadMoonManager.isActive(level)) {
            if (isBuffed(living)) removeBuffs(living);
            return;
        }

        boolean hostile = living.getAttribute(Attributes.ATTACK_DAMAGE) != null;
        if (!hostile) return;
        if (isBuffed(living)) return;
        markBuffed(living);

        // --- Buff de stats (ADD_MULTIPLIED_BASE: value = mult - 1) ---
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(DM_HEALTH_MOD) == null) {
            health.addPermanentModifier(new AttributeModifier(
                    DM_HEALTH_MOD, HEALTH_MULT - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            living.setHealth(living.getMaxHealth());
        }
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(DM_DAMAGE_MOD) == null) {
            dmg.addPermanentModifier(new AttributeModifier(
                    DM_DAMAGE_MOD, DAMAGE_MULT - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }

        // --- Spawn rate +200%: 2 mobs extra ---
        for (int i = 0; i < 2; i++) {
            try {
                LivingEntity extra = (LivingEntity) living.getType().create(level);
                if (extra != null) {
                    markBuffed(extra); // corta la recursion: el extra NO debe re-multiplicarse
                    extra.moveTo(
                        living.getX() + (living.getRandom().nextDouble() - 0.5) * 6,
                        living.getY(),
                        living.getZ() + (living.getRandom().nextDouble() - 0.5) * 6,
                        living.getYRot(), living.getXRot()
                    );
                    if (extra instanceof Mob mob) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                                MobSpawnType.MOB_SUMMONED, null);
                    }
                    level.addFreshEntity(extra);
                }
            } catch (Exception ignored) {}
        }
    }

    /** Retira los buffs de luna muerta de un mob y limpia su marca. */
    private static void removeBuffs(LivingEntity living) {
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(DM_HEALTH_MOD);
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(DM_DAMAGE_MOD);
        if (living.getHealth() > living.getMaxHealth()) {
            living.setHealth(living.getMaxHealth());
        }
        living.getPersistentData().putBoolean(BUFFED_TAG, false);
    }

    /** Barrido al amanecer: revierte los buffs de todos los mobs cargados de un nivel. */
    public static void revertAll(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && isBuffed(living)) {
                removeBuffs(living);
            }
        }
    }
}
