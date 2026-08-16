package com.example.data.model

enum class ItemCategory(val displayName: String, val iconName: String) {
    ALIMENTO("Alimentos", "restaurant"),
    BRINQUEDO("Brinquedos", "sports_esports"),
    MEDICAMENTO("Medicamentos", "medication"),
    ROUPA("Roupas", "checkroom"),
    ACESSORIO("Acessórios", "diamond"),
    DECORACAO("Decorações", "palette")
}

data class ShopItem(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val price: Int,
    val description: String,
    val iconEmoji: String,
    val hungerBoost: Int = 0,
    val energyBoost: Int = 0,
    val happinessBoost: Int = 0,
    val hygieneBoost: Int = 0,
    val healthBoost: Int = 0,
    val expBoost: Int = 0
)

object ShopCatalog {
    val items = listOf(
        // Alimentos (Aumentam a Fome e dão EXP/Energia)
        ShopItem("food_apple", "Maçã Fresca", ItemCategory.ALIMENTO, 10, "Uma maçã crocante e deliciosa.", "🍎", hungerBoost = 15, healthBoost = 5, expBoost = 5),
        ShopItem("food_cookie", "Biscoito Doce", ItemCategory.ALIMENTO, 15, "Biscoito sabor chocolate recheado.", "🍪", hungerBoost = 20, happinessBoost = 10, expBoost = 5),
        ShopItem("food_fish", "Peixe Fresco", ItemCategory.ALIMENTO, 25, "Peixe fresquinho cheio de nutrientes.", "🐟", hungerBoost = 35, healthBoost = 10, expBoost = 10),
        ShopItem("food_meat", "Bife Suculento", ItemCategory.ALIMENTO, 40, "Carne nobre para encher a barriga.", "🥩", hungerBoost = 50, energyBoost = 15, expBoost = 15),
        ShopItem("food_pizza", "Fatia de Pizza", ItemCategory.ALIMENTO, 35, "Queijo derretido irresistível!", "🍕", hungerBoost = 40, happinessBoost = 25, expBoost = 10),

        // Brinquedos (Reutilizáveis, NÃO alteram fome, aumentam Felicidade e dão EXP)
        ShopItem("toy_ball", "Bola Saltitante", ItemCategory.BRINQUEDO, 30, "Bola colorida que quica alto. Brincadeira divertida!", "⚽", happinessBoost = 25, expBoost = 15, hungerBoost = 0),
        ShopItem("toy_duck", "Patinho de Borracha", ItemCategory.BRINQUEDO, 45, "Patinho que faz barulho fofo.", "🦆", happinessBoost = 35, hygieneBoost = 10, expBoost = 20, hungerBoost = 0),
        ShopItem("toy_laser", "Laser Brincalhão", ItemCategory.BRINQUEDO, 70, "Ponto vermelho que desperta o instinto.", "🔦", happinessBoost = 50, expBoost = 30, hungerBoost = 0),
        ShopItem("toy_plush", "Ursinho de Pelúcia", ItemCategory.BRINQUEDO, 90, "Companheiro fofinho para a soneca e abraços.", "🧸", happinessBoost = 60, energyBoost = 15, expBoost = 35, hungerBoost = 0),

        // Medicamentos (Consumíveis, restauram Saúde e Energia, NÃO alteram fome)
        ShopItem("med_potion", "Poção Revitalizante", ItemCategory.MEDICAMENTO, 60, "Restaura a saúde e energia ao máximo!", "🧪", healthBoost = 60, energyBoost = 40, expBoost = 25, hungerBoost = 0),
        ShopItem("med_vitamin", "Vitamina Fortalecedora", ItemCategory.MEDICAMENTO, 35, "Fortalece a imunidade e dá disposição ao bichinho!", "💊", healthBoost = 30, energyBoost = 20, expBoost = 10, hungerBoost = 0),

        // Roupas
        ShopItem("cloth_hat_magic", "Chapéu de Mago", ItemCategory.ROUPA, 120, "Aumenta a aura mágica do seu bichinho.", "🧙‍♂️", happinessBoost = 15),
        ShopItem("cloth_cap", "Boné Descolado", ItemCategory.ROUPA, 80, "Estilo esportivo para o dia a dia.", "🧢", happinessBoost = 10),
        ShopItem("cloth_crown", "Coroa Real", ItemCategory.ROUPA, 250, "Uma coroa dourada digna da realeza.", "👑", happinessBoost = 30),
        ShopItem("cloth_bow", "Laço Vermelho", ItemCategory.ROUPA, 60, "Um charmoso laço de cetim.", "🎀", happinessBoost = 10),
        ShopItem("cloth_cape", "Capa de Herói", ItemCategory.ROUPA, 180, "Para bichinhos com espírito heroico.", "🦸", happinessBoost = 20),

        // Acessórios
        ShopItem("acc_glasses", "Óculos de Sol", ItemCategory.ACESSORIO, 90, "Fique 100% estiloso e protegido da luz.", "🕶️", happinessBoost = 15),
        ShopItem("acc_bell", "Sino Dourado", ItemCategory.ACESSORIO, 75, "Um pequeno sino tilintante para o pescoço.", "🔔", happinessBoost = 10),
        ShopItem("acc_bowtie", "Gravata Borboleta", ItemCategory.ACESSORIO, 85, "Elegância pura para ocasiões especiais.", "👔", happinessBoost = 15),
        ShopItem("acc_chain", "Colar de Ouro", ItemCategory.ACESSORIO, 200, "Brilho puro com pingente de estrela.", "⭐", happinessBoost = 25),

        // Decorações
        ShopItem("decor_bedroom", "Quarto Aconchegante", ItemCategory.DECORACAO, 50, "Paredes em tons pastel e luz suave.", "🛏️", happinessBoost = 10),
        ShopItem("decor_forest", "Floresta Mágica", ItemCategory.DECORACAO, 140, "Árvores exuberantes e vaga-lumes.", "🌲", happinessBoost = 20),
        ShopItem("decor_beach", "Praia Tropical", ItemCategory.DECORACAO, 160, "Areia dourada e brisa marítima.", "🏖️", happinessBoost = 20),
        ShopItem("decor_space", "Espaço Sideral", ItemCategory.DECORACAO, 220, "Estrelas brilhantes, nebulosas e planetas.", "🌌", happinessBoost = 30)
    )

    fun findItemById(id: String): ShopItem? {
        if (id == "food_potion") {
            return items.find { it.id == "med_potion" }
        }
        return items.find { it.id == id }
    }
}
