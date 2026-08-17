# Transições temporárias dos cômodos → Sala

**Data:** 2026-08-17  
**Status:** implementado

## Objetivo

Reutilizar o padrão de retorno automático do Banheiro para Cozinha, Quintal e Garagem, sem alterar o fluxo do Banheiro nem o Quarto (sono).

## Diagnóstico

### Por que o Banheiro funcionava

Em `batheInBathroom`:

1. Captura `prev = currentRoom` **antes** de ir ao banheiro.
2. Define `currentRoom = BATHROOM`.
3. Após `delay(2800)`, só volta se ainda estiver no banheiro **e** `prev != BATHROOM`.

O timer começa com o cômodo de origem correto; a condição de retorno é válida.

### Por que Cozinha / Quintal falhavam

Ao abrir o diálogo, o app já mudava para `KITCHEN` / `BACKYARD`. Um timer no estilo banheiro capturava `previousRoom == KITCHEN` (ou BACKYARD), então a condição `previousRoom != KITCHEN` falhava e **nunca** voltava para a Sala.

### Por que a Garagem falhava

Não havia vínculo explícito entre “sair do fluxo de minijogo” e `LIVING_ROOM`. Dependência de lifecycle da Activity (`ON_RESUME`) não dispara ao voltar no NavHost Compose.

## Solução

### Mecanismo comum: `TemporaryRoomSession`

Arquivo: `app/src/main/java/com/example/ui/rooms/TemporaryRoomSession.kt`

| API | Função |
|-----|--------|
| `enter(room, timeoutMs)` | Vai ao cômodo e inicia timer |
| `bump(room, timeoutMs)` | Reinicia timer (nova alimentação/brincadeira) |
| `goToWithoutTimer(room)` | Só muda o cômodo (ex.: abrir diálogo) |
| `returnToLivingRoomNow()` | Cancela timer e volta à Sala |
| `cancelTimer()` / `hasActiveTimer()` | Cancelamento e checagem |

Timeouts:

- Cozinha: animação ~3,2s + 5s = **8200 ms**
- Quintal: animação ~3,4s + 20s = **23400 ms**

### Integração em `HomeScreen`

- Cozinha: `openKitchenForFeed` → diálogo; `onFeedItem` → `bump(KITCHEN)`; dismiss sem timer ativo → Sala.
- Quintal: idem com `BACKYARD` e 20s.
- Banheiro: **inalterado** (`batheInBathroom` local).
- Quarto: só via `isSleeping` / `toggleSleepRoom`; timers cancelados ao dormir.
- Garagem: `openGarageMinigames` + flag em `MainApp` (`pendingLivingRoomAfterMinigames`) → `forceReturnToLivingRoom` ao voltar para Home.

### Arquivos alterados

1. `app/src/main/java/com/example/ui/rooms/TemporaryRoomSession.kt` *(novo)*
2. `app/src/main/java/com/example/ui/screens/HomeScreen.kt`
3. `app/src/main/java/com/example/ui/MainApp.kt`
4. `step-by-step/2026-08-17-transicoes-comodos-temporarios.md` *(este)*

## Testes (checklist)

| # | Cenário | Resultado |
|---|---------|-----------|
| 1 | Banho → Banheiro → Sala | Lógica inalterada (`batheInBathroom`, 2800 ms) — esperado OK |
| 2 | Alimentar 1× → Cozinha → Sala | `bump` + 8200 ms; dismiss pós-feed **não** cancela (via `hasActiveTimer`) |
| 3 | Alimentar várias× | `bump` cancela job anterior e reinicia |
| 4 | Brincadeira rápida → 20s → Sala | `BACKYARD_AFTER_PLAY_MS` = 23400 |
| 5 | Brincar de novo antes de 20s | `bump` reinicia |
| 6 | Garagem → finalizar → Sala | `pendingLivingRoomAfterMinigames` + `forceReturnToLivingRoom` |
| 7 | Garagem → cancelar (voltar) → Sala | Mesmo sinal ao retornar à rota Home |
| 8 | Sono → Quarto | `enter`/`bump` ignoram se `isSleeping`; `LaunchedEffect` força BEDROOM |
| 9 | Troca rápida de ações | `cancelTimer` / restart em um único `Job` |

**Compilação:** `:app:compileDebugKotlin` — SUCCESS.

**Testes unitários novos:** `TemporaryRoomSessionTest` (suite completa de unit tests ainda falha por erros pré-existentes em `PetEvolutionTest`, fora do escopo).

## Manutenibilidade

`TemporaryRoomSession` centraliza cancelamento e retorno, evitando três implementações divergentes. O Banheiro permanece isolado de propósito (já estável). Próximo passo opcional: migrar o Banheiro para a mesma API (`enter` com timeout 2800) quando houver tempo de regressão visual.

