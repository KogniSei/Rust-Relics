# Revisión de Código: Rust & Relics (NeoForge 1.21.1)

**Fecha:** 2025-06-04  
**Versión del mod:** 0.3.0  
**Minecraft:** 1.21.1  
**NeoForge:** 21.1.233  
**MDG:** 2.0.141

---

## 1. Resumen Ejecutivo

El proyecto **Rust & Relics** es un mod de progresión por etapas (*stages*) que porta lógica crítica desde KubeJS a Java nativo para mejorar el rendimiento. La arquitectura general es **sólida y bien organizada**: los subsistemas están separados en paquetes claros, usa el bus de eventos de NeoForge de forma reactiva (sin *polling* por tick) y separa correctamente lógica servidor/cliente.

**Estado general:** Bien estructurado, pero con **varios bugs funcionales** que deben corregirse antes de una release estable, principalmente relacionados con la persistencia de estado de entidades y posibles errores de compilación con la API de scoreboards.

---

## 2. Problemas Críticos (Bloqueantes / Funcionales)

### 2.1 Re-buffeo infinito de mobs (BloodMoon, DeadMoon, Hardmode) 🔴

**Archivos afectados:**
- `bloodmoon.BloodMoonBuffs` (línea 51: `BUFFED_TAG`)
- `bloodmoon.DeadMoonBuffs` (línea 13: `BUFFED_TAG`)
- `stage.HardmodeBuffs` (línea 36: `BUFFED_TAG`)

**Problema:** Los tags de entidad (`living.addTag("rr_bm_buffed")`) **NO persisten** cuando el chunk se descarga y se vuelve a cargar. Esto significa que cada vez que un mob entra/sale de un chunk (viaje del jugador, despawn/reload), se vuelve a buffear acumulativamente.

**Impacto:** Mobs con vida y daño multiplicados exponencialmente, rompiendo el balance del juego.

**Solución recomendada:** Usar `DataAttachments` (ya los usas para jugadores) o datos NBT persistentes (`entity.getPersistentData()`).

```java
// Ejemplo de corrección con NBT persistente:
public static boolean isBuffed(LivingEntity entity, String key) {
    return entity.getPersistentData().getBoolean(key);
}
public static void markBuffed(LivingEntity entity, String key) {
    entity.getPersistentData().putBoolean(key, true);
}
```

---

### 2.2 Memory leak en escalado de jefes (`BossScaling`) 🔴

**Archivo:** `boss.BossScaling.java` (línea 54)

**Problema:** El `Set<UUID> scaledBosses` es un `HashSet` estático que solo se limpia cuando el boss muere (`onEntityDeath`). Si un boss:
- Se desaparece naturalmente (despawn)
- El chunk se descarga sin que el boss muera
- El servidor se cierra

...su UUID permanece en el set **para siempre**. En servidores largos esto consume memoria sin límite.

**Solución recomendada:**
1. Usar un `WeakHashMap<LivingEntity, Boolean>` (pero `LivingEntity` no es weak-referenceable directamente)
2. O mejor: limpiar el set periódicamente validando UUIDs activos, o usar un `Cache<UUID, Boolean>` con tiempo de expiración (Guava o Caffeine si está disponible).
3. Alternativa simple: en cada login o cada 5 minutos, eliminar UUIDs que no correspondan a entidades vivas.

---

### 2.3 Posible error de compilación: API de Scoreboard 1.21.1 🔴

**Archivo:** `util.Scoreboards.java` (línea 35)

**Problema:** En Minecraft 1.21.1, `Scoreboard.getOrCreatePlayerScore()` fue reemplazado por `getOrCreateScore(ScoreHolder, Objective, boolean)`. El método actual probablemente **no compila** en 1.21.1.

**Solución recomendada:**
```java
// NeoForge 1.21.1 correcto:
ScoreAccess access = sb.getOrCreateScore(ScoreHolder.forNameOnly(holder), obj, true);
access.set(value);
```

**Nota:** Verifica que `ScoreHolder.forNameOnly` exista en tu versión de Parchment/NeoForge. Si no, usa `ScoreHolder.fromString` o el constructor interno.

---

### 2.4 Posible error de compilación: `damageSources()` en entidad 🔴

**Archivo:** `effects.KarmicRetribution.java` (línea 40)

**Problema:** `entity.damageSources()` no existe en `LivingEntity` en 1.21.1. Los `DamageSources` se obtienen desde el nivel.

**Solución:**
```java
entity.hurt(entity.level().damageSources().magic(), actualDmg);
```

---

### 2.5 Uso redundante de `execute()` en hilo del servidor 🟡

**Archivos:**
- `bloodmoon.BloodMoonBuffs` (línea 89)
- `bloodmoon.DeadMoonBuffs` (línea 45)
- `stage.HardmodeBuffs` (línea 91)

**Problema:** Dentro de `EntityJoinLevelEvent`, el código ya se ejecuta en el **hilo del servidor**. Llamar `level.getServer().execute(...)` es redundante y puede causar problemas de timing (el spawn se ejecuta en el siguiente tick, potencialmente fuera del contexto del evento).

