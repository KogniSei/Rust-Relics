# Rust & Relics

Un mod de progresión global para NeoForge 1.21.1 que transforma Minecraft en una experiencia de pacing intencional, sin tutoriales forzados ni checklists de quests.

## ¿Qué hace?

El mundo tiene un **stage** invisible que avanza cuando la comunidad del servidor cumple hitos: matar bosses, explorar dimensiones, derrotar desafíos. No es individual — es **global**. Cuando alguien abre el Nether, el mundo cambia para todos.

Cada stage desbloquea equipamiento, spawns nuevos, eventos atmosféricos y presión creciente. Puedes ignorar la progresión y construir en paz, o perseguirla y sentir cómo el juego se vuelve más hostil y más gratificante.

## Filosofía

- **Nada es gratis.** Cada avance tiene un costo visible en el mundo.
- **Sin hand-holding.** Los logros guían, pero no obligan. El jugador descubre.
- **De vanilla+ a RPG.** Empieza calmado, evoluciona a números grandes y decisiones tensas.
- **Hecho para servidores.** La progresión es conversación, no competencia.

## Características

| Sistema | Descripción |
|---|---|
| Stages globales (0–5) | Hitos basados en bosses y dimensiones |
| Gating de equipo | No puedes saltar tiers. El diamante espera su momento. |
| Efectos de set | Cada armadura tiene identidad, no solo números más altos |
| Eventos mundiales | Luna de sangre, noche eterna, eclipses |
| Buffs escalonados | Mobs y jugadores crecen juntos |
| Scoreboard bridge | Compatible con datapacks y KubeJS vía `rr_stage` |

## Instalación

1. Descarga el `.jar` de [Releases](../../releases)
2. Colócalo en tu carpeta `mods/` junto a NeoForge 1.21.1
3. Inicia el mundo. La progresión empieza sola.

## Compilar desde código

```bash
./gradlew build
