# Ajuste dos limites visuais do DayPeriod

**Data:** 2026-08-21  
**Escopo:** só ciclo visual + testes de fronteira. Sono 22:00–07:30 intacto. Sem redesenho de cenários.

## Novos limites

| Período | Antes | Depois |
|---------|-------|--------|
| MORNING | 07:30–11:59 | 07:30–11:59 |
| AFTERNOON | 12:00–17:59 | 12:00–17:29 |
| EVENING | 18:00–21:59 | 17:30–18:29 |
| NIGHT | 22:00–07:29 | 18:30–07:29 |

## Arquivos

- `DayPeriod.kt` — comentários
- `GameTimeManager.kt` — `periodAt` + fronteiras 17:30 / 18:30
- `PlayLocationRules.kt` — bloqueio via sono obrigatório (`inMandatorySleepHours`), não via NIGHT visual
- `GameTimeManagerTest.kt` — 17:29, 17:30, 18:29, 18:30, 21:59, 22:00, 07:29, 07:30
