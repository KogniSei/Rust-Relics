# Rust & Relics

Rust & Relics is a NeoForge 1.21.1 progression mod that adds a global stage
system inspired by Terraria hardmode. It gates equipment, reacts to boss kills,
syncs progression through scoreboards, and adds world-pressure events such as
Blood Moon nights.

This repository currently builds a working jar:

```powershell
.\gradlew.bat build
```

The compiled mod is generated at:

```text
build/libs/rustrelics-0.2.0.jar
```

## What It Does

- Tracks a global `rr_stage` progression value for the world.
- Blocks or allows equipment based on progression.
- Locks Nether access until the configured stage is reached.
- Adds Blood Moon activation/deactivation state through scoreboard data.
- Applies stage-based world pressure, spawn control, boss buffs, and set effects.
- Keeps compatibility-oriented optional hooks for content mods without requiring
  compile-time dependencies.

## Why A Server Owner Would Pay For It

Many modpacks become too open too quickly: players rush diamond/netherite,
skip mid-game content, and burn out. Rust & Relics gives a server or private
pack a stronger pacing layer by making progression feel earned and communal.

The $5 starter deliverable is a ready-to-build NeoForge mod plus a compact
configuration/adaptation pass:

- confirm the jar builds;
- explain where progression logic lives;
- adjust one gate, message, or stage threshold;
- provide installation instructions for a NeoForge 1.21.1 server/client.

## Install

1. Build the jar with `.\gradlew.bat build`.
2. Copy `build/libs/rustrelics-0.2.0.jar` into the `mods` folder.
3. Run Minecraft/NeoForge 1.21.1.
4. Use the mod's stage commands and advancements to drive progression.

## Developer Notes

Important source areas:

- `com.rustrelics.stage`: stage state, triggers, hardmode, Nether portal gates.
- `com.rustrelics.equipment`: equipment gates and armor set effects.
- `com.rustrelics.bloodmoon`: Blood Moon state and buffs.
- `com.rustrelics.spawn`: spawn pressure adjustments.
- `com.rustrelics.command`: stage command helpers.

Runtime logic is native Java. Data and content shaping from external mods can
still be handled separately through datapacks or KubeJS, with `rr_stage` acting
as the bridge.

Current native coverage:

- Stage persistence and scoreboard mirrors for `rr_stage` and `rr_bloodmoon`.
- Boss and Nether triggers, equipment gating, set effects, spawn control,
  Blood Moon pressure, boss buffs, and Stage 3+ hardmode pressure.
- Stage 0 and Stage 1 advancement resources under `src/main/resources/data`.

Still intentionally external or pending:

- Item durability/attribute reshaping for third-party mod items.
- Late-stage datagen cleanup for recipes, loot tables, and any remaining
  datapack/KubeJS content.
- End-to-end in-game validation against the full modpack loadout.

## License

All Rights Reserved.
