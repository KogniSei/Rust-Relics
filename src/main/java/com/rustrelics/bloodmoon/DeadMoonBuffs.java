package com.rustrelics.bloodmoon;

import com.rustrelics.config.EventConfig;
import com.rustrelics.util.SpawnMarker;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

import java.util.List;

/**
 * Efectos de la Luna Muerta sobre los mobs hostiles y el jugador.
 *
 * Mobs hostiles (al spawn natural; la CANTIDAD la maneja {@link MoonSpawner}):
 *   - +11% vida
 *   - +rango de deteccion (FOLLOW_RANGE): te detectan desde mas lejos
 *   - NO reciben +daño (cambio de diseño respecto a versiones previas)
 *
 * Efecto especial: la regeneracion del jugador se reduce 50% (toda curacion).
 * Loot: mayor probabilidad de libros encantados al matar (config).
 *
 * Los buffs de stat son temporales: AttributeModifier permanentes retirables, que
 * se RETIRAN al amanecer ({@link #revertAll}) o de forma perezosa al recargar.
 */
public final class DeadMoonBuffs {

    private static final String BUFFED_TAG = "rr_dm_buffed";
    private static final double HEALTH_MULT = 1.11;   // +11% vida
    private static final double FOLLOW_BONUS = 0.50;  // +50% rango de deteccion
    private static final float REGEN_FACTOR = 0.5f;   // -50% curacion del jugador

    private static final ResourceLocation DM_HEALTH_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "deadmoon_health");
    private static final ResourceLocation DM_FOLLOW_MOD =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "deadmoon_follow");
    /** Legacy (versiones previas aplicaban +daño): se retira para limpiar mobs antiguos. */
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
        if (SpawnMarker.isExtra(living)) return;

        // Luna ya terminada: revertir un mob marcado en una luna anterior.
        if (!DeadMoonManager.isActive(level)) {
            if (isBuffed(living)) removeBuffs(living);
            return;
        }

        boolean hostile = living.getAttribute(Attributes.ATTACK_DAMAGE) != null;
        if (!hostile) return;
        if (isBuffed(living)) return;
        applyBuffs(living);
    }

    /**
     * Aplica los buffs de stat de la luna muerta (idempotente): +vida y +rango de
     * deteccion, SIN daño. Lo usan onEntityJoin y el spawner ({@link MoonSpawner}).
     */
    public static void applyBuffs(LivingEntity living) {
        markBuffed(living);
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(DM_HEALTH_MOD) == null) {
            health.addPermanentModifier(new AttributeModifier(
                    DM_HEALTH_MOD, HEALTH_MULT - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            living.setHealth(living.getMaxHealth());
        }
        AttributeInstance follow = living.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null && follow.getModifier(DM_FOLLOW_MOD) == null) {
            follow.addPermanentModifier(new AttributeModifier(
                    DM_FOLLOW_MOD, FOLLOW_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Retira los buffs de luna muerta de un mob (incl. el +daño legacy) y limpia su marca. */
    private static void removeBuffs(LivingEntity living) {
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(DM_HEALTH_MOD);
        AttributeInstance follow = living.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.removeModifier(DM_FOLLOW_MOD);
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.removeModifier(DM_DAMAGE_MOD); // limpieza legacy
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

    // --- Efecto especial: regeneracion del jugador -50% ---
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!DeadMoonManager.isActive(level)) return;
        event.setAmount(event.getAmount() * REGEN_FACTOR);
    }

    // --- Loot: libros encantados (solo kills de jugador) ---
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (!DeadMoonManager.isActive(level)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        if (victim.getRandom().nextFloat() >= EventConfig.DEADMOON.bookChance) return;

        ItemStack book = randomEnchantedBook(level, victim.getRandom());
        if (book.isEmpty()) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.5, victim.getZ(), book);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private static ItemStack randomEnchantedBook(ServerLevel level, RandomSource rng) {
        List<Holder.Reference<Enchantment>> all =
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList();
        if (all.isEmpty()) return ItemStack.EMPTY;
        Holder<Enchantment> ench = all.get(rng.nextInt(all.size()));
        int max = Math.max(1, ench.value().getMaxLevel());
        int lvl = 1 + rng.nextInt(max);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(ench, lvl);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return book;
    }
}
