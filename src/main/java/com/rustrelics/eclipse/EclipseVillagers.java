package com.rustrelics.eclipse;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * "Los aldeanos se esconden" durante el Eclipse Solar.
 *
 *   - Sweep cada {@link #INTERVAL} ticks: aplica Invisibilidad (refrescada) a los
 *     aldeanos del overworld mientras el eclipse esta activo.
 *   - {@link LivingChangeTargetEvent}: si el eclipse esta activo, ningun mob puede
 *     fijar a un aldeano como objetivo (se cancela el cambio de objetivo).
 */
public final class EclipseVillagers {

    private static final int INTERVAL = 40;
    private static final int INVIS_DURATION = 80; // > INTERVAL para no parpadear

    private EclipseVillagers() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) return;

        ServerLevel overworld = server.overworld();
        if (!EclipseBuffs.eclipseActive(overworld)) return;

        for (Entity entity : overworld.getAllEntities()) {
            if (entity instanceof Villager villager) {
                villager.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, INVIS_DURATION, 0, true, false, false));
            }
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Villager)) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (EclipseBuffs.eclipseActive(level)) {
            event.setCanceled(true); // aldeano intargeteable durante el eclipse
        }
    }
}
