package com.rustrelics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rustrelics.RustRelics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Config de los eventos (Luna Muerta / Luna Pálida / Eclipse Solar): pools de
 * spawn, intensidad del spawner y probabilidades de loot. Se carga desde
 * {@code config/rustrelics_events.json5} (estilo {@code item_obliterator.json5});
 * si el archivo no existe, se escribe con los defaults curados de este modpack.
 *
 * Los IDs se resuelven de forma perezosa contra los registros (ya congelados en
 * runtime), asi que cualquier ID inexistente se ignora → soft-dep safe.
 */
public final class EventConfig {

    /** Parámetros de un evento. */
    public static final class EventCfg {
        public int interval, minWave, maxWave, softCap, ringMin, ringMax;
        public double bookChance, relicChance;
        public List<String> poolIds = new ArrayList<>();
        public List<String> relicNamespaces = new ArrayList<>();

        private List<EntityType<?>> resolvedPool; // cache perezosa

        /** Pool resuelto a EntityType (ignora IDs ausentes). Cacheado tras la 1ª llamada. */
        public List<EntityType<?>> pool() {
            if (resolvedPool == null) {
                List<EntityType<?>> r = new ArrayList<>();
                for (String s : poolIds) {
                    ResourceLocation rl = ResourceLocation.tryParse(s);
                    if (rl != null) BuiltInRegistries.ENTITY_TYPE.getOptional(rl).ifPresent(r::add);
                }
                resolvedPool = r;
            }
            return resolvedPool;
        }

        void invalidate() { resolvedPool = null; }
    }

    public static final EventCfg DEADMOON = new EventCfg();
    public static final EventCfg PALEMOON = new EventCfg();
    public static final EventCfg ECLIPSE = new EventCfg();

    private static List<Item> relicItems; // cache perezosa (palemoon)

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EventConfig() {}

    static {
        setCodeDefaults();
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    public static void loadConfig() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("rustrelics_events.json5");
        try {
            if (!Files.exists(path)) {
                Files.writeString(path, buildDefaultJson());
                RustRelics.LOGGER.info("[EventConfig] Config creada con defaults: {}", path);
                return; // los defaults ya estan en memoria
            }
            String raw = Files.readString(path);
            String json = raw.replaceAll("//[^\n]*", "").replaceAll(",\\s*([\\]}])", "$1");
            JsonObject root = new Gson().fromJson(json, JsonObject.class);
            parseEvent(root, "deadmoon", DEADMOON);
            parseEvent(root, "palemoon", PALEMOON);
            parseEvent(root, "eclipse", ECLIPSE);
            relicItems = null; // re-resolver con la config nueva
            RustRelics.LOGGER.info("[EventConfig] Config de eventos cargada.");
        } catch (Exception e) {
            RustRelics.LOGGER.error("[EventConfig] Error cargando config; uso defaults.", e);
        }
    }

    private static void parseEvent(JsonObject root, String key, EventCfg cfg) {
        if (root == null || !root.has(key) || !root.get(key).isJsonObject()) return;
        JsonObject o = root.getAsJsonObject(key);
        cfg.interval = getInt(o, "interval", cfg.interval);
        cfg.minWave = getInt(o, "min_wave", cfg.minWave);
        cfg.maxWave = getInt(o, "max_wave", cfg.maxWave);
        cfg.softCap = getInt(o, "soft_cap", cfg.softCap);
        cfg.ringMin = getInt(o, "ring_min", cfg.ringMin);
        cfg.ringMax = getInt(o, "ring_max", cfg.ringMax);
        cfg.bookChance = getDouble(o, "book_chance", cfg.bookChance);
        cfg.relicChance = getDouble(o, "relic_chance", cfg.relicChance);
        if (o.has("spawn_pool") && o.get("spawn_pool").isJsonArray()) {
            cfg.poolIds = toStringList(o.getAsJsonArray("spawn_pool"));
            cfg.invalidate();
        }
        if (o.has("relic_namespaces") && o.get("relic_namespaces").isJsonArray()) {
            cfg.relicNamespaces = toStringList(o.getAsJsonArray("relic_namespaces"));
        }
    }

    private static int getInt(JsonObject o, String k, int def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : def;
    }

    private static double getDouble(JsonObject o, String k, double def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : def;
    }

