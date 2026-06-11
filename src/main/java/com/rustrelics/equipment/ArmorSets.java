package com.rustrelics.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Deteccion de sets de armadura completos (4 piezas del mismo material).
 * Equivalente nativo de los helpers fullSet/pieceMatches de equipment_effects.js.
 *
 * IDs por ResourceLocation string → soft-dep safe (mod ausente = nunca coincide).
 */
public final class ArmorSets {

    public static final String SILVER = "caverns_and_chasms:silver";
    public static final String STERLING = "galosphere:sterling";
    public static final String SANGUINE = "caverns_and_chasms:sanguine";
    public static final String NECROMIUM = "caverns_and_chasms:necromium";

    private ArmorSets() {}

    /** true si lleva las 4 piezas del material indicado (p.ej. "galosphere:sterling"). */
    public static boolean fullSet(LivingEntity entity, String mat) {
        return (
            piece(entity, EquipmentSlot.HEAD, mat + "_helmet") &&
            piece(entity, EquipmentSlot.CHEST, mat + "_chestplate") &&
            piece(entity, EquipmentSlot.LEGS, mat + "_leggings") &&
            piece(entity, EquipmentSlot.FEET, mat + "_boots")
        );
    }

    /** Sets con "maldicion necrotica" (sin curacion natural): Sanguine o Necromium. */
    public static boolean cursed(LivingEntity entity) {
        return fullSet(entity, SANGUINE) || fullSet(entity, NECROMIUM);
    }

    public static boolean isArmorSlot(EquipmentSlot slot) {
        return (
            slot == EquipmentSlot.HEAD ||
            slot == EquipmentSlot.CHEST ||
            slot == EquipmentSlot.LEGS ||
            slot == EquipmentSlot.FEET
        );
    }

    private static boolean piece(
        LivingEntity entity,
        EquipmentSlot slot,
        String expectedId
    ) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
            .toString()
            .equals(expectedId);
    }
}
