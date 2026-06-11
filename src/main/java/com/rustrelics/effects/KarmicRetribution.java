package com.rustrelics.effects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class KarmicRetribution extends MobEffect {

    private static final double DMG_PER_TICK = 0.5;
    private static final double DMG_PER_TICK_ACCELERATED = 1.5;
    private static final int TICK_INTERVAL = 40;

    private static final ResourceLocation ARMOR_MOD_ID =
        ResourceLocation.parse("rustrelics:karmic_armor_pen");

    protected KarmicRetribution() {
        super(MobEffectCategory.HARMFUL, 0xAA00AA);
        addAttributeModifier(
            Attributes.ARMOR,
            ARMOR_MOD_ID,
            -3.0,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return false;
        double dmg = (amplifier >= 1) ? DMG_PER_TICK_ACCELERATED : DMG_PER_TICK;
        entity.hurt(entity.damageSources().magic(), (float) dmg);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