    private static List<String> toStringList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        for (JsonElement el : arr) out.add(el.getAsString());
        return out;
    }

    // ------------------------------------------------------------------
    // Reliquias (palemoon)
    // ------------------------------------------------------------------

    /** Devuelve un ItemStack de una reliquia aleatoria (de los namespaces config), o null. */
    public static ItemStack randomRelic(RandomSource rng) {
        if (relicItems == null) {
            List<Item> items = new ArrayList<>();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (PALEMOON.relicNamespaces.contains(entry.getKey().location().getNamespace())) {
                    items.add(entry.getValue());
                }
            }
            relicItems = items;
        }
        if (relicItems.isEmpty()) return null;
        return new ItemStack(relicItems.get(rng.nextInt(relicItems.size())));
    }

    // ------------------------------------------------------------------
    // Defaults
    // ------------------------------------------------------------------

    private static void setCodeDefaults() {
        DEADMOON.interval = 100; DEADMOON.minWave = 3; DEADMOON.maxWave = 5;
        DEADMOON.softCap = 36; DEADMOON.ringMin = 16; DEADMOON.ringMax = 48;
        DEADMOON.bookChance = 0.08;
        DEADMOON.poolIds = List.of(
            "minecraft:zombie", "minecraft:husk", "minecraft:skeleton", "minecraft:stray",
            "minecraft:spider", "minecraft:cave_spider", "minecraft:creeper",
            "minecraft:drowned", "minecraft:witch", "minecraft:zombie_villager");

        PALEMOON.interval = 140; PALEMOON.minWave = 1; PALEMOON.maxWave = 2;
        PALEMOON.softCap = 20; PALEMOON.ringMin = 16; PALEMOON.ringMax = 48;
        PALEMOON.relicChance = 0.04;
        PALEMOON.relicNamespaces = List.of("artifacts");
        PALEMOON.poolIds = List.of(
            "minecraft:phantom",
            "hominid:bellman", "hominid:famished", "hominid:fossilized", "hominid:incendiary",
            "hominid:juggernaut", "hominid:mellified", "hominid:vampire",
            "fangs_n_claws:werewolf", "fangs_n_claws:ogre", "fangs_n_claws:hell_ogre",
            "fangs_n_claws:cave_ogre", "fangs_n_claws:goblin", "fangs_n_claws:dart_goblin",
            "fangs_n_claws:imp", "fangs_n_claws:scorpion", "fangs_n_claws:silver_skeleton",
            "fangs_n_claws:ghost", "fangs_n_claws:owlbear", "fangs_n_claws:werevillager",
            "mythsandlegends:abaddon", "mythsandlegends:alp", "mythsandlegends:amarok",
            "mythsandlegends:black_charro", "mythsandlegends:candle_keeper", "mythsandlegends:condemned",
            "mythsandlegends:gargoyle", "mythsandlegends:imp", "mythsandlegends:karakondjul",
            "mythsandlegends:lampad", "mythsandlegends:possessed_armor",
            "mythsandlegends:possessed_armor_archer", "mythsandlegends:possessed_armor_inquisitor",
            "mythsandlegends:scorched_sentinel", "mythsandlegends:warborn_aegis",
            "born_in_chaos_v1:nightmare_stalker", "born_in_chaos_v1:dread_hound",
            "born_in_chaos_v1:fallen_chaos_knight", "born_in_chaos_v1:scarlet_persecutor",
            "born_in_chaos_v1:bonescaller", "born_in_chaos_v1:decaying_zombie");

        ECLIPSE.interval = 120; ECLIPSE.minWave = 2; ECLIPSE.maxWave = 4;
        ECLIPSE.softCap = 30; ECLIPSE.ringMin = 16; ECLIPSE.ringMax = 48;
        ECLIPSE.poolIds = List.of(
            "minecraft:pillager", "minecraft:vindicator", "minecraft:evoker",
            "minecraft:illusioner", "minecraft:ravager",
            "illagerinvasion:alchemist", "illagerinvasion:archivist", "illagerinvasion:basher",
            "illagerinvasion:firecaller", "illagerinvasion:inquisitor", "illagerinvasion:invoker",
            "illagerinvasion:marauder", "illagerinvasion:necromancer", "illagerinvasion:provoker",
            "illagerinvasion:sorcerer",
            "takesapillage:archer", "takesapillage:skirmisher", "takesapillage:legioner",
            "raidsenhanced:zapper",
            "hunters_return:hunter");
    }

    private static String buildDefaultJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("deadmoon", dto(DEADMOON, "book_chance"));
        root.put("palemoon", dto(PALEMOON, "relic_chance"));
        root.put("eclipse", dto(ECLIPSE, null));
        return GSON.toJson(root);
    }

    private static Map<String, Object> dto(EventCfg c, String lootKey) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("interval", c.interval);
        m.put("min_wave", c.minWave);
        m.put("max_wave", c.maxWave);
        m.put("soft_cap", c.softCap);
        m.put("ring_min", c.ringMin);
        m.put("ring_max", c.ringMax);
        if ("book_chance".equals(lootKey)) m.put("book_chance", c.bookChance);
        if ("relic_chance".equals(lootKey)) {
            m.put("relic_chance", c.relicChance);
            m.put("relic_namespaces", c.relicNamespaces);
        }
        m.put("spawn_pool", c.poolIds);
        return m;
    }
}
