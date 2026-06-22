package com.rustrelics.bloodmoon;

import com.rustrelics.stage.StageSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Efectos de la Luna Palida sobre los mobs hostiles.
 *
 * Durante la Luna Palida, los mobs hostiles reciben:
 *   - Buff de vida y daño (+50% base, +15% por jugador extra dentro de 64 bloques)
 *   - Aumento de spawn rate (+125% base, +15% por jugador extra dentro de 64 bloques)
 *
 * "Hostil" = tiene el atributo attack_damage (filtro identico al original).
 * Solo afecta mientras la luna palida esta activa y Stage >= 1.
 *
 * Los buffs de stats se aplican como AttributeModifier PERMANENTES (se guardan con
 * la entidad, asi sobreviven la recarga de chunk mientras dura la luna) y se RETIRAN:
 *   - al amanecer, via {@link #revertAll(ServerLevel)} (barrido de BloodMoonManager), y
 *   - de forma perezosa, si un mob buffeado reaparece cuando la luna ya termino.
 * Asi el buff es temporal de verdad (a diferencia del Hardmode, que es permanente).
 */
public final class BloodMoonBuffs {

    private static final String BUFFED_TAG = "rr_bm_buffed";

    private static final ResourceLocation BM_HEALTH_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "bloodmoon_health");
    private static final ResourceLocation BM_DAMAGE_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "bloodmoon_damage");

    private static boolean isBuffed(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(BUFFED_TAG);
    }

    private static void markBuffed(LivingEntity entity) {
        entity.getPersistentData().putBoolean(BUFFED_TAG, true);
    }

    private BloodMoonBuffs() {}

    private static boolean bloodMoonActive(ServerLevel level) {
        StageSavedData data = StageSavedData.get(level);
        return data.getBloodmoon() == 1 && data.getStage() >= 1;
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity.getAttribute(Attributes.ATTACK_DAMAGE) != null;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof ServerPlayer) return;

        // Luna ya terminada: si reaparece un mob marcado en una luna anterior
        // (estaba en un chunk descargado durante el barrido del amanecer), revertir.
        if (!bloodMoonActive(level)) {
            if (isBuffed(living)) removeBuffs(living);
            return;
        }
        if (!isHostile(living)) return;
        if (isBuffed(living)) return;
        markBuffed(living);

        // Contar jugadores dentro de 64 bloques del mob
        long nearby = level.players().stream()
                .filter(p -> p.distanceToSqr(living) <= 64.0 * 64.0)
                .count();
        if (nearby < 1) nearby = 1;

        double scale = 1.0 + (nearby - 1) * 0.15;

        // ADD_MULTIPLIED_BASE suma value*base; para un +50% (escalado) el valor es 0.50*scale.
        double bonus = 0.50 * scale;

        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(BM_HEALTH_MOD) == null) {
            health.addPermanentModifier(new AttributeModifier(
                    BM_HEALTH_MOD, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            living.setHealth(living.getMaxHealth());
        }

        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(BM_DAMAGE_MOD) == null) {
            dmg.addPermanentModifier(new AttributeModifier(
                    BM_DAMAGE_MOD, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }

        // Spawn rate: base +125%, escala +15% por jugador extra
        double extraChance = 1.25 * scale;
        int guaranteed = (int) extraChance;
        double fractional = extraChance - guaranteed;

        for (int i = 0; i < guaranteed; i++) {
            spawnExtra(living, level);
        }
        if (living.getRandom().nextFloat() < fractional) {
            spawnExtra(living, level);
        }
    }

    /** Retira los buffs de luna de un mob y limpia su marca. */
    private static void removeBuffs(LivingEntity living) {
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(BM_HEALTH_MOD);
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(BM_DAMAGE_MOD);
        // Si la vida actual quedo por encima del nuevo maximo, recortar.
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

    private static void spawnExtra(LivingEntity original, ServerLevel level) {
        try {
            LivingEntity extra = (LivingEntity) original.getType().create(level);
            if (extra != null) {
                markBuffed(extra); // corta la recursion: el extra NO debe re-multiplicarse
                extra.moveTo(
                    original.getX() + (original.getRandom().nextDouble() - 0.5) * 6,
                    original.getY(),
                    original.getZ() + (original.getRandom().nextDouble() - 0.5) * 6,
                    original.getYRot(), original.getXRot()
                );
                // Inicializacion de spawn vanilla (AI, equipo, atributos aleatorios).
                if (extra instanceof Mob mob) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                            MobSpawnType.MOB_SUMMONED, null);
                }
                level.addFreshEntity(extra);
            }
        } catch (Exception ignored) {}
    }

    // --- Loot mejorado (solo kills de jugador) ---
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bloodMoonActive(level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) {
            return; // solo si lo mato un jugador
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(
            victim.getType()
        ).toString();
        float roll = victim.getRandom().nextFloat();

        switch (id) {
            case "minecraft:zombie", "minecraft:drowned", "minecraft:husk" -> {
                if (roll < 0.25f) {
                    addDrop(
                        event,
                        victim,
                        level,
                        new ItemStack(Items.IRON_INGOT)
                    );
                }
            }
            case "minecraft:skeleton", "minecraft:stray" -> {
                int count = 2 + victim.getRandom().nextInt(3); // 2-4
                addDrop(
                    event,
                    victim,
                    level,
                    new ItemStack(Items.ARROW, count)
                );
            }
            case "minecraft:pillager" -> {
                if (roll < 0.15f) {
                    addDrop(event, victim, level, new ItemStack(Items.EMERALD));
                }
            }
            case "minecraft:vindicator" -> {
                if (roll < 0.10f) {
                    addDrop(event, victim, level, new ItemStack(Items.EMERALD));
                }
            }
            default -> {
            }
        }
    }

    // --- Bonus de XP: +3 por kill de mob hostil ---
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bloodMoonActive(level) || !isHostile(victim)) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            killer.giveExperiencePoints(3);
        }
    }

    private static void addDrop(
        LivingDropsEvent event,
        LivingEntity victim,
        Level level,
        ItemStack stack
    ) {
        ItemEntity drop = new ItemEntity(
            level,
            victim.getX(),
            victim.getY() + 0.5,
            victim.getZ(),
            stack
        );
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }
}
