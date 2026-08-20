package com.example.data.model

enum class HouseRoom(
    val id: String,
    val displayName: String,
    val icon: String,
    val description: String
) {
    LIVING_ROOM(
        id = "living_room",
        displayName = "Sala",
        icon = "🛋️",
        description = "Ambiente principal para relaxar e conviver"
    ),
    BEDROOM(
        id = "bedroom",
        displayName = "Quarto",
        icon = "🛏️",
        description = "Quarto Aconchegante para repouso e sono"
    ),
    KITCHEN(
        id = "kitchen",
        displayName = "Cozinha",
        icon = "🍳",
        description = "Cozinha equipada para deliciosas refeições"
    ),
    BATHROOM(
        id = "bathroom",
        displayName = "Banheiro",
        icon = "🛁",
        description = "Banheiro para banhos relaxantes e higiene"
    ),
    BACKYARD(
        id = "backyard",
        displayName = "Quintal",
        icon = "🌳",
        description = "Área ao ar livre para brincadeiras e diversão"
    ),
    GARAGE(
        id = "garage",
        displayName = "Garagem",
        icon = "🚲",
        description = "Espaço de atividades, oficinas e minijogos"
    ),
    SCHOOL(
        id = "school",
        displayName = "Escola",
        icon = "🎒",
        description = "Sala de aula para Filhote e Jovem"
    );

    companion object {
        fun fromId(id: String?): HouseRoom {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: LIVING_ROOM
        }
    }
}
