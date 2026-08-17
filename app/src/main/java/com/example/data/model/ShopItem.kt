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
        // --- Alimentos genéricos (base do jogo) ---
        ShopItem("food_apple", "Maçã Fresca", ItemCategory.ALIMENTO, 10, "Uma maçã crocante e deliciosa.", "🍎", hungerBoost = 15, healthBoost = 5, expBoost = 5),
        ShopItem("food_cookie", "Biscoito Doce", ItemCategory.ALIMENTO, 15, "Biscoito sabor chocolate recheado.", "🍪", hungerBoost = 20, happinessBoost = 10, expBoost = 5),
        ShopItem("food_fish", "Peixe Fresco", ItemCategory.ALIMENTO, 25, "Peixe fresquinho cheio de nutrientes.", "🐟", hungerBoost = 35, healthBoost = 10, expBoost = 10),
        ShopItem("food_meat", "Bife Suculento", ItemCategory.ALIMENTO, 40, "Carne nobre para encher a barriga.", "🥩", hungerBoost = 50, energyBoost = 15, expBoost = 15),
        ShopItem("food_pizza", "Fatia de Pizza", ItemCategory.ALIMENTO, 35, "Queijo derretido irresistível!", "🍕", hungerBoost = 40, happinessBoost = 25, expBoost = 10),

        // --- Alimentos favoritos / especiais (preferências por espécie) ---
        ShopItem("food_tuna", "Atum", ItemCategory.ALIMENTO, 28, "Lata de atum suculento, sucesso felino.", "🐟", hungerBoost = 30, happinessBoost = 8, healthBoost = 5, expBoost = 10),
        ShopItem("food_bone", "Ossinho", ItemCategory.ALIMENTO, 22, "Osso crocante para roer com alegria.", "🦴", hungerBoost = 25, happinessBoost = 12, expBoost = 8),
        ShopItem("food_carrot", "Cenoura", ItemCategory.ALIMENTO, 12, "Cenoura crocante e docinha.", "🥕", hungerBoost = 20, healthBoost = 5, expBoost = 6),
        ShopItem("food_lettuce", "Alface", ItemCategory.ALIMENTO, 10, "Folhas fresquinhas e leves.", "🥬", hungerBoost = 15, healthBoost = 5, expBoost = 5),
        ShopItem("food_seeds", "Mistura de Sementes", ItemCategory.ALIMENTO, 12, "Sementes variadas para bochechas cheias.", "🌾", hungerBoost = 18, energyBoost = 5, expBoost = 6),
        ShopItem("food_corn", "Milho Doce", ItemCategory.ALIMENTO, 14, "Espiga dourada e adocicada.", "🌽", hungerBoost = 22, happinessBoost = 8, expBoost = 7),
        ShopItem("food_berries", "Mix de Frutas Vermelhas", ItemCategory.ALIMENTO, 18, "Morango, amora e mirtilo juntos.", "🫐", hungerBoost = 20, happinessBoost = 10, healthBoost = 5, expBoost = 8),
        ShopItem("food_seaweed", "Algas", ItemCategory.ALIMENTO, 20, "Algas marinhas ricas em minerais.", "🌊", hungerBoost = 25, healthBoost = 8, expBoost = 8),
        ShopItem("food_night_bites", "Petiscos da Noite", ItemCategory.ALIMENTO, 24, "Petiscos macios para caçadores noturnos.", "🌙", hungerBoost = 28, happinessBoost = 10, expBoost = 10),
        ShopItem("food_bamboo", "Bambu", ItemCategory.ALIMENTO, 25, "Talos crocantes de bambu fresco.", "🎋", hungerBoost = 30, happinessBoost = 12, expBoost = 10),
        ShopItem("food_bamboo_shoot", "Broto de Bambu", ItemCategory.ALIMENTO, 32, "Brotinhos tenros, iguaria especial.", "🎍", hungerBoost = 35, happinessBoost = 15, healthBoost = 5, expBoost = 12),
        ShopItem("food_steak", "Filé Premium", ItemCategory.ALIMENTO, 55, "Corte nobre para predadores majestosos.", "🍖", hungerBoost = 55, energyBoost = 20, expBoost = 18),
        ShopItem("food_shrimp", "Camarão", ItemCategory.ALIMENTO, 30, "Camarões frescos do oceano.", "🦐", hungerBoost = 32, happinessBoost = 12, expBoost = 12),
        ShopItem("food_chili", "Pimenta Flamejante", ItemCategory.ALIMENTO, 35, "Arde na língua, aquece o espírito.", "🌶️", hungerBoost = 25, happinessBoost = 15, energyBoost = 10, expBoost = 14),
        ShopItem("food_honey", "Mel Dourado", ItemCategory.ALIMENTO, 28, "Mel puro e brilhante.", "🍯", hungerBoost = 22, happinessBoost = 15, healthBoost = 5, expBoost = 10),
        ShopItem("food_ember_fruit", "Fruta de Brasas", ItemCategory.ALIMENTO, 80, "Fruta mística que brilha como brasas.", "🔥", hungerBoost = 40, happinessBoost = 20, energyBoost = 15, expBoost = 25),
        ShopItem("food_sun_nectar", "Néctar Solar", ItemCategory.ALIMENTO, 80, "Néctar dourado banhado pelo sol.", "☀️", hungerBoost = 40, happinessBoost = 20, healthBoost = 10, expBoost = 25),

        // --- Expansão de prateleira (compráveis; não são favoritos automáticos) ---
        // Saudáveis
        ShopItem("food_banana", "Banana", ItemCategory.ALIMENTO, 8, "Banana madura e energética.", "🍌", hungerBoost = 15),
        ShopItem("food_strawberry", "Morango", ItemCategory.ALIMENTO, 12, "Morango doce e fresquinho.", "🍓", hungerBoost = 18, happinessBoost = 2),
        ShopItem("food_watermelon", "Melancia", ItemCategory.ALIMENTO, 18, "Fatia suculenta de melancia.", "🍉", hungerBoost = 25),
        ShopItem("food_grapes", "Uvas", ItemCategory.ALIMENTO, 15, "Cacho de uvas doces.", "🍇", hungerBoost = 20, happinessBoost = 2),
        ShopItem("food_salad", "Salada", ItemCategory.ALIMENTO, 20, "Salada colorida e nutritiva.", "🥗", hungerBoost = 20, healthBoost = 5),
        ShopItem("food_soup", "Sopa", ItemCategory.ALIMENTO, 18, "Sopa quentinha e reconfortante.", "🍲", hungerBoost = 18, energyBoost = 5, healthBoost = 5),
        ShopItem("food_natural_sandwich", "Sanduíche Natural", ItemCategory.ALIMENTO, 25, "Sanduíche leve com vegetais.", "🥪", hungerBoost = 30),
        ShopItem("food_omelet", "Omelete", ItemCategory.ALIMENTO, 22, "Omelete fofinha e proteica.", "🍳", hungerBoost = 25, healthBoost = 3),
        // Refeições
        ShopItem("food_roast_chicken", "Frango Assado", ItemCategory.ALIMENTO, 35, "Frango assado douradinho.", "🍗", hungerBoost = 40, healthBoost = 5),
        ShopItem("food_lasagna", "Lasanha", ItemCategory.ALIMENTO, 40, "Lasanha com queijo derretido.", "🍝", hungerBoost = 45, happinessBoost = 5),
        ShopItem("food_burger", "Hambúrguer", ItemCategory.ALIMENTO, 35, "Hambúrguer suculento no ponto.", "🍔", hungerBoost = 40, happinessBoost = 5),
        ShopItem("food_barbecue", "Churrasco", ItemCategory.ALIMENTO, 50, "Churrasco caprichado na brasa.", "🥩", hungerBoost = 50, happinessBoost = 10),
        // Sobremesas
        ShopItem("food_ice_cream", "Sorvete", ItemCategory.ALIMENTO, 20, "Sorvete geladinho e cremoso.", "🍦", hungerBoost = 10, happinessBoost = 10),
        ShopItem("food_cake", "Bolo", ItemCategory.ALIMENTO, 25, "Fatia de bolo fofinho.", "🍰", hungerBoost = 15, happinessBoost = 8),
        ShopItem("food_donut", "Donut", ItemCategory.ALIMENTO, 18, "Donut glacê irresistível.", "🍩", hungerBoost = 12, happinessBoost = 5),
        ShopItem("food_pudding", "Pudim", ItemCategory.ALIMENTO, 22, "Pudim cremoso com calda.", "🍮", hungerBoost = 15, happinessBoost = 5),
        // Bebidas
        ShopItem("drink_natural_juice", "Suco Natural", ItemCategory.ALIMENTO, 12, "Suco fresco de frutas.", "🧃", hungerBoost = 5, energyBoost = 10),
        ShopItem("drink_hot_chocolate", "Chocolate Quente", ItemCategory.ALIMENTO, 18, "Chocolate quente aconchegante.", "☕", hungerBoost = 8, energyBoost = 15),
        ShopItem("drink_coffee_milk", "Café com Leite", ItemCategory.ALIMENTO, 20, "Café com leite cremoso.", "🥛", hungerBoost = 5, energyBoost = 20),

        // Brinquedos (Reutilizáveis, NÃO alteram fome, aumentam Felicidade e dão EXP)
        ShopItem("toy_ball", "Bola Saltitante", ItemCategory.BRINQUEDO, 30, "Bola colorida que quica alto. Brincadeira divertida!", "⚽", happinessBoost = 25, expBoost = 15, hungerBoost = 0),
        ShopItem("toy_duck", "Patinho de Borracha", ItemCategory.BRINQUEDO, 45, "Patinho que faz barulho fofo.", "🦆", happinessBoost = 35, hygieneBoost = 10, expBoost = 20, hungerBoost = 0),
        ShopItem("toy_laser", "Laser Brincalhão", ItemCategory.BRINQUEDO, 70, "Ponto vermelho que desperta o instinto.", "🔦", happinessBoost = 50, expBoost = 30, hungerBoost = 0),
        ShopItem("toy_plush", "Ursinho de Pelúcia", ItemCategory.BRINQUEDO, 90, "Companheiro fofinho para a soneca e abraços.", "🧸", happinessBoost = 60, energyBoost = 15, expBoost = 35, hungerBoost = 0),

        // Medicamentos (Consumíveis, restauram Saúde e Energia, curam doenças específicas)
        ShopItem("med_potion", "Poção Revitalizante", ItemCategory.MEDICAMENTO, 60, "Restaura a saúde (+60), energia (+40) e cura qualquer doença!", "🧪", healthBoost = 60, energyBoost = 40, expBoost = 25, hungerBoost = 0),
        ShopItem("med_vitamin", "Vitamina Fortalecedora", ItemCategory.MEDICAMENTO, 35, "Fortalece a imunidade, cura Fadiga e recupera energia!", "💊", healthBoost = 30, energyBoost = 30, expBoost = 15, hungerBoost = 0),
        ShopItem("med_digestive", "Remédio Digestivo", ItemCategory.MEDICAMENTO, 25, "Alivia desconforto estomacal e cura Indigestão.", "🍵", healthBoost = 25, energyBoost = 10, expBoost = 10, hungerBoost = 0),
        ShopItem("med_cold", "Xarope para Resfriado", ItemCategory.MEDICAMENTO, 25, "Alivia espirros, febre e cura Resfriado.", "🍯", healthBoost = 25, energyBoost = 10, expBoost = 10, hungerBoost = 0),

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
