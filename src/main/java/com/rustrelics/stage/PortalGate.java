package com.rustrelics.stage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Bloqueo del Nether hasta Stage 2 (tras Elder Guardian). Porta
 * stage_portal_control.js, pero sin el polling por tick del original.
 *
 *   Capa 1 (encendido): cancela mechero/fire_charge sobre obsidiana si Stage < 2.
 *   Capa 2 (cruce):      cancela el viaje al Nether si Stage < 2 — via
 *                        EntityTravelToDimensionEvent, que dispara exacto al
 *                        cruzar, en vez de empujar al jugador cada 10 ticks.
 */
public final class PortalGate {

    private PortalGate() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.OBSIDIAN)) {
            return;
        }
        ItemStack item = event.getItemStack();
        boolean igniter = item.is(Items.FLINT_AND_STEEL) || item.is(Items.FIRE_CHARGE);
        if (!igniter) {
            return;
        }
        if (StageManager.getStage(player.serverLevel()) >= 2) {
            return;
        }

        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(
                "§c[Rust & Relics] §fUna barrera magica apaga la chispa. El Nether esta sellado."));
        player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        int stage = StageManager.getStage(player.serverLevel());

        // Nether desbloqueado en Stage 2 (Elder Guardian); End en Stage 4 (Wither).
        if (event.getDimension() == Level.NETHER && stage < 2) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(
                    "§c[!] El portal te escupe. Debes llegar al Stage 2 primero."));
        } else if (event.getDimension() == Level.END && stage < 4) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(
                    "§5[!] El vacio te rechaza. El End permanece sellado hasta el Stage 4."));
        }
    }
}
