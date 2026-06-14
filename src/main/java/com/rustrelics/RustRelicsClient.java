package com.rustrelics;

import com.rustrelics.client.DiamondFocusOverlay;
import com.rustrelics.enchantment.leveling.client.EnchantmentTooltips;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = RustRelics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RustRelicsClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(DiamondFocusOverlay.class);
        NeoForge.EVENT_BUS.register(EnchantmentTooltips.class);
    }
}
