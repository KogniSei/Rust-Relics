package com.rustrelics.effects;

import com.rustrelics.RustRelics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, RustRelics.MODID);

    public static final DeferredHolder<MobEffect, KarmicRetribution> KARMIC_RETRIBUTION =
        EFFECTS.register("karmic_retribution", KarmicRetribution::new);

    private ModEffects() {}
}
