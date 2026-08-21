# Melhoria visual da Pescaria

**Data:** 2026-08-21  
**Escopo:** só apresentação. Engine / pontuação / combo / timer / economia intactos.

## Pet real

- Removido círculo azul genérico.
- `PetCanvasRenderer` com `PetBehaviorState.SENTADO` na margem.
- `pet.copy(isSleeping = false)` só para exibição no minijogo.
- Espécie, estágio e equipamentos via renderer existente.

## Cenário

- `FishingLakeRenderer`: céu (DayPeriod/Weather via OutdoorAmbience), nuvens/sol/lua, grama, barranco, juncos, lago com gradiente/ondas/bolhas/pedras.
- `FishingCatchRenderer`: corpo + emoji + glow raro/incomum; `spriteKey()` para sprites futuros.

## UX

- HUD em chips compactos.
- Botão menor (canto inferior direito): Lancar / Recolher.
- Feedback “Fisgou!” + splash no Canvas.
- MainApp passa `pet`, `gameTime.period`, `gameTime.weather`.
