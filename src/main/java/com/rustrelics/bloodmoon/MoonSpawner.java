package com.rustrelics.bloodmoon;

import com.rustrelics.config.EventConfig;
import com.rustrelics.config.EventConfig.EventCfg;
import com.rustrelics.eclipse.EclipseBuffs;
import com.rustrelics.stage.StageSavedData;
import com.rustrelics.util.SpawnMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Spawner de evento centrado en el jugador, unificado para los tres eventos
 * (Luna Muerta, Luna Pálida, Eclipse Solar).
 *
 * Mientras un evento esta activo, cada {@code interval} ticks (de su {@link EventCfg})
 * genera oleadas de mobs de su pool en un anillo alrededor de cada jugador del
 * overworld, en posiciones validas, con un soft-cap propio. Asi el evento se siente
 * agresivo sin chocar con el mob-cap (spawnea cerca del jugador) ni laguear.
 *
 * Cada mob se marca con {@link SpawnMarker} (ningun sistema de buff lo re-multiplica)
 * y recibe directamente los buffs del evento. Para la Luna Pálida tambien aplica
 * Suerte a los jugadores (su efecto especial).
 *
 * Pools, intensidad y loot se configuran en {@link EventConfig}.
 */
public final class MoonSpawner {

    private static final int CAP_RADIUS = 64;
    private static final int PLACE_TRIES = 6;

    private enum Buff { DEAD, PALE, ECLIPSE }

    private MoonSpawner() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long t = server.getTickCount();

        ServerLevel overworld = server.overworld();
        StageSavedData data = StageSavedData.get(overworld);
        boolean dead = data.getDeadmoon() == 1;
        boolean pale = data.getBloodmoon() == 1 && data.getStage() >= 1;
        boolean eclipse = data.getEclipseTicks() > 0;

        if (dead && t % EventConfig.DEADMOON.interval == 0) {
            runEvent(server, EventConfig.DEADMOON, Buff.DEAD, true);
        }
        if (pale && t % EventConfig.PALEMOON.interval == 0) {
            applyLuck(server);
            runEvent(server, EventConfig.PALEMOON, Buff.PALE, true);
        }
        if (eclipse && t % EventConfig.ECLIPSE.interval == 0) {
            runEvent(server, EventConfig.ECLIPSE, Buff.ECLIPSE, false);
        }
    }

    /** Efecto especial de la Luna Pálida: Suerte para los jugadores del overworld. */
    private static void applyLuck(MinecraftServer server) {
        int duration = EventConfig.PALEMOON.interval + 60;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0, true, false, true));
        }
    }

    private static void runEvent(MinecraftServer server, EventCfg cfg, Buff buff, boolean requireDark) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.isSpectator() || !player.isAlive()) continue;
            spawnWave(player.serverLevel(), player, cfg, buff, requireDark);
        }
    }

    private static void spawnWave(ServerLevel level, ServerPlayer player, EventCfg cfg,
                                  Buff buff, boolean requireDark) {
        List<EntityType<?>> pool = cfg.pool();
        if (pool.isEmpty()) return;

        AABB capBox = player.getBoundingBox().inflate(CAP_RADIUS);
        int existing = level.getEntitiesOfClass(Mob.class, capBox, SpawnMarker::isExtra).size();
        if (existing >= cfg.softCap) return;

        RandomSource rng = level.getRandom();
        int span = Math.max(1, cfg.maxWave - cfg.minWave + 1);
        int wave = Math.min(cfg.minWave + rng.nextInt(span), cfg.softCap - existing);

        for (int i = 0; i < wave; i++) {
            BlockPos pos = findSpawnPos(level, player, cfg, rng, requireDark);
            if (pos == null) continue;

            EntityType<?> type = pool.get(rng.nextInt(pool.size()));
            if (!(type.create(level) instanceof Mob mob)) continue;

            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360f, 0f);
            SpawnMarker.markExtra(mob); // ningun sistema de buff lo multiplicara

            try {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            } catch (Exception ignored) {}

            applyBuff(buff, level, mob);
            level.addFreshEntity(mob);
        }
    }

    private static void applyBuff(Buff buff, ServerLevel level, Mob mob) {
        switch (buff) {
            case DEAD -> DeadMoonBuffs.applyBuffs(mob);
            case PALE -> BloodMoonBuffs.applyBuffs(level, mob);
            case ECLIPSE -> EclipseBuffs.applyBuffs(level, mob);
        }
    }

    /** Busca una posicion valida en el anillo (suelo solido, 2 huecos, sin fluido). */
    private static BlockPos findSpawnPos(ServerLevel level, ServerPlayer player, EventCfg cfg,
                                         RandomSource rng, boolean requireDark) {
        for (int t = 0; t < PLACE_TRIES; t++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = cfg.ringMin + rng.nextDouble() * (cfg.ringMax - cfg.ringMin);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            int baseY = player.getBlockY();

            for (int dy = 4; dy >= -6; dy--) {
                BlockPos p = new BlockPos(x, baseY + dy, z);
                if (isValidSpawn(level, p, requireDark)) return p;
            }
        }
        return null;
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos p, boolean requireDark) {
        if (!level.isLoaded(p)) return false;
        if (!level.getBlockState(p.below()).blocksMotion()) return false; // suelo solido
        if (level.getBlockState(p).blocksMotion()) return false;          // espacio libre
        if (level.getBlockState(p.above()).blocksMotion()) return false;  // 2 de alto
        if (!level.getFluidState(p).isEmpty()) return false;              // no en agua/lava
        // El eclipse es de dia: no exige oscuridad. Las lunas si (evita bases iluminadas).
        return !requireDark || level.getMaxLocalRawBrightness(p) <= 7;
    }
}
