# Organização da aba Alimentos (Loja / Mochila)

**Data:** 2026-08-17  
**Status:** implementado

## Problema

Os 22 alimentos apareciam juntos na aba Alimentos.

## Solução

- `FoodShopOrganization` + `FoodShopSubcategory` / `FoodShelfGroup`
- Chips horizontais: Favoritos | Saudáveis | Refeições | Sobremesas | Bebidas | Especiais
- Favoritos dinâmicos via `FoodPreferenceCatalog` (espécie atual)
- Mesma filtragem na Mochila (só itens possuídos)
- Fallback: “Nenhum alimento favorito encontrado para esta espécie.”

## Não alterado

`FoodPreferenceCatalog`, `FoodBonusResolver`, `FeedOutcome`, bônus, Species, Room, saúde.
