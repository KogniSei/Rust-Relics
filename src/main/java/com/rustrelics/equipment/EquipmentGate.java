package com.rustrelics.equipment;

import com.rustrelics.stage.StageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * Gating de equipo por stage. ESTE ES EL NUCLEO PERF-CRITICO del porteo.
 *
 * Reemplaza por completo el polling por tick de tres scripts KubeJS:
 *   - stage_armor_control.js  (armadura, cada 20 ticks)
 *   - equipment_lock.js       (herramientas de diamante, cada 40 ticks)
 *   - la deteccion de equipo de equipment_effects.js
 *
 * En vez de preguntar "¿que llevas puesto?" cada segundo por jugador, este
 * handler dispara UNA sola vez, en el instante exacto en que un slot cambia.
 *
 * Reglas (identicas al comportamiento KubeJS original):
 *   Stage < 1: bloquea armadura de hierro y diamante + herramientas de diamante.
 *   Stage < 4: bloquea armadura de netherite.
 *   Stage < 5: bloquea armadura Sanguine y Necromium (C&C) — solo Endgame.
 *   Stage 5+:  sin restricciones.
 */
public final class EquipmentGate {

    private static final String MSG_DIAMOND =
            "§c[Rust & Relics] §fEl diamante industrial aun no ha despertado. Derrota al Berserk primero.";
    private static final String MSG_NETHERITE =
            "§4[!] El calor abrasador de este material te quema. (Requiere Stage 4)";
    private static final String MSG_ENDGAME =
            "§5[!] Este poder oscuro solo obedece a quien vencio al Ender Dragon. (Requiere Endgame)";

    private EquipmentGate() {
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        // Solo jugadores reales en servidor; ignora mobs y lado cliente.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getTo();
        if (stack.isEmpty()) {
            return;
        }

        // Ignorar cambios del MISMO item (durabilidad/cantidad): LivingEquipmentChangeEvent
        // dispara cada vez que un pico/espada pierde durabilidad al usarse, lo que spammeaba
        // el mensaje en cada golpe. Solo actuamos en un re-equipamiento real (cambia el TIPO).
        if (ItemStack.isSameItem(event.getFrom(), stack)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int stage = StageManager.getStage(level);
        if (stage >= 5) {
            return; // Endgame: todo desbloqueado
        }

        EquipmentSlot slot = event.getSlot();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        boolean isArmorSlot = isArmorSlot(slot);

        String message = null;

        if (isArmorSlot) {
            if (stage < 1 && (path.startsWith("diamond_") || path.startsWith("iron_"))) {
                message = MSG_DIAMOND;
            } else if (stage < 4 && path.startsWith("netherite_")) {
                message = MSG_NETHERITE; // bloqueada hasta Stage 4
            } else if (path.startsWith("sanguine_") || path.startsWith("necromium_")) {
                message = MSG_ENDGAME; // C&C: bloqueada hasta Stage 5 (Endgame)
            }
        } else if (isHandSlot(slot)) {
            // En mano solo se confiscan herramientas/armas de diamante en Stage < 1.
            if (stage < 1 && isDiamondTool(path)) {
                message = MSG_DIAMOND;
            }
        }

        if (message != null) {
            confiscate(player, slot, stack, message);
        }
    }

    /** Devuelve el item al inventario, vacia el slot, avisa y suena el yunque. */
    private static void confiscate(ServerPlayer player, EquipmentSlot slot, ItemStack stack, String message) {
        ItemStack returned = stack.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(returned);
        player.sendSystemMessage(Component.literal(message));
        player.playNotifySound(SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 0.5f);
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private static boolean isHandSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    private static boolean isDiamondTool(String path) {
        return path.equals("diamond_pickaxe")
                || path.equals("diamond_axe")
                || path.equals("diamond_shovel")
                || path.equals("diamond_hoe")
                || path.equals("diamond_sword");
    }
}