**Solución:** Crear la entidad directamente sin `execute()`:
```java
// Dentro de onEntityJoin — ya estamos en server thread
LivingEntity extra = (LivingEntity) living.getType().create(level);
if (extra != null) { ... }
```

---

## 3. Problemas de Rendimiento

### 3.1 Iteración de todas las entidades en Eclipse (`EclipseMobEffects`)

**Archivo:** `eclipse.EclipseMobEffects.java` (línea 33)

**Problema:** `overworld.getEntities().getAll()` itera **todas** las entidades cargadas en el mundo cada 20 ticks. Si hay miles de entidades (items, proyectiles, mobs), esto genera basura y carga innecesaria.

**Solución:** Iterar solo `Mob` que estén en fuego:
```java
for (ServerLevel level : server.getAllLevels()) {
    if (level.dimension() != Level.OVERWORLD) continue;
    for (Entity entity : level.getEntities().getAll()) {
        if (entity instanceof Mob mob && mob.isOnFire()) {
            mob.clearFire();
        }
    }
}
```

Aun mejor: usar `level.getEntitiesOfClass(Mob.class, level.getWorldBorder().getBounds())` o aplicar el efecto solo cuando un mob recibe daño de fuego (evento), en lugar de un scan periódico.

---

### 3.2 Cooldown de armadura en `EnchantmentLeveling`

**Archivo:** `enchantment.leveling.EnchantmentLeveling.java` (líneas 32-34)

**Problema:** El `Map<UUID, Long> lastArmorTick` nunca se limpia. Si un jugador se desconecta, su UUID permanece en el mapa para siempre. Es un memory leak muy pequeño pero acumulativo.

**Solución:** Limpiar en `PlayerEvent.PlayerLoggedOutEvent` o usar un `Long2LongOpenHashMap` con expiración.

---

## 4. Problemas de Lógica / Balance

### 4.1 `ItemObliterator` — Trade prevention inefectivo

**Archivo:** `item.ItemObliterator.java` (líneas 86-93)

**Problema:** `onVillagerTrade` solo remueve el item **después** de que el trade ya se completó. El jugador recibe XP y el trade se cuenta. La prevención debería cancelar el evento **antes** de que ocurra.

**Solución:** En NeoForge 1.21.1, `TradeWithVillagerEvent` **no es cancelable**. Alternativas:
- Reemplazar el resultado del trade por `ItemStack.EMPTY` si es posible.
- O documentar que es una limitación conocida y que el item se elimina al entrar al inventario.

---

### 4.2 `EternalNightManager` sobrescribe clima del eclipse al finalizar

**Archivo:** `stage.EternalNightManager.java` (línea 33)

**Problema:** Cuando el stage llega a 5, `restoreDayCycle()` restaura el tiempo a 1000 pero **no restaura el clima** si el eclipse lo había forzado a lluvia. El `SolarEclipseManager` maneja el clima, pero si el eclipse se corta por stage change, el clima puede quedar desincronizado.

**Solución:** Asegurar que `restoreDayCycle` también resetee el clima forzado por el eclipse, o hacer que `EternalNightManager` y `SolarEclipseManager` se coordinen mejor.

---

### 4.3 `SilentStage.materialTier` — Lógica de tier ambigua

**Archivo:** `stage.SilentStage.java` (líneas 84-100)

**Problema:** `path.contains("gold")` coincide con items que contengan "gold" en cualquier parte del nombre (ej. `golden_carrot`, `gold_ingot`, etc.). Además, `chainmail` y `chain` pueden coincidir ambos. Esto puede asignar tiers incorrectos a items no-intencionados.

**Solución:** Usar prefijos (`path.startsWith(...)`) o un `Map<String, Integer>` explícito con IDs conocidos.

---

## 5. Problemas de Mantenibilidad / Estilo

### 5.1 Archivo binario en código fuente

**Archivo:** `src/main/java/com/rustrelics/rustrelics.zip`

**Problema:** Hay un archivo `.zip` dentro del paquete Java. Esto no debería estar en el código fuente. Si es un backup, debería estar en `.gitignore` o fuera del proyecto.

**Acción:** Eliminar del repositorio.

---

### 5.2 Strings hardcodeados en vez de traducciones

**Archivos:** Múltiples (todos los mensajes de chat, títulos, etc.)

**Problema:** Casi todos los mensajes de usuario usan `Component.literal("§c[Rust & Relics] ...")` en lugar de `Component.translatable(...)`. El archivo `en_us.json` tiene traducciones que **no se utilizan** en el código Java.

**Impacto:** Dificulta la traducción a otros idiomas y la consistencia de mensajes.

**Solución:** Migrar los mensajes frecuentes a `Component.translatable("rustrelics.xxx.yyy")`.

---

### 5.3 Duplicación de código en manejo de clima

**Archivos:** `SolarEclipseManager` y `EternalNightManager` manipulan ambos el tiempo y clima del overworld sin compartir una abstracción común.

