package com.rustrelics.enchantment.leveling;

import com.rustrelics.RustRelics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModCompat {

    private static Set<String> detectedEnchantMods = Collections.emptySet();

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        Set<String> found = new HashSet<>();
        ModList.get().getMods().forEach(modInfo -> {
            String modId = modInfo.getModId();
            if (modId.contains("enchant")) {
                found.add(modId);
            }
        });
        detectedEnchantMods = Set.copyOf(found);

        if (!detectedEnchantMods.isEmpty()) {
            RustRelics.LOGGER.info(
                    "[R&R Leveling] Detected enchantment mods: {}",
                    String.join(", ", detectedEnchantMods)
            );
            RustRelics.LOGGER.info(
                    "[R&R Leveling] Enchantments from these mods are automatically compatible with the leveling system."
            );
        } else {
            RustRelics.LOGGER.info(
                    "[R&R Leveling] No enchantment-focused mods detected. Using vanilla enchantments only."
            );
        }
    }

    public static boolean hasEnchantMod(String modId) {
        return detectedEnchantMods.contains(modId);
    }

    public static Set<String> getDetectedEnchantMods() {
        return detectedEnchantMods;
    }
}
