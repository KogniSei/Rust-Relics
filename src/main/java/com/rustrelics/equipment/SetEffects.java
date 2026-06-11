package com.rustrelics.equipment;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Efectos de set NO-atributo, portados de equipment_effects.js a eventos puros.
 * CERO polling por tick — cada efecto reacciona a su propio evento:
 *
 *   - Sterling: vision nocturna mientras se lleva (via cambio de equipo) +
 *               vulnerabilidad al fuego (via dano entrante).
 *   - Silver:   inmunidad a ceguera (via aplicacion de efecto).
 *   - Sanguine/Necromium: maldicion necrotica, sin curacion natural (via heal).
 *
 * Mejora sobre el original: la vision nocturna ya no se "refresca" cada segundo;
 * se aplica con duracion infinita al equipar el set y se retira al quitarlo,
 * distinguiendola de una pocion (que tiene duracion finita) para no pisarla.
 */
public final class SetEffects {

    private static final float FIRE_MULTIPLIER = 1.5f;

    private SetEffects() {
    }

    // --- STERLING: vision nocturna mientras se lleva el set ---
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ArmorSets.isArmorSlot(event.getSlot())) {
            return;
        }

        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean hasOurNightVision = current != null && current.isInfiniteDuration();

        if (ArmorSets.fullSet(player, ArmorSets.STERLING)) {
            if (!hasOurNightVision) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            }
        } else if (hasOurNightVision) {
            // Solo retira la NUESTRA (infinita); no toca una pocion (finita).
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    // --- SANGUINE / NECROMIUM: sin curacion natural ---
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ArmorSets.cursed(player)) {
            return;
        }
        // La Regeneracion (pocion / manzana dorada) SI cura; el resto (comida) no.
        if (player.hasEffect(MobEffects.REGENERATION)) {
            return;
        }
        event.setCanceled(true);
    }

    // --- SILVER: inmunidad a ceguera ---
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!event.getEffectInstance().is(MobEffects.BLINDNESS)) {
            return;
        }
        if (ArmorSets.fullSet(player, ArmorSets.SILVER)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // --- STERLING: vulnerabilidad al fuego ---
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ArmorSets.fullSet(player, ArmorSets.STERLING)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * FIRE_MULTIPLIER);
        }
    }
}
