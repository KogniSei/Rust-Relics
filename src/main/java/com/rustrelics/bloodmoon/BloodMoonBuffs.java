package com.rustrelics.bloodmoon;

import com.rustrelics.stage.StageSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
 * Efectos de la Luna de Sangre sobre los mobs. Porta las ramas de spawn, loot y
 * XP de blood_moon.js. El loot, que en KubeJS usaba LootJS, aqui se inyecta por
 * LivingDropsEvent (event-driven, sin datapack adicional).
 *
 * Solo afecta mientras la luna esta activa y Stage >= 1. "Hostil" = tiene el
 * atributo attack_damage (filtro identico al original).
 */
public final class BloodMoonBuffs {

    private static final String BUFFED_TAG = "rr_bm_buffed";

    private BloodMoonBuffs() {}

    private static boolean bloodMoonActive(ServerLevel level) {
        StageSavedData data = StageSavedData.get(level);
        return data.getBloodmoon() == 1 && data.getStage() >= 1;
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity.getAttribute(Attributes.ATTACK_DAMAGE) != null;
    }

    // --- Buff al aparecer: +495% HP, +283% dano ---
    // Valores altos intencionales: el equipo de diamante hace que los mobs
    // sean triviales, la Blood Moon debe sentirse como una amenaza real.
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!bloodMoonActive(level) || !isHostile(living)) {
            return;
        }
        if (living.getTags().contains(BUFFED_TAG)) {
            return; // ya buffeado (evita compuesto en recarga de chunk)
        }
        living.addTag(BUFFED_TAG);

        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * 5.95);
            living.setHealth(living.getMaxHealth());
        }
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.setBaseValue(dmg.getBaseValue() * 3.83);
        }
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
