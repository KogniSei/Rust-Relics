package com.rustrelics.bloodmoon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class DeadMoonBuffs {

    private static final String BUFFED_TAG = "rr_dm_buffed";
    private static final double HEALTH_MULT = 1.11;
    private static final double DAMAGE_MULT = 1.11;

    private static boolean isBuffed(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(BUFFED_TAG);
    }

    private static void markBuffed(LivingEntity entity) {
        entity.getPersistentData().putBoolean(BUFFED_TAG, true);
    }

    private DeadMoonBuffs() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof ServerPlayer) return;
        if (!DeadMoonManager.isActive(level)) return;
        if (isBuffed(living)) return;

        boolean hostile = living.getAttribute(Attributes.ATTACK_DAMAGE) != null;
        if (!hostile) return;

        markBuffed(living);

        // 200% spawn rate increase = spawn 2 extra mobs
        for (int i = 0; i < 2; i++) {
            try {
                LivingEntity extra = (LivingEntity) living.getType().create(level);
                if (extra != null) {
                    extra.moveTo(
                        living.getX() + (living.getRandom().nextDouble() - 0.5) * 6,
                        living.getY(),
                        living.getZ() + (living.getRandom().nextDouble() - 0.5) * 6,
                        living.getYRot(), living.getXRot()
                    );
                    level.addFreshEntity(extra);
                }
            } catch (Exception ignored) {}
        }
    }
}
