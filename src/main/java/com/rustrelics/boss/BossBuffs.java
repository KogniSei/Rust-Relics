package com.rustrelics.boss;

import com.rustrelics.RustRelics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Buffs de boss al aparecer. Equivalente nativo de la rama spawn de stage_events.js.
 *
 * Galosphere Berserker: boss memorable de Stage 0. 695 HP (base vanilla ~155) +
 * 12 puntos de armadura. Referenciado por ResourceLocation, soft-dep safe.
 *
 * EntityJoinLevelEvent dispara en cada carga al nivel (incl. recarga de chunk),
 * asi que se evita re-buffear comprobando si la salud base ya es la objetivo.
 */
public final class BossBuffs {

    private static final String BERSERKER_ID = "galosphere:berserker";
    private static final double BERSERKER_HP = 695.0;
    private static final double BERSERKER_ARMOR = 12.0;

    private BossBuffs() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (!BERSERKER_ID.equals(id.toString())) {
            return;
        }

        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || health.getBaseValue() == BERSERKER_HP) {
            return; // ya buffeado (recarga de chunk) o sin atributo
        }

        health.setBaseValue(BERSERKER_HP);
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(BERSERKER_ARMOR);
        }
        living.setHealth((float) BERSERKER_HP);

        RustRelics.LOGGER.info("[R&R] Berserker buffeado: {} HP, {} armor.", BERSERKER_HP, BERSERKER_ARMOR);
    }
}
