package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodBonusResolverTest {

    private val fish = ShopCatalog.findItemById("food_fish")!!
    private val bamboo = ShopCatalog.findItemById("food_bamboo")!!

    @Test
    fun favorite_addsHappinessAndExp_withoutChangingHunger() {
        val resolved = FoodBonusResolver.resolve(Species.GATO, fish)

        assertTrue(resolved.wasFavorite)
        assertEquals(fish.hungerBoost, resolved.hungerGain)
        assertEquals(fish.happinessBoost + FoodPreferenceCatalog.FAVORITE_HAPPINESS_BONUS, resolved.happinessGain)
        assertEquals(FoodBonusResolver.applyExpBonus(fish.expBoost), resolved.expGain)
        assertEquals(12, resolved.expGain) // ceil(10 * 1.2)
    }

    @Test
    fun nonFavorite_keepsBaseValues() {
        val resolved = FoodBonusResolver.resolve(Species.GATO, bamboo)

        assertFalse(resolved.wasFavorite)
        assertEquals(bamboo.hungerBoost, resolved.hungerGain)
        assertEquals(bamboo.happinessBoost, resolved.happinessGain)
        assertEquals(bamboo.expBoost, resolved.expGain)
    }

    @Test
    fun basicSnack_isNeverFavorite() {
        val resolved = FoodBonusResolver.resolve(Species.PANDA, null)

        assertFalse(resolved.wasFavorite)
        assertEquals(25, resolved.hungerGain)
        assertEquals(10, resolved.happinessGain)
        assertEquals(10, resolved.expGain)
    }

    @Test
    fun applyExpBonus_roundsUp() {
        assertEquals(6, FoodBonusResolver.applyExpBonus(5)) // ceil(6.0)
        assertEquals(12, FoodBonusResolver.applyExpBonus(10))
        assertEquals(18, FoodBonusResolver.applyExpBonus(15))
    }

    @Test
    fun happinessBaseTen_becomesTwenty() {
        val cookie = ShopCatalog.findItemById("food_cookie")!!
        val resolved = FoodBonusResolver.resolve(Species.CACHORRO, cookie)
        assertTrue(resolved.wasFavorite)
        assertEquals(cookie.happinessBoost + 10, resolved.happinessGain)
        assertEquals(cookie.hungerBoost, resolved.hungerGain)
    }
}
