package com.rustrelics.client;

import com.rustrelics.equipment.DiamondFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
public class DiamondFocusOverlay {

    private static int currentCharges = 0;
    private static final int ICON_SIZE = 9;
    private static final int SPACING = 12;

    public static void setCharges(int charges) {
        currentCharges = charges;
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (currentCharges == 0) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int totalWidth = (DiamondFocus.MAX_CHARGES * SPACING) - (SPACING - ICON_SIZE);
        int startX = (screenWidth / 2) - (totalWidth / 2);
        int baseY = screenHeight - 35;

        for (int i = 0; i < DiamondFocus.MAX_CHARGES; i++) {
            boolean filled = i < currentCharges;
            ResourceLocation texture = filled ? DiamondFocus.TEXTURE_ON : DiamondFocus.TEXTURE_OFF;
            int x = startX + (i * SPACING);
            graphics.blit(texture, x, baseY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }
}
