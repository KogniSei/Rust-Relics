package com.rustrelics.effects;

import com.rustrelics.attachment.ModAttachments;
import com.rustrelics.stage.StageManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class KarmicRetributionHandler {

    private static final int KR_DURATION = 200;
    private static final int KR_AMPLIFIER_BASE = 0;
    private static final int KR_AMPLIFIER_HIT = 1;
    private static final int MAX_AMPLIFIER = 10;

    private KarmicRetributionHandler() {}

    @SubscribeEvent
    public static void onDragonProjectile(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile instanceof DragonFireball)) return;
        if (!(projectile.level() instanceof ServerLevel level)) return;
        if (StageManager.getStage(level) < 4) return;

        if (event.getRayTraceResult() instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof ServerPlayer player) {
                int amp =
                    StageManager.getStage(level) >= 5
                        ? KR_AMPLIFIER_HIT
                        : KR_AMPLIFIER_BASE;
                player.addEffect(
                    new MobEffectInstance(
                        ModEffects.KARMIC_RETRIBUTION,
                        KR_DURATION,
                        amp,
                        false,
                        true,
                        true
                    )
                );
            }
        }
    }

    @SubscribeEvent
    public static void onMobDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (StageManager.getStage(level) < 5) return;

        if (!(event.getSource().getDirectEntity() instanceof LivingEntity)) return;

        if (event.getSource().getEntity() instanceof ServerPlayer) return;

        int villagerKills = player.getData(ModAttachments.VILLAGER_KILLS);
        if (villagerKills <= 0) return;

        MobEffectInstance existing = player.getEffect(
            ModEffects.KARMIC_RETRIBUTION
        );

        int villagerTier = Math.min(villagerKills / 10, 5);
        int amp = villagerTier;

        if (existing != null) {
            amp = Math.min(existing.getAmplifier() + 1, MAX_AMPLIFIER);
        }

        player.addEffect(
            new MobEffectInstance(
                ModEffects.KARMIC_RETRIBUTION,
                existing != null
                    ? existing.getDuration() + KR_DURATION
                    : KR_DURATION,
                amp,
                false,
                true,
                true
            )
        );
    }
}
