package com.rustrelics.attachment;

import com.mojang.serialization.Codec;
import com.rustrelics.RustRelics;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RustRelics.MODID);

    /**
     * Cargas del Foco de Cristal (0-3). Persiste entre sesiones via Codec.
     * Server-side; se sincroniza al cliente con SyncDiamondChargesPacket.
     */
    public static final Supplier<AttachmentType<Integer>> DIAMOND_CHARGES =
        ATTACHMENT_TYPES.register("diamond_charges", () ->
            AttachmentType.builder(() -> 0)
                .serialize(Codec.INT)
                .build()
        );

    /**
     * Cuantos aldeanos ha matado el jugador. Persiste entre sesiones.
     * Usado para escalar la Retribucion Karmica en Stage 5.
     */
    public static final Supplier<AttachmentType<Integer>> VILLAGER_KILLS =
        ATTACHMENT_TYPES.register("villager_kills", () ->
            AttachmentType.builder(() -> 0)
                .serialize(Codec.INT)
                .build()
        );

    /**
     * Cuantos pillagers/illagers ha matado el jugador. Persiste entre sesiones.
     * Usado por PillageThreatTracker para escalar la agresividad de la faccion.
     */
    public static final Supplier<AttachmentType<Integer>> PILLAGER_KILLS =
        ATTACHMENT_TYPES.register("pillager_kills", () ->
            AttachmentType.builder(() -> 0)
                .serialize(Codec.INT)
                .build()
        );
}
