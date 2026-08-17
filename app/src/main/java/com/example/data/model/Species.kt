package com.example.data.model

enum class Rarity(val displayName: String, val colorHex: Long, val weight: Int) {
    COMUM("Comum", 0xFF64748B, 60),
    RARA("Rara", 0xFF0284C7, 25),
    EPICA("Épica", 0xFF9333EA, 12),
    LENDARIA("Lendária", 0xFFEAB308, 3)
}

enum class PetStage(
    val displayName: String,
    val minDaysAlive: Int,
    val minLevel: Int,
    val scaleMultiplier: Float,
    val description: String
) {
    OVO("Ovo", 0, 1, 0.8f, "Aconchegado no ninho esperando para chocar"),
    FILHOTE("Filhote", 0, 1, 0.85f, "Pequeno, curioso e cheio de energia"),
    JOVEM("Jovem", 2, 5, 1.0f, "Ágil, brincalhão e aprendendo rápido"),
    ADULTO("Adulto", 7, 12, 1.15f, "Forte, leal e em sua plenitude"),
    IDOSO("Idoso", 30, 25, 1.15f, "Sábio, carinhoso e um companheiro para toda a vida");

    fun nextStage(): PetStage? = when (this) {
        OVO -> FILHOTE
        FILHOTE -> JOVEM
        JOVEM -> ADULTO
        ADULTO -> IDOSO
        IDOSO -> null
    }

    fun meetsRequirements(daysAlive: Int, level: Int): Boolean {
        return daysAlive >= minDaysAlive && level >= minLevel
    }
}

enum class Species(
    val id: String,
    val displayName: String,
    val rarity: Rarity,
    val description: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val soundLabel: String
) {
    // Comuns
    GATO("gato", "Gato", Rarity.COMUM, "Curioso e carinhoso, adora brincar com novelos de lã.", 0xFFFFB74D, 0xFFFFE0B2, "Miau!"),
    CACHORRO("cachorro", "Cachorro", Rarity.COMUM, "Leal e super animado, está sempre pronto para passear.", 0xFF8D6E63, 0xFFD7CCC8, "Au au!"),
    COELHO("coelho", "Coelho", Rarity.COMUM, "Pula sem parar e adora mastigar cenouras crocantes.", 0xFFE0E0E0, 0xFFF8BBD0, "Sniff!"),
    HAMSTER("hamster", "Hamster", Rarity.COMUM, "Pequenino de bochechas cheias e energia contagiante.", 0xFFFFCC80, 0xFFFFF3E0, "Squeak!"),
    TARTARUGA("tartaruga", "Tartaruga", Rarity.COMUM, "Calma e sábia, possui um casco resistente e simpático.", 0xFF66BB6A, 0xFFC8E6C9, "Ploop!"),
    CORUJA("coruja", "Coruja", Rarity.COMUM, "Olhos atentos e bico afiado, adora a noite e mistérios.", 0xFF8D6E63, 0xFFFFF9C4, "Uhuu!"),

    // Raras
    RAPOSA("raposa", "Raposa", Rarity.RARA, "Esperta e graciosa com sua bela cauda avermelhada.", 0xFFF4511E, 0xFFFFCC80, "Yip yip!"),
    PANDA("panda", "Panda", Rarity.RARA, "Fofo e pacífico, ama comer folhas de bambu o dia todo.", 0xFF374151, 0xFFF9FAFB, "Grr!"),
    LOBO("lobo", "Lobo", Rarity.RARA, "Nobre e corajoso, uiva para a lua cheia com lealdade.", 0xFF607D8B, 0xFFCFD8DC, "Auuu!"),
    GUAXINIM("guaxinim", "Guaxinim", Rarity.RARA, "O travesso mascarado que adora colecionar pequenos tesouros.", 0xFF78909C, 0xFFECEFF1, "Chirr!"),

    // Épicas
    TIGRE("tigre", "Tigre", Rarity.EPICA, "Poderoso e imponente, com listras marcantes e olhar feroz.", 0xFFFF9800, 0xFF212121, "Roaaar!"),
    LEAO("leao", "Leão", Rarity.EPICA, "O rei da selva com uma juba dourada cheia de majestade.", 0xFFFBC02D, 0xFFFFF59D, "Rooaaar!"),
    POLVO("polvo", "Polvo", Rarity.EPICA, "Inteligente habitante dos mares com tentáculos ágeis.", 0xFFEC4899, 0xFFFCE7F3, "Glub glub!"),

    // Lendárias
    DRAGAO("dragao", "Dragão", Rarity.LENDARIA, "Criatura mística lendária com asas poderosas e chamas místicas.", 0xFF7C3AED, 0xFFF59E0B, "Fwooosh!"),
    FENIX("fenix", "Fênix", Rarity.LENDARIA, "Pássaro imortal que brilha em chamas douradas renascendo sempre.", 0xFFEF4444, 0xFFFDE047, "Kreeee!");

    companion object {
        fun fromId(id: String): Species {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GATO
        }

        fun getRandomSpecies(): Species {
            // Weighted random selection based on rarity weights
            val pool = mutableListOf<Species>()
            entries.forEach { species ->
                repeat(species.rarity.weight) {
                    pool.add(species)
                }
            }
            return pool.random()
        }
    }
}
