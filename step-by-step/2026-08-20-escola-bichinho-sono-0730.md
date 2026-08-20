# Escola do Bichinho + sono 22:00–07:30

**Data:** 2026-08-20  
**Status:** implementado

## Objetivo

Sistema escolar para FILHOTE/JOVEM com congelamento de atributos online/offline, recompensa única por turno, e mudança do sono noturno de **08:00** para **07:30**.

## Migration

- **Room version 3 → 4** (`MIGRATION_3_4`)
- Colunas: `isAtSchool`, `schoolEndTimestamp`, `lastSchoolRewardEndTimestamp`
- Sem `fallbackToDestructiveMigration`

## Arquivos principais

| Arquivo | Função |
|---------|--------|
| `PetSchoolRules.kt` | Turnos 08–12 / 13–17, elegibilidade, mín. 15 min |
| `Entities.kt` / `AppDatabase.kt` | Persistência + migration |
| `PetStatsCalculator.kt` | Freeze escolar + noite até 07:30 |
| `PetRepository.kt` | `sendPetToSchool` / `completeSchoolSession` / tick+offline |
| `PetCareWorker` / `NotificationHelper` | Suspende care na escola; quiet = noite |
| `HomeScreen` / `SchoolScenery` / `MainApp` | Ação Escola + cenário |
| `SchoolSystemTest` / `SleepSystemTest` | Cobertura obrigatória |

## Sono (nova regra)

- **22:00 → 07:30** protegido
- Às **07:30** acorda automaticamente (mesmo com energia < 100%)
- Escola da manhã disponível a partir das **08:00**

## Escola online/offline

1. `sendPetToSchool` grava `isAtSchool` + `schoolEndTimestamp`
2. Enquanto `now < schoolEnd`: sem decay (live tick e simulação offline)
3. Ao cruzar o fim: limpa flags, +10 XP, +5 felicidade, +5 moedas (1× via `lastSchoolRewardEndTimestamp`)
4. Toast: `🎒 Voltei da escola!` + reagenda notificações

## Anti-notificação na escola

Scheduler retorna `SCHOOL_UNTIL_END`; worker bloqueia com `AT_SCHOOL`. Saúde crítica fora do escopo de “não alterar notificações fora do período escolar” — care comum fica suspenso.
