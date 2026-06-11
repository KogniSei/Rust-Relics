package com.rustrelics.boss;

import com.rustrelics.RustRelics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class WitherBossBuffs {

    // Stage 4 boss: debe ser un muro. 4 es vanilla, 12 es razonable para netherite.
    private static final double WITHER_ARMOR = 12.0;
    private static final int EFFECT_DURATION = 200;

    private WitherBossBuffs() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof WitherBoss wither)) return;

        AttributeInstance armor = wither.getAttribute(Attributes.ARMOR);
        if (
            armor == null ||
            Math.abs(armor.getBaseValue() - WITHER_ARMOR) < 0.001
        ) return;

        armor.setBaseValue(WITHER_ARMOR);
        RustRelics.LOGGER.info("[R&R] Wither armored: {} armor.", WITHER_ARMOR);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWitherAttack(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        if (target instanceof WitherBoss) return; // NO autodamage

        boolean hitByWither =
            event.getSource().getEntity() instanceof WitherBoss ||
            event.getSource().getDirectEntity() instanceof WitherSkull;
        if (!hitByWither) return;

        // Wither siempre aplica Wither II
        target.addEffect(
            new MobEffectInstance(MobEffects.WITHER, EFFECT_DURATION, 1)
        );

        // Progresión por fase de vida del Wither (requiere obtener el Wither desde source)
        if (event.getSource().getEntity() instanceof WitherBoss wither) {
            float pct = wither.getHealth() / wither.getMaxHealth();

            if (pct < 0.50f) {
                target.addEffect(
                    new MobEffectInstance(MobEffects.POISON, EFFECT_DURATION, 1)
                );
            }
            if (pct < 0.25f) {
                target.addEffect(
                    new MobEffectInstance(
                        MobEffects.WEAKNESS,
                        EFFECT_DURATION,
                        1
                    )
                );
                target.addEffect(
                    new MobEffectInstance(MobEffects.HUNGER, EFFECT_DURATION, 1)
                );
            }
        }
    }
}
