package com.rustrelics.enchantment.leveling.client;

import com.rustrelics.RustRelics;
import com.rustrelics.enchantment.leveling.EnchantmentProgress;
import com.rustrelics.enchantment.leveling.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = RustRelics.MODID, value = Dist.CLIENT)
public class EnchantmentTooltips {

    private static final int BAR_LENGTH = 12;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        EnchantmentProgress prog = stack.getOrDefault(
                ModDataComponents.ENCHANTMENT_PROGRESS.get(), EnchantmentProgress.EMPTY);
        if (prog.isEmpty()) return;

        ItemEnchantments ench = stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (ench.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();

        for (Holder<Enchantment> holder : ench.keySet()) {
            ResourceLocation id = holder.getKey().location();
            int p = prog.getProgress(id);
            if (p <= 0) continue;

            int level = ench.getLevel(holder);
            int maxLevel = holder.value().getMaxLevel();
            if (level >= maxLevel) continue;

            int threshold = EnchantmentProgress.getThreshold(stack.getMaxDamage(), level + 1);
            int filled = Math.min(p * BAR_LENGTH / Math.max(threshold, 1), BAR_LENGTH);

            StringBuilder bar = new StringBuilder(" \u00a77[\u00a7a");
            bar.append("|".repeat(Math.max(0, filled)));
            bar.append("\u00a77");
            bar.append("\u00b7".repeat(Math.max(0, BAR_LENGTH - filled)));
            bar.append("\u00a77] \u00a7e");
            bar.append(p);
            bar.append("\u00a77/\u00a7e");
            bar.append(threshold);

            tooltip.add(Component.literal(bar.toString()));
        }
    }
}
