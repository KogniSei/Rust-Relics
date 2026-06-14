package com.rustrelics.enchantment.leveling;

import com.rustrelics.RustRelics;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RustRelics.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnchantmentProgress>> ENCHANTMENT_PROGRESS =
            COMPONENTS.register("enchantment_progress", () ->
                    DataComponentType.<EnchantmentProgress>builder()
                            .persistent(EnchantmentProgress.CODEC)
                            .networkSynchronized(EnchantmentProgress.STREAM_CODEC)
                            .build()
            );

    private ModDataComponents() {}
}
