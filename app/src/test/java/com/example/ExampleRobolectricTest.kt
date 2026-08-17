package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PetAutonomousState
import com.example.data.model.PetBehaviorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bichinho Virtual", appName)
  }

  @Test
  fun `verify pet behavior states`() {
    val defaultState = PetAutonomousState()
    assertEquals(PetBehaviorState.OCIOSO, defaultState.behaviorState)
    assertNotNull(PetBehaviorState.CAMINHANDO.displayName)
    assertNotNull(PetBehaviorState.COM_SAUDADE.displayName)
    assertNotNull(PetBehaviorState.BOCEJANDO.displayName)
  }

  @Test
  fun `test inventory consolidation logic`() {
    val rawList = listOf(
        com.example.data.local.InventoryEntity(id = 1, itemId = "toy_ball", category = "BRINQUEDO", name = "Bola Saltitante", quantity = 1),
        com.example.data.local.InventoryEntity(id = 2, itemId = "toy_ball", category = "BRINQUEDO", name = "Bola Saltitante", quantity = 1),
        com.example.data.local.InventoryEntity(id = 3, itemId = "decor_bedroom", category = "DECORACAO", name = "Quarto", quantity = 1, isEquipped = true),
        com.example.data.local.InventoryEntity(id = 4, itemId = "decor_bedroom", category = "DECORACAO", name = "Quarto", quantity = 1, isEquipped = true),
        com.example.data.local.InventoryEntity(id = 5, itemId = "decor_bedroom", category = "DECORACAO", name = "Quarto", quantity = 1, isEquipped = true),
        com.example.data.local.InventoryEntity(id = 6, itemId = "food_cookie", category = "ALIMENTO", name = "Biscoito Doce", quantity = 2),
        com.example.data.local.InventoryEntity(id = 7, itemId = "food_cookie", category = "ALIMENTO", name = "Biscoito Doce", quantity = 2)
    )

    val consolidated = rawList.groupBy { it.itemId }.values.map { group ->
        val first = group.first()
        val isReusable = first.category in listOf("BRINQUEDO", "ROUPA", "ACESSORIO", "DECORACAO")
        val totalQty = if (isReusable) 1 else group.sumOf { it.quantity }
        val equipped = group.any { it.isEquipped }
        first.copy(quantity = totalQty, isEquipped = equipped)
    }

    assertEquals(3, consolidated.size)
    val ball = consolidated.find { it.itemId == "toy_ball" }
    assertEquals(1, ball?.quantity)

    val decor = consolidated.find { it.itemId == "decor_bedroom" }
    assertEquals(1, decor?.quantity)
    assertEquals(true, decor?.isEquipped)

    val cookie = consolidated.find { it.itemId == "food_cookie" }
    assertEquals(4, cookie?.quantity)
  }
}

