package com.example.data.model

import kotlin.math.ceil

/**
 * Aplica bônus de alimento favorito sem alterar hungerBoost.
 */
object FoodBonusResolver {

    data class ResolvedBonuses(
        val hungerGain: Int,
        val happinessGain: Int,
        val expGain: Int,
        val energyGain: Int,
        val healthGain: Int,
        val wasFavorite: Boolean
    )

    fun resolve(
        species: Species,
        shopItem: ShopItem?
    ): ResolvedBonuses {
        val hungerGain = shopItem?.hungerBoost ?: 25
        val energyGain = shopItem?.energyBoost ?: 5
        val healthGain = shopItem?.healthBoost ?: 5
        var happinessGain = shopItem?.happinessBoost ?: 10
        var expGain = shopItem?.expBoost ?: 10

        val wasFavorite = shopItem != null &&
            FoodPreferenceCatalog.isFavorite(species, shopItem.id)

        if (wasFavorite) {
            happinessGain += FoodPreferenceCatalog.FAVORITE_HAPPINESS_BONUS
            expGain = applyExpBonus(expGain)
        }

        return ResolvedBonuses(
            hungerGain = hungerGain,
            happinessGain = happinessGain,
            expGain = expGain,
            energyGain = energyGain,
            healthGain = healthGain,
            wasFavorite = wasFavorite
        )
    }

    fun applyExpBonus(baseExp: Int): Int =
        ceil(baseExp * FoodPreferenceCatalog.FAVORITE_EXP_MULTIPLIER).toInt()
}
