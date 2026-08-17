package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodPreferenceCatalogTest {

    @Test
    fun everySpecies_hasExactlyThreeFavorites() {
        Species.entries.forEach { species ->
            val favorites = FoodPreferenceCatalog.favoritesFor(species)
            assertEquals(
                "Espécie ${species.id} deve ter exatamente 3 favoritos",
                3,
                favorites.size
            )
            assertEquals(
                "Favoritos de ${species.id} não devem ter IDs duplicados",
                3,
                favorites.toSet().size
            )
        }
    }

    @Test
    fun everyFavorite_existsInShopCatalog() {
        val shopIds = ShopCatalog.items.map { it.id }.toSet()
        Species.entries.forEach { species ->
            FoodPreferenceCatalog.favoritesFor(species).forEach { foodId ->
                assertTrue(
                    "Favorito $foodId de ${species.id} deve existir no ShopCatalog",
                    shopIds.contains(foodId)
                )
                val item = ShopCatalog.findItemById(foodId)
                assertNotNull(item)
                assertEquals(ItemCategory.ALIMENTO, item!!.category)
            }
        }
    }

    @Test
    fun isFavorite_matchesCatalog() {
        assertTrue(FoodPreferenceCatalog.isFavorite(Species.GATO, "food_fish"))
        assertFalse(FoodPreferenceCatalog.isFavorite(Species.GATO, "food_bamboo"))
        assertFalse(FoodPreferenceCatalog.isFavorite(Species.GATO, null))
        assertFalse(FoodPreferenceCatalog.isFavorite(Species.GATO, ""))
    }

    @Test
    fun adjustedTable_owlLionPhoenix() {
        assertEquals(
            listOf("food_fish", "food_meat", "food_night_bites"),
            FoodPreferenceCatalog.favoritesFor(Species.CORUJA)
        )
        assertEquals(
            listOf("food_meat", "food_steak", "food_fish"),
            FoodPreferenceCatalog.favoritesFor(Species.LEAO)
        )
        assertEquals(
            listOf("food_berries", "food_honey", "food_sun_nectar"),
            FoodPreferenceCatalog.favoritesFor(Species.FENIX)
        )
    }

    @Test
    fun shopHasFiveGenericsAndSeventeenNewFoods() {
        val foods = ShopCatalog.items.filter { it.category == ItemCategory.ALIMENTO }
        val genericIds = setOf(
            "food_apple", "food_cookie", "food_fish", "food_meat", "food_pizza"
        )
        val preferenceEraIds = setOf(
            "food_tuna", "food_bone", "food_carrot", "food_lettuce", "food_seeds",
            "food_corn", "food_berries", "food_seaweed", "food_night_bites",
            "food_bamboo", "food_bamboo_shoot", "food_steak", "food_shrimp",
            "food_chili", "food_honey", "food_ember_fruit", "food_sun_nectar"
        )
        assertTrue(foods.map { it.id }.containsAll(genericIds))
        assertTrue(foods.map { it.id }.containsAll(preferenceEraIds))
        assertEquals(41, foods.size)
    }
}
