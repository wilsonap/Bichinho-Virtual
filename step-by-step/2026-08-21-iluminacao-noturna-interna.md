# Correção da iluminação noturna interna

**Data:** 2026-08-21  
**Escopo:** só lógica de iluminação — sem redesenhar cômodos.

## Problema

`OutdoorAmbience.dimInterior(period, isSleeping)` fazia `NIGHT || isSleeping`, e o renderer passava isso como “paleta escura” para Sala, Quarto, Cozinha e Banheiro. Às 19:20 a casa inteira ficava escura.

## Separação

| Camada | Responsabilidade |
|--------|------------------|
| DayPeriod | Céu nas janelas / quintal |
| Weather | Sol, nuvens, chuva na janela |
| IndoorLighting | Lâmpadas artificiais (móveis claros à noite) |
| isSleeping | Paleta escura **só no quarto** durante o sono |

## Comportamento esperado

| Hora | Sala/Cozinha/Banheiro | Quarto |
|------|------------------------|--------|
| 19:20 acordado | Iluminado + janela noite | Iluminado + janela noite + brilho quente |
| 22:30 dormindo | (pet no quarto) | Paleta de sono + vinheta |

Quintal continua escuro no NIGHT visual. Garagem com luz artificial.

## Arquivos

- `IndoorLighting.kt` (novo)
- `RoomSceneryRenderer.kt` — deixa de aplicar dim de NIGHT nos internos
- `OutdoorAmbience.kt` — `dimInterior` = só sono; glow mais suave
- Sala/Garagem/Cozinha/Banheiro/Escola — overlays e regras alinhados
- `IndoorLightingTest.kt`
