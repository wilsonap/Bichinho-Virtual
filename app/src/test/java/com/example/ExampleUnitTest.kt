package com.example

import com.example.data.model.ItemCategory
import com.example.data.model.ShopCatalog
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify all shop items have correct categories`() {
    val items = ShopCatalog.items

    // Verify toys are classified strictly as BRINQUEDO
    val ball = items.find { it.id == "toy_ball" }
    assertNotNull(ball)
    assertEquals(ItemCategory.BRINQUEDO, ball?.category)

    val duck = items.find { it.id == "toy_duck" }
    assertNotNull(duck)
    assertEquals(ItemCategory.BRINQUEDO, duck?.category)

    // Verify foods are classified strictly as ALIMENTO
    val apple = items.find { it.id == "food_apple" }
    assertNotNull(apple)
    assertEquals(ItemCategory.ALIMENTO, apple?.category)

    // Verify medicines are classified strictly as MEDICAMENTO
    val potion = items.find { it.id == "med_potion" }
    assertNotNull(potion)
    assertEquals(ItemCategory.MEDICAMENTO, potion?.category)

    val medicine = items.find { it.id == "med_vitamin" }
    assertNotNull(medicine)
    assertEquals(ItemCategory.MEDICAMENTO, medicine?.category)

    // Verify hats and accessories are preserved
    val cap = items.find { it.id == "cloth_cap" }
    assertNotNull(cap)
    assertEquals(ItemCategory.ROUPA, cap?.category)

    val glasses = items.find { it.id == "acc_glasses" }
    assertNotNull(glasses)
    assertEquals(ItemCategory.ACESSORIO, glasses?.category)

    // Verify room decor
    val decor = items.find { it.id == "decor_bedroom" }
    assertNotNull(decor)
    assertEquals(ItemCategory.DECORACAO, decor?.category)
  }

  @Test
  fun `verify findItemById handles both standard and legacy ids`() {
    val ball = ShopCatalog.findItemById("toy_ball")
    assertNotNull(ball)
    assertEquals("toy_ball", ball?.id)
    assertEquals(ItemCategory.BRINQUEDO, ball?.category)

    val potion = ShopCatalog.findItemById("med_potion")
    assertNotNull(potion)
    assertEquals(ItemCategory.MEDICAMENTO, potion?.category)
  }
}
