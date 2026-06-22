package com.rustrelics.item;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rustrelics.RustRelics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ItemObliterator {

    private static Set<String> blacklist = Collections.emptySet();

    private static int tickCounter = 0;

    private ItemObliterator() {}

    public static void loadConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("item_obliterator.json5");
        if (!Files.exists(configPath)) {
            RustRelics.LOGGER.warn("[ItemObliterator] No se encuentra {}; lista vacia.", configPath);
            return;
        }
        try {
            String raw = Files.readString(configPath);
            String json = raw.replaceAll("//[^\n]*", "");
            json = json.replaceAll(",\\s*([\\]}])", "$1");

            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            JsonArray arr = obj.getAsJsonArray("blacklisted_items");
            Set<String> items = new HashSet<>();
            for (JsonElement el : arr) {
                items.add(el.getAsString());
            }
            blacklist = Collections.unmodifiableSet(items);
            RustRelics.LOGGER.info("[ItemObliterator] {} items blacklisteados.", blacklist.size());
        } catch (Exception e) {
            RustRelics.LOGGER.error("[ItemObliterator] Error cargando config", e);
        }
    }

    private static boolean isBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = stack.getItemHolder().unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");
        return blacklist.contains(id);
    }

    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (isBlacklisted(item.getItem())) {
            item.discard();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> {
            if (drop instanceof ItemEntity item && isBlacklisted(item.getItem())) {
                item.discard();
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onVillagerTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack result = event.getMerchantOffer().getResult();
            if (isBlacklisted(result)) {
                player.getInventory().removeItem(result);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scanAndRemove(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scanAndRemove(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % 100 != 0) return;
        var server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scanAndRemove(player);
        }
    }

    private static void scanAndRemove(ServerPlayer player) {
        var inventory = player.getInventory();
        boolean removed = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isBlacklisted(stack)) {
                inventory.setItem(i, ItemStack.EMPTY);
                removed = true;
            }
        }
        if (removed) {
            player.inventoryMenu.broadcastFullState();
        }
    }
}
