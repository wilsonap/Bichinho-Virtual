# Minijogo Siga as Pegadas (footsteps)

**Data:** 2026-08-21  
**minigameId:** `footsteps`  
**Rota:** `game_footsteps`

## Pipeline reutilizado

Hub → rota → `recordMinigameScore("footsteps", …)` → `recordMinigameResult` (moedas, missão, XP, highscore).  
Migration **5→6**: `footstepsHighscore`.

## Arquivos

| Arquivo | Função |
|---------|--------|
| `game/footsteps/FootstepsGameEngine.kt` | Sequência, vidas, combo, animação |
| `ui/screens/FootstepsMinigameScreen.kt` | Parque + pet real + pads |
| `FootstepsGameEngineTest.kt` | Acerto, erro, vidas, combo x5, rewards |

## Regras

- Observe → pet anda a sequência; Sua vez → jogador repete
- Rodada n: sequência com `2+n` passos (máx. 8)
- 3 vidas; combo máx. x5; moedas só no fim
- Loop `withFrameNanos` só com `needsAnimationLoop`
