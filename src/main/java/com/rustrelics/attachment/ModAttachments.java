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
}
