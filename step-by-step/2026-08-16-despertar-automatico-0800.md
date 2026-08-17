# Step-by-step: Despertar automático às 08:00 (paridade live/offline)

**Data:** 2026-08-16  
**Objetivo:** Unificar o despertar às 08:00 entre aplicativo aberto e fechado.

## Problema identificado

| Modo | Comportamento anterior |
|------|-------------------------|
| Offline (`PetStatsCalculator.calculateSimulatedStats`) | Às 08:00 forçava `isSleeping = false` na transição noite→dia |
| Live (`PetRepository.tickLiveStats`) | No dia, só acordava se `energy >= 100` (regra de sono por exaustão) |

Com o app aberto, o pet podia continuar dormindo após as 08:00 até a energia chegar a 100%.

## Correção aplicada

### Arquivo: `PetRepository.kt` — função `tickLiveStats`

1. Parâmetro opcional `now: Long = System.currentTimeMillis()` (testabilidade; default inalterado).
2. No ramo **diurno**, antes do sono por exaustão:

```text
se isSleeping && lastUpdateTimestamp estava na janela noturna
  → isSleeping = false
```

Isso espelha o offline (`wasNightInPreviousMinute`), **sem** chamar `wakeUpPet()` (evita bônus de energia indevido).

### O que NÃO foi alterado

- Sono por exaustão (`energy <= 5`)
- Recuperação de energia no sono diurno / noturno
- Proteção noturna (fome lenta, higiene/felicidade/saúde)
- Notificações (`PetCareWorker`)
- Cálculo offline (`PetStatsCalculator`)

## Testes adicionados em `SleepSystemTest.kt`

| Teste | Cenário |
|-------|---------|
| `testLiveTickAutoAwakeningAt0800RegardlessOfEnergy` | App aberto: 07:59 → 08:00, energia 40 → acorda |
| `testOfflineAutoAwakeningAt0800RegardlessOfEnergy` | App fechado: mesmo horário/energia → acorda |
| `testLiveTickDoesNotForceWakeDuringDaytimeExhaustionNap` | Sono diurno por exaustão permanece até 100% |

## Função / utilidade dos arquivos tocados

| Arquivo | Função |
|---------|--------|
| `PetRepository.kt` | Persistência e tick live de stats; agora força despertar na transição 08:00 |
| `SleepSystemTest.kt` | Regressão do sistema de sono (live + offline + exaustão) |
| Este documento | Registro do avanço e da decisão de design |

## Regra final unificada

- **22:00–08:00:** horário controla o sono (`isSleeping = true`).
- **A partir de 08:00:** horário deixa de controlar; transição força despertar; em seguida valem só as regras diurnas (exaustão / 100%).
