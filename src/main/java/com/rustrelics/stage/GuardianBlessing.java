package com.rustrelics.stage;

import com.rustrelics.attachment.ModAttachments;
import com.rustrelics.silent.HealthTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * "Stage silencioso 2": bendicion permanente del Elder Guardian.
 *
 * Cuando un jugador mata a un Elder Guardian se marca el attachment persistente
 * {@link ModAttachments#GUARDIAN_BLESSED} y se le aplica +35% de vida maxima.
 *
 * El buff se aplica como modificador TRANSITORIO y se RE-APLICA en cada login y
 * respawn (igual que {@link Stage4PlayerBuffs}) — el flag persistente es la fuente
 * de verdad. Esto lo hace fiable: aunque el modificador se pierda por un reinicio,
 * un reset de atributos de otro mod, o el orden de eventos en la muerte, vuelve a
 * ponerse en cuanto el jugador entra/revive. (El enfoque anterior lo aplicaba una
 * sola vez en el instante de la muerte y, si no "pegaba", se perdia para siempre.)
 */
public final class GuardianBlessing {

    private static final String ELDER_GUARDIAN_ID = "minecraft:elder_guardian";
    private static final ResourceLocation HP_MOD_ID =
            ResourceLocation.fromNamespaceAndPath("rustrelics", "guardian_hp");
    private static final double HP_MULTIPLIER = 0.35; // +35% vida maxima

    private GuardianBlessing() {}

    @SubscribeEvent
    public static void onElderGuardianDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        if (!ELDER_GUARDIAN_ID.equals(id)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        boolean first = !player.getData(ModAttachments.GUARDIAN_BLESSED);
        player.setData(ModAttachments.GUARDIAN_BLESSED, true);
        apply(player);
        if (first) {
            player.sendSystemMessage(Component.literal(
                    "§b[Rust & Relics] §fBendición del Guardián: §e§l+35% §fvida máxima."));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.GUARDIAN_BLESSED)) {
            apply(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.GUARDIAN_BLESSED)) {
            apply(player);
        }
    }

    /** Aplica el +35% de vida (idempotente por id de modificador). */
    private static void apply(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HP_MOD_ID) == null) {
            health.addTransientModifier(new AttributeModifier(
                    HP_MOD_ID, HP_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        HealthTracker.recalculate(player);
    }
}
