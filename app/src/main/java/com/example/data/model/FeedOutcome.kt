package com.example.data.model

/**
 * Resultado de [com.example.data.repository.PetRepository.feedPet].
 */
data class FeedOutcome(
    val success: Boolean,
    val wasFavorite: Boolean = false
) {
    companion object {
        fun failed() = FeedOutcome(success = false, wasFavorite = false)
    }
}