**Sugerencia:** Crear un `WorldStateManager` que centralice las manipulaciones de tiempo/clima para evitar conflictos.

---

### 5.4 Nombres de variables poco claros en `KarmicRetributionHandler`

**Archivo:** `effects.KarmicRetributionHandler.java`

**Problema:** `KR_DURATION`, `KR_AMPLIFIER_BASE`, `KR_AMPLIFIER_HIT` son abreviaciones innecesarias. `KARMIC_DURATION` y `KARMIC_AMPLIFIER_HIT` son más legibles.

---

## 6. Verificaciones Positivas ✅

### 6.1 Arquitectura de eventos
Excelente uso de eventos reactivos (`LivingEquipmentChangeEvent`, `EntityJoinLevelEvent`, `CanPlayerSleepEvent`) en lugar de polling por tick. Esto es exactamente el objetivo del porteo desde KubeJS.

### 6.2 Separación Cliente/Servidor
`RustRelicsClient` separa correctamente la inicialización del cliente. Los overlays (`DiamondFocusOverlay`, `KarmicRetributionHud`) usan `@OnlyIn(Dist.CLIENT)` o `@EventBusSubscriber(value = Dist.CLIENT)` apropiadamente.

### 6.3 Uso de Data Attachments
`ModAttachments` usa correctamente `AttachmentType` con serialización (`Codec.INT`) para persistencia. Esto es la forma moderna de 1.21.1 para guardar datos por entidad.

### 6.4 Networking moderno
`SyncDiamondChargesPacket` implementa `CustomPacketPayload` con `StreamCodec` y `PayloadRegistrar`, que es el patrón correcto para NeoForge 1.21.1.

### 6.5 Data Components
`ModDataComponents` registra un `DataComponentType<EnchantmentProgress>` con `persistent()` y `networkSynchronized()`, lo cual es correcto para guardar progreso de encantamientos en el item stack.

### 6.6 Soft-dependencies
Los mods opcionales (`galosphere`, `caverns_and_chasms`, `hominid`, etc.) están correctamente declarados como `optional` en `neoforge.mods.toml` con `ordering = AFTER`. Todas las referencias a entidades/items de estos mods se hacen por `ResourceLocation` string, evitando `ClassNotFoundException`.

### 6.7 Firma de SavedData 1.21.1
`StageSavedData.load(CompoundTag, HolderLookup.Provider)` usa la firma correcta para 1.21.1.

### 6.8 Configuración de build
El `build.gradle` usa MDG 2.0 correctamente, con `neoForge.ideSyncTask generateModMetadata` y configuración de runs apropiada. Las propiedades de reemplazo se expanden correctamente en `neoforge.mods.toml`.

---

## 7. Recomendaciones Priorizadas

| Prioridad | Tarea | Archivo(s) | Esfuerzo |
|---|---|---|---|
| 🔴 Crítica | Corregir persistencia de buff tags | `BloodMoonBuffs`, `DeadMoonBuffs`, `HardmodeBuffs` | Medio |
| 🔴 Crítica | Corregir API de Scoreboard | `util.Scoreboards` | Bajo |
| 🔴 Crítica | Corregir `damageSources()` | `effects.KarmicRetribution` | Bajo |
| 🔴 Crítica | Limpiar memory leak en `BossScaling` | `boss.BossScaling` | Medio |
| 🟡 Alta | Eliminar `execute()` redundante | `BloodMoonBuffs`, `DeadMoonBuffs`, `HardmodeBuffs` | Bajo |
| 🟡 Alta | Optimizar `EclipseMobEffects` | `eclipse.EclipseMobEffects` | Bajo |
| 🟡 Alta | Limpiar memory leak en `EnchantmentLeveling` | `enchantment.leveling.EnchantmentLeveling` | Bajo |
| 🟢 Media | Eliminar `.zip` del código fuente | `src/main/java/com/rustrelics/rustrelics.zip` | Bajo |
| 🟢 Media | Migrar strings a traducciones | Múltiples | Alto |
| 🟢 Media | Refinar `SilentStage.materialTier` | `stage.SilentStage` | Bajo |
| 🟢 Media | Documentar `ItemObliterator` trade limitation | `item.ItemObliterator` | Bajo |

---

## 8. Conclusión

**Rust & Relics** es un mod bien pensado y correctamente arquitectado para NeoForge 1.21.1. La separación de subsistemas, el uso de eventos reactivos y la adopción de APIs modernas (Data Attachments, Data Components, CustomPacketPayload) demuestran un buen entendimiento del ecosistema actual.

Los **problemas críticos** son principalmente:
1. **Persistencia de estado** (tags de entidad no persistentes)
2. **Memory leaks** (UUIDs acumulados, mapas de cooldown no limpiados)
3. **Posibles errores de compilación** (API de scoreboard, `damageSources()`)

Una vez corregidos estos puntos, el mod está en una excelente posición para ser estable y escalable.

---

*Revisión realizada sobre el código fuente del repositorio MDK-1.21.1-ModDevGradle.*
