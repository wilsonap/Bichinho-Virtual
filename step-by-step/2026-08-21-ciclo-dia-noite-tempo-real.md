# Ciclo dia/noite em tempo real + clima visual

**Data:** 2026-08-21  
**Status:** implementado (limites visuais ajustados no mesmo dia)

## Fonte única

`com.example.time.GameTimeManager` — `DayPeriod` + `WeatherState` (**somente ciclo visual**).

| Período | Horário local (visual) |
|---------|------------------------|
| MORNING | 07:30–11:59 |
| AFTERNOON | 12:00–17:29 |
| EVENING | 17:30–18:29 |
| NIGHT | 18:30–07:29 |

Sono obrigatório permanece **22:00–07:30** (`PetStatsCalculator`) — independente do visual.

Exemplos: 19:00 / 21:30 = NIGHT visual + pet acordado; 22:00 = NIGHT + dormindo; 07:30 = MORNING + acordar.

Atualização: ao abrir, ao voltar do background (`onAppForegrounded`), e loop até a próxima fronteira visual (07:30, 12:00, 17:30, 18:30; máx. 60s). Sem recálculo por frame.

## Brincar (externo)

- Manhã/tarde + limpo → Quintal  
- Entardecer / noite visual (acordado) → Garagem (+ mensagem)  
- Chuva → Garagem (interno)  
- Sono obrigatório 22:00–07:30 ou `isSleeping` → bloqueado  
- Bloqueio **não** usa `DayPeriod.NIGHT` sozinho (senão 18:30–22:00 bloquearia pet acordado)

## Visual

`OutdoorAmbience` pinta céu/janelas; cômodos reutilizam móveis. Quintal: só camadas de céu/luz/clima sobre o layout fixo.

## Arquivos

Criados: `DayPeriod.kt`, `GameTimeManager.kt`, `PlayLocationRules.kt`, `OutdoorAmbience.kt`, `GameTimeManagerTest.kt`  
Alterados: cenários (janelas), `RoomSceneryRenderer`, `HomeScreen`, `MainApp`, `PetViewModel`, etc.
