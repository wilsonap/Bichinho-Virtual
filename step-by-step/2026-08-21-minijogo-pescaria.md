# Minijogo Pescaria (fishing)

**Data:** 2026-08-21  
**minigameId:** `fishing`  
**Rota:** `game_fishing`

## Auditoria (reutilizado)

| Peça | Origem |
|------|--------|
| Hub card | `MinigamesHubScreen` |
| Nav / BGM / hide bar | `MainApp` padrão `game_*` |
| Recompensa | `recordMinigameScore` → `recordMinigameResult` |
| Missões / XP | genéricos (inalterados) |
| Performance | padrão Catch/Runner: engine + `withFrameNanos` + Canvas |

## Arquivos

| Arquivo | Função |
|---------|--------|
| `game/fishing/FishingGameEngine.kt` | Física, peixes, lixo, combo, timer 60s |
| `ui/screens/FishingMinigameScreen.kt` | Canvas + HUD + resultado |
| `FishingGameEngineTest.kt` | Captura, raro, lixo, combo x5, timer, rewards |
| Migration **4→5** | `fishingHighscore` em `game_stats` |

## Economia

Pontos só no jogo; moedas finais: `computeCoinsEarned(score)` (faixa 5–55), sem farm por peixe.

## Mecânica

Lançar → anzol desce; tocar → sobe; captura na subida; lixo zera combo; multiplicador máx. x5.
