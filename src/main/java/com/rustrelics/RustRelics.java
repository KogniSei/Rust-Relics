package com.rustrelics;

import com.mojang.logging.LogUtils;
import com.rustrelics.boss.BossBuffs;
import com.rustrelics.command.RustRelicsCommands;
import com.rustrelics.equipment.EquipmentGate;
import com.rustrelics.stage.StageTriggers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entrypoint de Rust & Relics.
 *
 * Este mod porta a Java la logica de runtime perf-critica que antes vivia en
 * KubeJS (gating de equipo, triggers de stage, efectos de set, spawns, luna de
 * sangre). El moldeo de datos de items de otros mods (durabilidad/atributos)
 * sigue en KubeJS; el puente entre ambos mundos es el scoreboard rr_stage, que
 * este mod espeja en cada cambio de stage.
 */
@Mod(RustRelics.MODID)
public class RustRelics {
    public static final String MODID = "rustrelics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RustRelics(IEventBus modEventBus, ModContainer modContainer) {
        // Listeners del bus de eventos del juego (NeoForge.EVENT_BUS).
        // Cada subsistema registra sus propios @SubscribeEvent estaticos.
        NeoForge.EVENT_BUS.register(EquipmentGate.class);
        NeoForge.EVENT_BUS.register(RustRelicsCommands.class);
        NeoForge.EVENT_BUS.register(StageTriggers.class);
        NeoForge.EVENT_BUS.register(BossBuffs.class);

        LOGGER.info("[Rust & Relics] Mod nativo inicializado (modid={}).", MODID);
    }
}
