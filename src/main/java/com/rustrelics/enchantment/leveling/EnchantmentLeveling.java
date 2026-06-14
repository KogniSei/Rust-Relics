package com.rustrelics.enchantment.leveling;

import com.rustrelics.RustRelics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentLeveling {

    private static final int PROGRESS_PER_ACTION = 1;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (sp.getAbilities().instabuild) return;
        addProgressAndLevelUp(sp.getMainHandItem(), sp);
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        // Player recibe daño -> armadura
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getAbilities().instabuild) return;
            for (ItemStack armor : player.getArmorSlots()) {
                addProgressAndLevelUp(armor, player);
            }
        }
        // Player ataca -> arma
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (player.getAbilities().instabuild) return;
            addProgressAndLevelUp(player.getMainHandItem(), player);
        }
    }

    private static void addProgressAndLevelUp(ItemStack stack, ServerPlayer player) {
        if (stack.isEmpty()) return;

        ItemEnchantments oldEnch = stack.get(DataComponents.ENCHANTMENTS);
        if (oldEnch == null || oldEnch.isEmpty()) return;

        EnchantmentProgress currentProg = stack.getOrDefault(
                ModDataComponents.ENCHANTMENT_PROGRESS.get(), EnchantmentProgress.EMPTY);

        Map<ResourceLocation, Integer> newProgress = new HashMap<>();
        Map<Holder<Enchantment>, Integer> newLevels = new HashMap<>();
        boolean anyLevelUp = false;

        for (Holder<Enchantment> holder : oldEnch.keySet()) {
            int currentLevel = oldEnch.getLevel(holder);
            int maxLevel = holder.value().getMaxLevel();

            if (holder.is(EnchantmentTags.CURSE)) continue;
            if (currentLevel >= maxLevel) continue;

            ResourceLocation enchId = holder.getKey().location();
            int progress = currentProg.getProgress(enchId) + PROGRESS_PER_ACTION;
            int targetLevel = currentLevel + 1;

            while (targetLevel <= maxLevel &&
                    progress >= EnchantmentProgress.getThreshold(stack.getMaxDamage(), targetLevel)) {
                progress -= EnchantmentProgress.getThreshold(stack.getMaxDamage(), targetLevel);
                targetLevel++;
            }

            int levelsGained = targetLevel - currentLevel - 1;
            newProgress.put(enchId, progress);
            if (levelsGained > 0) {
                newLevels.put(holder, targetLevel - 1);
                anyLevelUp = true;
            }
        }

        if (anyLevelUp) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(oldEnch);
            for (Map.Entry<Holder<Enchantment>, Integer> entry : newLevels.entrySet()) {
                Holder<Enchantment> ench = entry.getKey();
                int oldLevel = oldEnch.getLevel(ench);
                int newLevel = entry.getValue();
                mutable.set(ench, newLevel);

                ResourceLocation enchLoc = ench.getKey().location();
                Component enchName = Component.translatable("enchantment." + enchLoc.getNamespace() + "." + enchLoc.getPath());
                player.displayClientMessage(
                        Component.literal("§d[").append(enchName)
                                .append(" ")
                                .append(Component.translatable("enchantment.level." + oldLevel))
                                .append(" -> §f")
                                .append(Component.translatable("enchantment.level." + newLevel))
                                .append("§d!]"),
                        true);

                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(
                            ParticleTypes.ENCHANTED_HIT,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            15, 0.5, 0.5, 0.5, 0.1
                    );
                }
            }
            stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        }

        stack.set(ModDataComponents.ENCHANTMENT_PROGRESS.get(),
                new EnchantmentProgress(newProgress));
    }
}
