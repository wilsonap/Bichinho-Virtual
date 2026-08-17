# Expansão do catálogo de alimentos

**Data:** 2026-08-17  
**Status:** implementado

## Inclusão

+19 alimentos compráveis na Loja (não no inventário inicial, não favoritos automáticos).

Total de alimentos no catálogo: **41** (22 anteriores + 19 novos).

## Arquivos

- `ShopItem.kt` / `ShopCatalog` — novos `ShopItem` com `ItemCategory.ALIMENTO`
- `FoodShopOrganization.kt` — prateleiras SAUDAVEL / REFEICAO / SOBREMESA / BEBIDA
- Testes atualizados em `FoodShopOrganizationTest` e `FoodPreferenceCatalogTest`

## Intactos

`FoodPreferenceCatalog`, `FoodBonusResolver`, `FeedOutcome`, Room, saúde, evolução, inventário inicial.
