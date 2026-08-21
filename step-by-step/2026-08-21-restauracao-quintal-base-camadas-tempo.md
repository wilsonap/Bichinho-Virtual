# Restauração do QuintalBase + camadas de tempo/clima

**Data:** 2026-08-21  
**Objetivo:** Desfazer o redesenho indevido do Quintal introduzido com o ciclo dia/noite, sem remover `DayPeriod` / `WeatherState` / `GameTimeManager`.

## Problema

A implementação do ciclo de tempo havia **reescrito** `BackyardScenery.kt` (novo layout), em vez de sobrepor luz/clima ao cenário já existente.

## Comparação Git

- Fonte da verdade do layout: `HEAD` (`git checkout HEAD -- BackyardScenery.kt`).
- Diff anterior: assinatura + céu/clima reescritos e estrutura do quintal alterada.
- Cômodos internos (`HouseRoomsScenery`, `Kitchen`, `Bathroom`, `RoomSceneryRenderer`): só janelas passaram a usar `OutdoorAmbience.drawWindowExterior` — móveis intactos.

## Arquitetura restaurada

```
QuintalBase (cerca, gramado, árvore, balanço, banco, flores, lago, caminho)
+ DayLightingLayer (céu / sol / lua / estrelas via OutdoorAmbience.theme)
+ WeatherLayer (nuvens / chuva / poças leves / outdoorDim)
```

## Arquivos

| Arquivo | Função |
|---------|--------|
| `BackyardScenery.kt` | Layout original restaurado; sky/nuvens/chuva/dim dinâmicos |
| `OutdoorAmbience.kt` | Tema compartilhado; AFTERNOON alinhado ao céu diurno original do Quintal |
| Cômodos internos | Sem alteração de móveis; janelas já dinâmicas |

## Restaurado (fixo)

Árvore + balanço, cerca, gramado, caminho de pedras, jardim direito (banco/flores/arbustos), lago, proporções/`horizonY`, posição relativa do pet.

## Dinâmico (ciclo de tempo)

Gradiente do céu, sol (altura/cor), lua + estrelas, nuvens, chuva + poças leves, `outdoorDim`, borboletas só com céu limpo e não-noite.

## Não feito

- Rollback de `GameTimeManager` / regras de brincar / escola / sono.
- Redesign de sala, quarto, cozinha, banheiro, garagem.
