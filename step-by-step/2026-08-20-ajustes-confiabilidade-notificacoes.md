# Ajustes finais — confiabilidade das notificações

**Data:** 2026-08-20  
**Status:** concluído (testes verdes)

## Objetivo

Corrigir confiabilidade e consistência do sistema de notificações **sem** alterar regras de gameplay (fome, higiene, energia, saúde, doenças, sono, evolução, UI, Room, médico, moedas, áudio).

## Arquivos alterados

| Arquivo | Função |
|---------|--------|
| `app/src/main/.../PetStatsCalculator.kt` | `ScheduleEstimate`; saúde no próximo agendamento; clamp 15 (care) / 5 (crítico) |
| `app/src/main/.../PetCareScheduler.kt` | shortcut crítico; logs SCHEDULE/SCHEDULER; catch não silencioso |
| `app/src/main/.../PetCareWorker.kt` | saúde fora do cap diário; logs WORKER/BLOCKED/SENT |
| `app/src/main/.../NotificationHelper.kt` | logs PERMISSION_DENIED / SENT / quiet hours |
| `app/src/main/.../NotificationPreferences.kt` | (já existente) `canSendCareNotificationToday` só para care |
| `app/src/test/.../NotificationSystemTest.kt` | cobertura dos cenários pedidos |
| `step-by-step/2026-08-20-ajustes-confiabilidade-notificacoes.md` | este documento |

## Antes → Depois

| Item | Antes | Depois |
|------|-------|--------|
| Scheduler saúde | Não projetava INDISPOSTO/DOENTE/CRITICO | Inclui limiares + dano quando fome/higiene ≤ 20 |
| Doente/crítico/doença | Delay mín. 15 min | Shortcut **5 min** se alerta de saúde ainda pendente |
| Cap diário (=3) | Podia bloquear saúde indiretamente | Só fome/higiene/energia/saudade; saúde é canal separado |
| catch `scheduleNextCheck` | Silencioso | `Log.e(PET_NOTIFICATION_SCHEDULER, …)` com petId/delay/reason |
| Diagnóstico | Ausente | Tags SCHEDULE / WORKER / SENT / BLOCKED |
| Quiet hours | 22–08 | Mantido; reagenda `QUIET_UNTIL_08` |

## Anti-spam (regra final)

1. **Cuidados comuns** (fome, higiene, energia, saudade):
   - flag por necessidade (`hasNotifiedHunger` etc.) — Single Alert Guarantee
   - teto **`MAX_DAILY_CARE_NOTIFICATIONS = 3`** por dia
2. **Saúde grave/crítica / doença ativa**:
   - **não** consome o teto de 3
   - Single Alert via `hasNotifiedHealth` (mesma condição não dispara em loop)
   - reset em tratamento médico (`onPetDoctorTreated`)
3. **Noite 22:00–08:00**: sem alertas normais; worker/scheduler reagendam para ~08:00

## Como saúde entrou no scheduler

`estimateMinutesUntilNextThreshold()`:

1. Se quiet hours → `QUIET_UNTIL_08`
2. Se `allowCriticalHealthShortcut` e (DOENTE \| CRITICO \| doença) → candidato `HEALTH_URGENT` @ 5 min
3. Candidatos care: fome/higiene/energia/saudade (como antes)
4. `addHealthScheduleCandidates()`:
   - doença ativa → `HEALTH_DISEASE` (agora)
   - estados atuais INDISPOSTO/DOENTE/CRITICO
   - projeção de cruzamento 60 / DOENTE_MAX / CRITICO_MAX considerando dano −1/10 min quando fome ou higiene ≤ 20
5. Clamp: care ≥ 15 min; saúde crítica com shortcut ≥ 5 min (sem polling contínuo)

`PetCareScheduler` liga o shortcut quando `isHealthEnabled && !hasNotifiedHealth` e o pet já está doente/crítico/com doença.

## Logs (Logcat)

```
PET_NOTIFICATION_SCHEDULE: scheduled type=HUNGER delay=42min petId=1 …
PET_NOTIFICATION_WORKER: worker started
PET_NOTIFICATION_SENT: sent type=HEALTH disease=INDIGESTAO
PET_NOTIFICATION_BLOCKED: blocked reason=QUIET_HOURS
PET_NOTIFICATION_BLOCKED: blocked reason=DAILY_LIMIT
PET_NOTIFICATION_BLOCKED: blocked reason=PERMISSION_DENIED type=HUNGER
PET_NOTIFICATION_SCHEDULER: scheduleNextCheck failed petId=… delay=… reason=… ex=…
```

## Testes

Classe: `com.example.notification.NotificationSystemTest` (`@Config(sdk = [34])`)

Cenários cobertos:

- Fome ≤ 20 / higiene ≤ 20 / energia ≤ 15 (acordado)
- Saúde entrando em DOENTE
- Doença ativa com saúde alta → delay crítico (`HEALTH_DISEASE`, 5 min com shortcut)
- Saúde crítica após 3 cares do dia ainda pode alertar (canal separado)
- Mesma doença: single alert + reset no médico
- 22–08 não agenda care “agora”; às 08:00 necessidade volta
- Permissão negada → log `PERMISSION_DENIED`
- Erro do scheduler → tag `PET_NOTIFICATION_SCHEDULER` com contexto

Comando:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.notification.NotificationSystemTest"
```

## Manutenibilidade

A separação **care vs saúde** no anti-spam e o `ScheduleEstimate(reason)` deixam o diagnóstico por Logcat previsível sem mudar o motor de stats. O shortcut de 5 min evita “esperar o próximo care” em crise, sem virar polling. Próximo passo opcional: extrair um `NotificationDiagnostics` fino se mais callers precisarem das mesmas tags.
