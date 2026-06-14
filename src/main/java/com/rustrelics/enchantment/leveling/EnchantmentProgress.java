package com.rustrelics.enchantment.leveling;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record EnchantmentProgress(Map<ResourceLocation, Integer> progress) {

    private static final Codec<Map<ResourceLocation, Integer>> MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT);

    public static final Codec<EnchantmentProgress> CODEC =
            MAP_CODEC.xmap(EnchantmentProgress::new, EnchantmentProgress::progress);

    public static final StreamCodec<ByteBuf, EnchantmentProgress> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT)
                    .map(EnchantmentProgress::new, EnchantmentProgress::progress);

    public static final EnchantmentProgress EMPTY = new EnchantmentProgress(Collections.emptyMap());

    public EnchantmentProgress {
        progress = Map.copyOf(progress);
    }

    public int getProgress(ResourceLocation enchId) {
        return progress.getOrDefault(enchId, 0);
    }

    public EnchantmentProgress withProgress(ResourceLocation enchId, int amount) {
        Map<ResourceLocation, Integer> map = new HashMap<>(progress);
        if (amount <= 0) {
            map.remove(enchId);
        } else {
            map.put(enchId, amount);
        }
        return new EnchantmentProgress(map);
    }

    public boolean isEmpty() {
        return progress.isEmpty();
    }

    public static int getThreshold(int maxDamage, int targetLevel) {
        int base = Mth.clamp(maxDamage / 100, 1, 8);
        return base * targetLevel * targetLevel;
    }
}
