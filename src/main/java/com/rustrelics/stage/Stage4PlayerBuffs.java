package com.rustrelics.stage;

import com.rustrelics.RustRelics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Aplica buffs permanentes al jugador cuando el Stage alcanza 4.
 *
 * Stage 4+ otorga:
 *   - +20% vida maxima (sobre base, ej. +4 HP para un jugador vanilla sin armadura)
 *   - +1.5 de dano de ataque (bonificacion base plana)
 *
 * Se aplica al: entrar al mundo, respawnear, y cuando el stage sube a 4
 * mientras el servidor esta corriendo (via {@link #applyToAll(ServerLevel)}).
 * Es idempotente por UUID de modifier — si ya existe, no se duplica.
 */
public final class Stage4PlayerBuffs {

    private static final int REQUIRED_STAGE = 4;

    private static final ResourceLocation HP_MOD_ID = ResourceLocation.parse(
        "rustrelics:stage4_hp"
    );
    private static final ResourceLocation DMG_MOD_ID = ResourceLocation.parse(
        "rustrelics:stage4_dmg"
    );

    /** +20% de la vida base (20 → 24 HP). */
    private static final double HP_MULTIPLIER = 0.20;
    private static final double DMG_FLAT = 0.5;

    private Stage4PlayerBuffs() {}

    public static void applyToAll(ServerLevel level) {
        if (StageManager.getStage(level) < REQUIRED_STAGE) return;
        level
            .getServer()
            .getPlayerList()
            .getPlayers()
            .forEach(Stage4PlayerBuffs::apply);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (StageManager.getStage(level) >= REQUIRED_STAGE) {
            apply(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (StageManager.getStage(level) >= REQUIRED_STAGE) {
            apply(player);
        }
    }

    private static void apply(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            if (health.getModifier(HP_MOD_ID) == null) {
                health.addTransientModifier(
                    new AttributeModifier(
                        HP_MOD_ID,
                        HP_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
                );
            }
        }

        AttributeInstance dmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            if (dmg.getModifier(DMG_MOD_ID) == null) {
                dmg.addTransientModifier(
                    new AttributeModifier(
                        DMG_MOD_ID,
                        DMG_FLAT,
                        AttributeModifier.Operation.ADD_VALUE
                    )
                );
            }
        }
    }
}
