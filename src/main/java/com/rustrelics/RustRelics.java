package com.rustrelics;

import com.mojang.logging.LogUtils;
import com.rustrelics.bloodmoon.BloodMoonBuffs;
import com.rustrelics.bloodmoon.BloodMoonManager;
import com.rustrelics.boss.BossBuffs;
import com.rustrelics.boss.WitherBossBuffs;
import com.rustrelics.command.RustRelicsCommands;
import com.rustrelics.eclipse.EclipseMobEffects;
import com.rustrelics.eclipse.SolarEclipseManager;
import com.rustrelics.effects.KarmicRetributionHandler;
import com.rustrelics.effects.ModEffects;
import com.rustrelics.attachment.ModAttachments;
import com.rustrelics.equipment.DiamondFocus;
import com.rustrelics.equipment.EquipmentGate;
import com.rustrelics.equipment.SetEffects;
import com.rustrelics.network.SyncDiamondChargesPacket;
import com.rustrelics.spawn.SpawnControl;
import com.rustrelics.stage.EternalNightManager;
import com.rustrelics.stage.HardmodeBuffs;
import com.rustrelics.stage.LifecycleSync;
import com.rustrelics.stage.PortalGate;
import com.rustrelics.stage.SilentStage;
import com.rustrelics.stage.SleepGate;
import com.rustrelics.stage.Stage4PlayerBuffs;
import com.rustrelics.stage.StageTriggers;
import com.rustrelics.stage.WardenBlessing;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
        NeoForge.EVENT_BUS.register(WitherBossBuffs.class);
        NeoForge.EVENT_BUS.register(LifecycleSync.class);
        // Fase 3 — efectos de set
        NeoForge.EVENT_BUS.register(SetEffects.class);
        // Fase 4 — gate del Nether, spawns y luna de sangre
        NeoForge.EVENT_BUS.register(PortalGate.class);
        NeoForge.EVENT_BUS.register(SpawnControl.class);
        NeoForge.EVENT_BUS.register(BloodMoonManager.class);
        NeoForge.EVENT_BUS.register(BloodMoonBuffs.class);
        // Hardmode permanente (Stage 3+)
        NeoForge.EVENT_BUS.register(HardmodeBuffs.class);
        // Stage 4: buffs al jugador y stage silencioso
        NeoForge.EVENT_BUS.register(Stage4PlayerBuffs.class);
        NeoForge.EVENT_BUS.register(SilentStage.class);
        // Stage 4: noche eterna y bloqueo de sueno
        NeoForge.EVENT_BUS.register(EternalNightManager.class);
        NeoForge.EVENT_BUS.register(SleepGate.class);
        // Stage Secreto: Warden
        NeoForge.EVENT_BUS.register(WardenBlessing.class);
        // Stage 5: Eclipse Solar (ahora que el ciclo dia/noche volvio)
        NeoForge.EVENT_BUS.register(SolarEclipseManager.class);
        NeoForge.EVENT_BUS.register(EclipseMobEffects.class);
        // Stage 5+: Retribucion Karmica
        NeoForge.EVENT_BUS.register(KarmicRetributionHandler.class);
        // Diamond Focus: cristal charges del set de diamante
        NeoForge.EVENT_BUS.register(DiamondFocus.class);
        // Registrar efectos personalizados via DeferredRegister
        ModEffects.EFFECTS.register(modEventBus);
        // Registrar Data Attachments
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Registrar eventos estaticos de la propia clase (payload registration)
        modEventBus.register(RustRelics.class);

        LOGGER.info(
            "[Rust & Relics] Mod nativo inicializado (modid={}).",
            MODID
        );
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
            SyncDiamondChargesPacket.TYPE,
            SyncDiamondChargesPacket.STREAM_CODEC,
            SyncDiamondChargesPacket::handle
        );
    }
}
