# Hierarquia visual da Cozinha (z-index)

**Data:** 2026-08-18  
**Status:** implementado

## Problema

Janela central coberta pelo balão de diálogo; balão ocupava área grande no topo.

## Ajustes

1. Janela movida para o **canto superior direito** (menor).
2. Removido armário superior direito que competia com a janela.
3. Balão: faixa reservada (~14.5% da altura), largura/padding ~12–13% menores, `maxLines = 3`.
4. Ordem Compose: cenário → pill → **pet** → **balão** (camadas 4 e 5).
5. Canvas: parede/piso → janela → móveis.

## Arquivos

- `KitchenScenery.kt`
- `HomeScreen.kt` (`PetLivingStage`)
