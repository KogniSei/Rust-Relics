package com.rustrelics.eclipse;

import com.rustrelics.stage.StageSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Efectos del Eclipse Solar sobre los mobs.
 *
 * Solo corre cuando el eclipse esta activo (eclipseTicks > 0), asi que el coste
 * en estado normal es un solo chequeo de int. Restringido al overworld: el
 * eclipse es un fenomeno de la superficie.
 *
 * Mientras dura el eclipse, en un unico barrido cada {@link #INTERVAL} ticks:
 *   - apaga el fuego de cualquier mob (los hostiles no se queman con el sol), y
 *   - buffa a los mobs con capacidad de ataque con Fuerza + Velocidad.
 *
 * Los buffs son MobEffects (no AttributeModifiers): no tocan los atributos base
 * (no corrompen nada) y caducan solos. Se re-aplican con duracion corta cada
 * barrido, asi cubren tambien a los mobs que ya estaban vivos al empezar el
 * eclipse y desaparecen ~{@link #BUFF_DURATION} ticks despues de que termine.
 *
 * Itera directamente las entidades cargadas del nivel en vez de pedir
 * getEntitiesOfClass sobre el AABB del world border (que por defecto abarca
 * ~60M de bloques).
 */
public final class EclipseMobEffects {

    private static final int INTERVAL = 20;
    private static final int BUFF_DURATION = 40; // > INTERVAL para no parpadear entre barridos

    private EclipseMobEffects() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) return;

        ServerLevel overworld = server.overworld();
        if (StageSavedData.get(overworld).getEclipseTicks() <= 0) return;

        for (Entity entity : overworld.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;

            if (mob.isOnFire()) {
                mob.clearFire();
            }

            // Buff de combate a mobs capaces de atacar (filtro: tiene attack_damage).
            if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                mob.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST, BUFF_DURATION, 0, true, false, false));
                mob.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, BUFF_DURATION, 0, true, false, false));
            }
        }
    }
}
