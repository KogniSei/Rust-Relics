package com.rustrelics.effects;

import com.rustrelics.RustRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = RustRelics.MODID, value = Dist.CLIENT)
public final class KarmicRetributionHud {

    private static final int PURPLE = 0x80AA00AA;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int Y_OFFSET = -10;

    private KarmicRetributionHud() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!player.hasEffect(ModEffects.KARMIC_RETRIBUTION)) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - Y_OFFSET - BAR_HEIGHT;

        int amplifier = player.getEffect(ModEffects.KARMIC_RETRIBUTION).getAmplifier();
        double ratio = Math.min(1.0, (amplifier + 1) / 5.0);
        int filledWidth = (int) (BAR_WIDTH * ratio);

        gui.fill(x, y, x + filledWidth, y + BAR_HEIGHT, PURPLE);
    }
}
