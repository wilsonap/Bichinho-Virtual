package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodShopOrganizationTest {

    @Test
    fun catalogHasExactly41Foods_withoutDuplicates() {
        val foods = ShopCatalog.items.filter { it.category == ItemCategory.ALIMENTO }
        assertEquals(41, foods.size)
        assertEquals(41, foods.map { it.id }.toSet().size)
        assertEquals(41, FoodShopOrganization.totalFoodItemsInCatalog())
    }

    @Test
    fun everyFoodHasExactlyOneShelfGroup() {
        val foods = ShopCatalog.items.filter { it.category == ItemCategory.ALIMENTO }
        foods.forEach { food ->
            val group = FoodShopOrganization.shelfGroupOf(food.id)
            assertTrue("Alimento ${food.id} deve ter prateleira", group != null)
        }

        val assigned = FoodShelfGroup.entries.sumOf { FoodShopOrganization.foodsInShelf(it).size }
        assertEquals(41, assigned)
    }

    @Test
    fun newExpansionFoods_areOnCorrectShelves_andNotAutoFavorites() {
        val expected = mapOf(
            "food_banana" to FoodShelfGroup.SAUDAVEL,
            "food_strawberry" to FoodShelfGroup.SAUDAVEL,
            "food_watermelon" to FoodShelfGroup.SAUDAVEL,
            "food_grapes" to FoodShelfGroup.SAUDAVEL,
            "food_salad" to FoodShelfGroup.SAUDAVEL,
            "food_soup" to FoodShelfGroup.SAUDAVEL,
            "food_natural_sandwich" to FoodShelfGroup.SAUDAVEL,
            "food_omelet" to FoodShelfGroup.SAUDAVEL,
            "food_roast_chicken" to FoodShelfGroup.REFEICAO,
            "food_lasagna" to FoodShelfGroup.REFEICAO,
            "food_burger" to FoodShelfGroup.REFEICAO,
            "food_barbecue" to FoodShelfGroup.REFEICAO,
            "food_ice_cream" to FoodShelfGroup.SOBREMESA,
            "food_cake" to FoodShelfGroup.SOBREMESA,
            "food_donut" to FoodShelfGroup.SOBREMESA,
            "food_pudding" to FoodShelfGroup.SOBREMESA,
            "drink_natural_juice" to FoodShelfGroup.BEBIDA,
            "drink_hot_chocolate" to FoodShelfGroup.BEBIDA,
            "drink_coffee_milk" to FoodShelfGroup.BEBIDA
        )

        expected.forEach { (id, shelf) ->
            val item = ShopCatalog.findItemById(id)
            assertTrue("Item $id deve existir", item != null)
            assertEquals(ItemCategory.ALIMENTO, item!!.category)
            assertEquals(shelf, FoodShopOrganization.shelfGroupOf(id))
            Species.entries.forEach { species ->
                assertFalse(
                    "$id não deve ser favorito automático de ${species.id}",
                    FoodPreferenceCatalog.isFavorite(species, id)
                )
            }
        }
    }

    @Test
    fun newExpansionFoods_haveExpectedPriceAndHunger() {
        assertEquals(8, ShopCatalog.findItemById("food_banana")!!.price)
        assertEquals(15, ShopCatalog.findItemById("food_banana")!!.hungerBoost)
        assertEquals(12, ShopCatalog.findItemById("food_strawberry")!!.price)
        assertEquals(18, ShopCatalog.findItemById("food_strawberry")!!.hungerBoost)
        assertEquals(2, ShopCatalog.findItemById("food_strawberry")!!.happinessBoost)
        assertEquals(50, ShopCatalog.findItemById("food_barbecue")!!.price)
        assertEquals(50, ShopCatalog.findItemById("food_barbecue")!!.hungerBoost)
        assertEquals(20, ShopCatalog.findItemById("drink_coffee_milk")!!.price)
        assertEquals(5, ShopCatalog.findItemById("drink_coffee_milk")!!.hungerBoost)
        assertEquals(20, ShopCatalog.findItemById("drink_coffee_milk")!!.energyBoost)
    }

    @Test
    fun foxFavorites_onlyThreeFoxFoods() {
        val ids = FoodShopOrganization
            .filterShopFoods(FoodShopSubcategory.FAVORITOS, Species.RAPOSA.id)
            .map { it.id }
        assertEquals(listOf("food_berries", "food_fish", "food_meat"), ids)
        assertFalse(ids.contains("food_bamboo"))
        assertFalse(ids.contains("food_ember_fruit"))
    }

    @Test
    fun pandaFavorites_onlyThreePandaFoods() {
        val ids = FoodShopOrganization
            .filterShopFoods(FoodShopSubcategory.FAVORITOS, Species.PANDA.id)
            .map { it.id }
        assertEquals(listOf("food_bamboo", "food_bamboo_shoot", "food_apple"), ids)
        assertFalse(ids.contains("food_fish"))
    }

    @Test
    fun dragonFavorites_onlyThreeDragonFoods() {
        val ids = FoodShopOrganization
            .filterShopFoods(FoodShopSubcategory.FAVORITOS, Species.DRAGAO.id)
            .map { it.id }
        assertEquals(listOf("food_chili", "food_meat", "food_ember_fruit"), ids)
        assertFalse(ids.contains("food_sun_nectar"))
        assertFalse(ids.contains("food_berries"))
    }

    @Test
    fun favoritesNeverIncludeOtherSpeciesOnlyFoods() {
        Species.entries.forEach { species ->
            val favoriteIds = FoodShopOrganization
                .filterShopFoods(FoodShopSubcategory.FAVORITOS, species.id)
                .map { it.id }
                .toSet()
            assertEquals(3, favoriteIds.size)
            assertEquals(
                FoodPreferenceCatalog.favoritesFor(species).toSet(),
                favoriteIds
            )
        }
    }

    @Test
    fun favoriteAlsoAppearsInNormalShelf() {
        // food_fish is REFEICAO and favorite of RAPOSA
        val meals = FoodShopOrganization
            .filterShopFoods(FoodShopSubcategory.REFEICAO, Species.RAPOSA.id)
            .map { it.id }
        assertTrue(meals.contains("food_fish"))

        val favorites = FoodShopOrganization
            .filterShopFoods(FoodShopSubcategory.FAVORITOS, Species.RAPOSA.id)
            .map { it.id }
        assertTrue(favorites.contains("food_fish"))
    }

    @Test
    fun emptySpeciesId_favoritesEmpty() {
        assertTrue(
            FoodShopOrganization
                .filterShopFoods(FoodShopSubcategory.FAVORITOS, null)
                .isEmpty()
        )
    }
}
