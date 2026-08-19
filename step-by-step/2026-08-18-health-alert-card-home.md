# Health Alert Card na Home

**Data:** 2026-08-18  
**Status:** implementado

## Objetivo

Comunicar doença/crítico de forma persistente, sem depender do balão de fala.

## UI

- `HealthAlertCard` abaixo da barra de cômodos (portrait) / acima do pet (landscape)
- Clique → `DoctorCheckupDialog` (Clínica)
- Pulso lento na borda (~1,6 s)
- Some ao curar / recuperar
- Barra de Saúde: `100/100 • 🤢 Indigestão` se houver doença nomeada

## Arquivos

- `BadgesAndBars.kt` — card + helpers `shouldShowHealthAlert` / `healthBarStatusSuffix`
- `HomeScreen.kt` — posicionamento + abertura da clínica
- Sem mudanças em regras de doença / médico / remédios
