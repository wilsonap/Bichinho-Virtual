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
}

