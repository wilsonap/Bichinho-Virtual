# Otimização de performance — Captura de Objetos

**Data:** 2026-08-18  
**Status:** implementado

## Problema

`delay(20)` + itens como `Box`/`Text` + mutação de `y` sem invalidar Snapshot → UI irregular e recomposições caras.

## Solução

| Antes | Depois |
|-------|--------|
| `while { delay(20) }` | `withFrameNanos` + delta-time |
| `Box`+`Text` por item | Emoji no Canvas (`drawIntoCanvas`) |
| Offset `320.dp`/`500.dp` | `x * width`, `y * height` |
| State Compose na física | `CatchGameEngine` puro |
| HUD a cada evento/frame | HUD ~10 Hz |
| Colisão só na faixa | Detecção de atravessamento (tunnel) |

## Arquivos

| Arquivo | Função |
|---------|--------|
| `game/catchgame/CatchGameEngine.kt` | Física, spawn, colisão, score |
| `ui/screens/CatchMinigameScreen.kt` | Input, Canvas, HUD, SFX |
| `test/.../CatchGameEngineTest.kt` | Calibração e regras |

## Calibração

- `speedPerSecond = speedTick * 50`
- Spawn a cada `35/50 = 0.7s`
- Zona captura `0.78..0.88`, tolerância X `0.12`

## Fora de escopo

Moedas/XP/missões/saúde/evolução, áudio global, Runner, fórmula `onFinishGame(score, coinsEarned)`.
