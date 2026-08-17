# Preferências alimentares por espécie

**Data:** 2026-08-17  
**Status:** implementado

## O que foi feito

- `FoodPreferenceCatalog` — fonte única (15 espécies × 3 favoritos).
- `FoodBonusResolver` — +10 felicidade, +20% XP; hunger intacto.
- `FeedOutcome` — `success` + `wasFavorite`.
- `feedPet` usa o resolver; toast especial no ViewModel.
- 17 novos alimentos no `ShopCatalog` (mantidos os 5 genéricos).
- Badge ❤️ / “Favorito” na Loja e na Mochila para a espécie atual.

## Ajustes de tabela aplicados

- Coruja: `food_night_bites` (não `food_mouse_snack`).
- Leão: `food_meat`, `food_steak`, `food_fish`.
- Fênix: `food_berries`, `food_honey`, `food_sun_nectar`.

## Arquivos

| Arquivo | Função |
|---------|--------|
| `FoodPreferenceCatalog.kt` | Mapa espécie → favoritos + constantes de bônus |
| `FoodBonusResolver.kt` | Cálculo dos deltas de feed |
| `FeedOutcome.kt` | Resultado de `feedPet` |
| `ShopItem.kt` | Catálogo com 22 alimentos |
| `PetRepository.kt` | Integração mínima em `feedPet` |
| `PetViewModel.kt` | Toast de favorito |
| `ShopScreen.kt` / `InventoryScreen.kt` | Indicador visual |
| `FoodPreferenceCatalogTest.kt` / `FoodBonusResolverTest.kt` | Testes do plano |

## Não alterado

Nascimento, raridade, doenças, evolução, schema Room, `Species.kt` (exceto uso via `fromId`).
