package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.audio.GameAudioManager
import com.example.notification.NotificationHelper
import com.example.ui.MainApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private var notificationType by mutableStateOf<String?>(null)
  private lateinit var audioManager: GameAudioManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    audioManager = GameAudioManager.getInstance(applicationContext)
    notificationType = intent?.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
    setContent {
      MyApplicationTheme {
        MainApp(notificationType = notificationType)
      }
    }
  }

  override fun onStart() {
    super.onStart()
    audioManager.onAppForeground()
  }

  override fun onStop() {
    super.onStop()
    audioManager.onAppBackground()
  }

  override fun onDestroy() {
    super.onDestroy()
    audioManager.release()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    notificationType = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
  }
}


