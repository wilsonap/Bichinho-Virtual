package com.example.data.model

/**
 * Subcategorias visuais da aba Alimentos (Loja/Mochila).
 * Não altera fome, XP, felicidade, saúde, indigestão nem favoritos.
 */
enum class FoodShopSubcategory(
    val label: String,
    val emoji: String
) {
    FAVORITOS("Favoritos", "❤️"),
    SAUDAVEL("Saudáveis", "🥗"),
    REFEICAO("Refeições", "🍖"),
    SOBREMESA("Sobremesas", "🍰"),
    BEBIDA("Bebidas", "☕"),
    ESPECIAL("Especiais", "✨");

    val chipLabel: String get() = "$emoji $label"
}

/**
 * Classificação de prateleira dos alimentos (uma por item).
 * Favoritos é dinâmico e não entra neste mapa.
 */
enum class FoodShelfGroup {
    SAUDAVEL,
    REFEICAO,
    SOBREMESA,
    BEBIDA,
    ESPECIAL
}

/**
 * Organização visual dos alimentos. Filtra instâncias do [ShopCatalog] sem duplicar itens.
 */
object FoodShopOrganization {

    private val shelfByFoodId: Map<String, FoodShelfGroup> = mapOf(
        // Saudáveis
        "food_apple" to FoodShelfGroup.SAUDAVEL,
        "food_carrot" to FoodShelfGroup.SAUDAVEL,
        "food_lettuce" to FoodShelfGroup.SAUDAVEL,
        "food_seeds" to FoodShelfGroup.SAUDAVEL,
        "food_berries" to FoodShelfGroup.SAUDAVEL,
        "food_seaweed" to FoodShelfGroup.SAUDAVEL,
        "food_bamboo" to FoodShelfGroup.SAUDAVEL,
        "food_bamboo_shoot" to FoodShelfGroup.SAUDAVEL,
        "food_corn" to FoodShelfGroup.SAUDAVEL,
        "food_banana" to FoodShelfGroup.SAUDAVEL,
        "food_strawberry" to FoodShelfGroup.SAUDAVEL,
        "food_watermelon" to FoodShelfGroup.SAUDAVEL,
        "food_grapes" to FoodShelfGroup.SAUDAVEL,
        "food_salad" to FoodShelfGroup.SAUDAVEL,
        "food_soup" to FoodShelfGroup.SAUDAVEL,
        "food_natural_sandwich" to FoodShelfGroup.SAUDAVEL,
        "food_omelet" to FoodShelfGroup.SAUDAVEL,
        // Refeições
        "food_fish" to FoodShelfGroup.REFEICAO,
        "food_meat" to FoodShelfGroup.REFEICAO,
        "food_tuna" to FoodShelfGroup.REFEICAO,
        "food_bone" to FoodShelfGroup.REFEICAO,
        "food_night_bites" to FoodShelfGroup.REFEICAO,
        "food_steak" to FoodShelfGroup.REFEICAO,
        "food_shrimp" to FoodShelfGroup.REFEICAO,
        "food_pizza" to FoodShelfGroup.REFEICAO,
        "food_roast_chicken" to FoodShelfGroup.REFEICAO,
        "food_lasagna" to FoodShelfGroup.REFEICAO,
        "food_burger" to FoodShelfGroup.REFEICAO,
        "food_barbecue" to FoodShelfGroup.REFEICAO,
        // Sobremesas
        "food_cookie" to FoodShelfGroup.SOBREMESA,
        "food_honey" to FoodShelfGroup.SOBREMESA,
        "food_ice_cream" to FoodShelfGroup.SOBREMESA,
        "food_cake" to FoodShelfGroup.SOBREMESA,
        "food_donut" to FoodShelfGroup.SOBREMESA,
        "food_pudding" to FoodShelfGroup.SOBREMESA,
        // Bebidas
        "food_sun_nectar" to FoodShelfGroup.BEBIDA,
        "drink_natural_juice" to FoodShelfGroup.BEBIDA,
        "drink_hot_chocolate" to FoodShelfGroup.BEBIDA,
        "drink_coffee_milk" to FoodShelfGroup.BEBIDA,
        // Especiais
        "food_chili" to FoodShelfGroup.ESPECIAL,
        "food_ember_fruit" to FoodShelfGroup.ESPECIAL
    )

    fun shelfGroupOf(foodId: String): FoodShelfGroup? = shelfByFoodId[foodId]

    fun foodsInShelf(group: FoodShelfGroup): List<ShopItem> =
        ShopCatalog.items.filter {
            it.category == ItemCategory.ALIMENTO && shelfByFoodId[it.id] == group
        }

    /**
     * Favoritos dinâmicos da espécie atual (mesmas instâncias do catálogo).
     */
    fun favoriteFoodsForSpecies(speciesId: String?): List<ShopItem> {
        if (speciesId.isNullOrBlank()) return emptyList()
        val favoriteIds = FoodPreferenceCatalog.favoritesFor(speciesId)
        return favoriteIds.mapNotNull { id -> ShopCatalog.findItemById(id) }
            .filter { it.category == ItemCategory.ALIMENTO }
    }

    fun filterShopFoods(
        subcategory: FoodShopSubcategory,
        speciesId: String?
    ): List<ShopItem> {
        return when (subcategory) {
            FoodShopSubcategory.FAVORITOS -> favoriteFoodsForSpecies(speciesId)
            FoodShopSubcategory.SAUDAVEL -> foodsInShelf(FoodShelfGroup.SAUDAVEL)
            FoodShopSubcategory.REFEICAO -> foodsInShelf(FoodShelfGroup.REFEICAO)
            FoodShopSubcategory.SOBREMESA -> foodsInShelf(FoodShelfGroup.SOBREMESA)
            FoodShopSubcategory.BEBIDA -> foodsInShelf(FoodShelfGroup.BEBIDA)
            FoodShopSubcategory.ESPECIAL -> foodsInShelf(FoodShelfGroup.ESPECIAL)
        }
    }

    /** Contagem única de alimentos no catálogo (sem duplicatas). */
    fun totalFoodItemsInCatalog(): Int =
        ShopCatalog.items.count { it.category == ItemCategory.ALIMENTO }
}
