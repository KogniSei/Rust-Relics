package com.rustrelics.network;

import com.rustrelics.RustRelics;
import com.rustrelics.client.DiamondFocusOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncDiamondChargesPacket(int charges) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncDiamondChargesPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(RustRelics.MODID, "sync_diamond_charges")
        );

    public static final StreamCodec<FriendlyByteBuf, SyncDiamondChargesPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncDiamondChargesPacket::charges,
            SyncDiamondChargesPacket::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDiamondChargesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DiamondFocusOverlay.setCharges(packet.charges()));
    }
}
