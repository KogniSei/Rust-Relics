package com.rustrelics.stage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Bendicion silenciosa del Warden. Mientras {@code wardenSlain} sea true, todos
 * los efectos NEGATIVOS sobre cualquier entidad llegan debilitados:
 *   - duracion: -30%
 *   - efectividad (amplificador): -30%, redondeado por nivel
 *
 * Se intercepta en {@link MobEffectEvent.Applicable}: se deniega el efecto
 * original y se re-aplica una version reducida. La guarda {@link #REDUCING}
 * evita la recursion infinita (el re-aplicado vuelve a disparar Applicable).
 */
public final class WardenBlessing {

    private static final double DURATION_REDUCTION = 0.3;
    private static final double EFFECTIVENESS_REDUCTION = 0.3;
    private static final Set<UUID> REDUCING = new HashSet<>();

    private WardenBlessing() {}

    /**
     * Amplificador reducido ~30%. Trabaja sobre el "nivel" (amp+1) para que el
     * redondeo sea natural: nivel III (amp 2) -> nivel II (amp 1), etc. Nunca
     * baja de nivel I (amp 0) — un efecto negativo de nivel I no se puede
     * debilitar mas sin eliminarlo, y de eso ya se encarga el recorte de duracion.
     */
    private static int reduceAmplifier(int amplifier) {
        int level = amplifier + 1;
        int reducedLevel = (int) Math.round(level * (1.0 - EFFECTIVENESS_REDUCTION));
        return Math.max(0, reducedLevel - 1);
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!StageSavedData.get(level).isWardenSlain()) return;

        MobEffectInstance effect = event.getEffectInstance();
        if (effect.getEffect().value().isBeneficial()) return;

        LivingEntity entity = event.getEntity();
        if (REDUCING.contains(entity.getUUID())) return;

        REDUCING.add(entity.getUUID());
        try {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            int reducedDuration = (int) (effect.getDuration() * (1.0 - DURATION_REDUCTION));
            if (reducedDuration <= 0) return;
            entity.addEffect(new MobEffectInstance(
                effect.getEffect(),
                reducedDuration,
                reduceAmplifier(effect.getAmplifier()),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
            ));
        } finally {
            REDUCING.remove(entity.getUUID());
        }
    }
}
