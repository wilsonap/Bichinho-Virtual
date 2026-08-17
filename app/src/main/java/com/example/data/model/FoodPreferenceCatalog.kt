package com.example.data.model

/**
 * Fonte única de verdade: exatamente 3 alimentos favoritos por espécie.
 * Não altera raridade, nascimento nem atributos visuais de [Species].
 */
object FoodPreferenceCatalog {

    const val FAVORITE_HAPPINESS_BONUS = 10
    const val FAVORITE_EXP_MULTIPLIER = 1.20f

    private val favoritesBySpecies: Map<Species, List<String>> = mapOf(
        Species.GATO to listOf("food_fish", "food_meat", "food_tuna"),
        Species.CACHORRO to listOf("food_meat", "food_cookie", "food_bone"),
        Species.COELHO to listOf("food_carrot", "food_apple", "food_lettuce"),
        Species.HAMSTER to listOf("food_seeds", "food_apple", "food_corn"),
        Species.TARTARUGA to listOf("food_lettuce", "food_berries", "food_seaweed"),
        Species.CORUJA to listOf("food_fish", "food_meat", "food_night_bites"),
        Species.RAPOSA to listOf("food_berries", "food_fish", "food_meat"),
        Species.PANDA to listOf("food_bamboo", "food_bamboo_shoot", "food_apple"),
        Species.LOBO to listOf("food_meat", "food_bone", "food_fish"),
        Species.GUAXINIM to listOf("food_pizza", "food_cookie", "food_fish"),
        Species.TIGRE to listOf("food_meat", "food_fish", "food_steak"),
        Species.LEAO to listOf("food_meat", "food_steak", "food_fish"),
        Species.POLVO to listOf("food_fish", "food_shrimp", "food_seaweed"),
        Species.DRAGAO to listOf("food_chili", "food_meat", "food_ember_fruit"),
        Species.FENIX to listOf("food_berries", "food_honey", "food_sun_nectar")
    )

    fun favoritesFor(species: Species): List<String> =
        favoritesBySpecies[species] ?: emptyList()

    fun favoritesFor(speciesId: String): List<String> =
        favoritesFor(Species.fromId(speciesId))

    fun isFavorite(species: Species, foodId: String?): Boolean {
        if (foodId.isNullOrBlank()) return false
        return favoritesFor(species).contains(foodId)
    }

    fun isFavorite(speciesId: String, foodId: String?): Boolean =
        isFavorite(Species.fromId(speciesId), foodId)
}
