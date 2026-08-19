# Otimização de performance — Corrida do Bichinho

**Data:** 2026-08-18  
**Status:** implementado

## Problema

Loop com `delay(20)` + `mutableState` a cada tick → recomposição em massa, FPS ~50 e stutter.

## Solução

| Antes | Depois |
|-------|--------|
| `while { delay(20) }` | `withFrameNanos` + delta-time |
| Física por tick | Unidades/segundo (`* 50`) |
| `distanceScore` state/frame | Engine puro + HUD ~10 Hz |
| `groundY = …` no Canvas | Removido |
| `filter` no draw | Iteração direta |
| Brush por frame | `remember` + `GroundBrushCache` |

## Arquivos

| Arquivo | Função |
|---------|--------|
| `game/runner/RunnerGameEngine.kt` | Física, spawn, colisão, distância |
| `ui/screens/RunnerMinigameScreen.kt` | Input, Canvas, HUD, SFX de evento |
| `test/.../RunnerGameEngineTest.kt` | Calibração FPS-independente |

## Calibração (legado 20 ms → 50 ticks/s)

- `speed = 7.5 * 50`
- `gravity = 1.1 * 50 * 50`
- `jump = -18 * 50`
- spawn a cada `80/50 = 1.6s`
- distância `50` unidades/s

## Fora de escopo

Moedas/XP/missões/saúde/evolução, áudio global, outros minijogos, fórmula de recompensa no fim.
