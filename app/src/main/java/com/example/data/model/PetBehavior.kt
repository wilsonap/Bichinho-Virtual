package com.example.data.model

enum class PetBehaviorState(val displayName: String, val defaultIcon: String) {
    OCIOSO("Ocioso", "✨"),
    CAMINHANDO("Caminhando", "🐾"),
    OLHANDO_LADOS("Olhando ao redor", "👀"),
    PULANDO("Pulando de alegria", "🦘"),
    SENTADO("Sentado descansando", "🪑"),
    COMENDO("Comendo", "🍎"),
    BRINCANDO("Brincando", "🎾"),
    DORMINDO("Dormindo", "💤"),
    ACORDANDO("Acordando", "☀️"),
    TOMANDO_BANHO("Tomando banho", "🫧"),
    DOENTE("Doente", "🩹"),
    FELIZ("Muito Feliz", "💖"),
    TRISTE("Triste", "😢"),
    COM_SAUDADE("Com Saudade", "🥺"),
    PROCURANDO_COMIDA("Procurando comida", "🔍"),
    BOCEJANDO("Bocejando com sono", "🥱")
}

data class PetAutonomousState(
    val behaviorState: PetBehaviorState = PetBehaviorState.OCIOSO,
    val walkOffsetX: Float = 0f, // -110dp to +110dp across room
    val targetOffsetX: Float = 0f,
    val walkDirection: Float = 1f, // 1f = facing right, -1f = facing left
    val isBlinking: Boolean = false,
    val blinkProgress: Float = 0f, // 0f = open, 1f = closed
    val lookGazeX: Float = 0f, // -1f = looking left, 0f = center, 1f = right
    val lookGazeY: Float = 0f, // -1f = looking up, 0f = center, 1f = down
    val jumpProgress: Float = 0f, // 0f to 1f vertical jump curve
    val isSquishing: Boolean = false,
    val currentSpeechText: String = "",
    val speechBubbleVisible: Boolean = true,
    val isLongingGreeting: Boolean = false,
    val lastInteractionTimestamp: Long = System.currentTimeMillis()
)
