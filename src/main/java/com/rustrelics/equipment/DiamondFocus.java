package com.rustrelics.equipment;

import com.rustrelics.RustRelics;
import com.rustrelics.attachment.ModAttachments;
import com.rustrelics.network.SyncDiamondChargesPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class DiamondFocus {

    public static final int MAX_CHARGES = 3;
    public static final float DAMAGE_PER_CHARGE = 0.25f;

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasFullDiamondSet(player)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;

        int current = getCharges(player);
        if (current < MAX_CHARGES) {
            setCharges(player, current + 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingIncomingDamageEvent event) {
        if (
            !(event.getSource().getEntity() instanceof ServerPlayer player)
        ) return;
        if (!hasFullDiamondSet(player)) return;

        int charges = getCharges(player);
        if (charges <= 0) return;

        event.setAmount(
            event.getAmount() * (1.0f + charges * DAMAGE_PER_CHARGE)
        );
        setCharges(player, 0);

        if (event.getEntity().level() instanceof ServerLevel level) {
            level.sendParticles(
                ParticleTypes.CRIT,
                event.getEntity().getX(),
                event.getEntity().getY() + 1.0,
                event.getEntity().getZ(),
                charges * 4,
                0.3,
                0.3,
                0.3,
                0.1
            );
            level.playSound(
                null,
                event.getEntity().blockPosition(),
                SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.PLAYERS,
                1.0f,
                1.2f + (charges * 0.1f)
            );
        }
    }

    public static int getCharges(ServerPlayer player) {
        return player.getData(ModAttachments.DIAMOND_CHARGES);
    }

    public static void setCharges(ServerPlayer player, int charges) {
        int clamped = Math.max(0, Math.min(MAX_CHARGES, charges));
        player.setData(ModAttachments.DIAMOND_CHARGES, clamped);
        PacketDistributor.sendToPlayer(
            player,
            new SyncDiamondChargesPacket(clamped)
        );
    }

    private static boolean hasFullDiamondSet(ServerPlayer player) {
        return (
            player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) &&
            player
                .getItemBySlot(EquipmentSlot.CHEST)
                .is(Items.DIAMOND_CHESTPLATE) &&
            player
                .getItemBySlot(EquipmentSlot.LEGS)
                .is(Items.DIAMOND_LEGGINGS) &&
            player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS)
        );
    }

    public static final ResourceLocation TEXTURE_OFF =
        ResourceLocation.fromNamespaceAndPath(
            RustRelics.MODID,
            "textures/gui/apagado.png"
        );
    public static final ResourceLocation TEXTURE_ON =
        ResourceLocation.fromNamespaceAndPath(
            RustRelics.MODID,
            "textures/gui/prendido.png"
        );
}
