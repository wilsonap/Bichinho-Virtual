# Comunicação do estado de doença

**Data:** 2026-08-18  
**Status:** implementado

## Escopo

Apenas feedback visual/sonoro/textual. **Sem** alterar causas, thresholds, medicamentos, médico, evolução, moedas ou inventário.

## Mudanças

| Área | Antes | Depois |
|------|-------|--------|
| Olhos | “X” (aparência de morte) | Semicerrados/cansados |
| Doente | Só olhos X | Olhos cansados + curativo 🩹 |
| Crítico | Igual doente | + suor + aura vermelha suave |
| Transição | Silenciosa / SFX no toque | Toast único + `PET_SICK` uma vez |
| Toque no pet doente | `PET_SICK` repetido | `PET_SAD` + fala contextual |
| HUD | `Saúde 32%` | `Saúde • 32/100 • Doente` |
| Badge | Ausente | Badge permanente Doente/Crítico |
| Notificação | Texto genérico | `🤒 [Nome] não está se sentindo bem...` |

## Arquivos

- `PetHealth.kt` — helpers de transição (sem mudar limiares)
- `PetCanvasRenderer.kt` — expressão / curativo
- `PetViewModel.kt` — toast/SFX/falas
- `BadgesAndBars.kt` — `HealthConditionBadge` + suffix na barra
- `HomeScreen.kt` — badge + status
- `PetCareWorker.kt` / `NotificationHelper.kt` — texto da notificação
- `PetHealthCommunicationTest.kt`

## Anti-spam

- Aviso/SFX só em `Saudável|Indisposto → Doente|Crítico`
- Notificação local respeita `hasNotifiedHealth`, quiet hours 22h–08h
- Cura/recuperação chama `onPetDoctorTreated()` para liberar próximo alerta
