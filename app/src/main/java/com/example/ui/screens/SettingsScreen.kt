package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PetEntity
import com.example.notification.NotificationHelper
import com.example.ui.components.MandatoryNamingDialog
import com.example.ui.components.NotificationPermissionExplanationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    pet: PetEntity?,
    notificationsEnabled: Boolean = true,
    hungerNotifications: Boolean = true,
    hygieneNotifications: Boolean = true,
    energyNotifications: Boolean = true,
    healthNotifications: Boolean = true,
    longingNotifications: Boolean = true,
    isMusicEnabled: Boolean = true,
    isSfxEnabled: Boolean = true,
    musicVolume: Float = 0.5f,
    sfxVolume: Float = 0.8f,
    onToggleNotifications: (Boolean) -> Unit = {},
    onToggleHunger: (Boolean) -> Unit = {},
    onToggleHygiene: (Boolean) -> Unit = {},
    onToggleEnergy: (Boolean) -> Unit = {},
    onToggleHealth: (Boolean) -> Unit = {},
    onToggleLonging: (Boolean) -> Unit = {},
    onToggleMusic: (Boolean) -> Unit = {},
    onToggleSfx: (Boolean) -> Unit = {},
    onChangeMusicVolume: (Float) -> Unit = {},
    onChangeSfxVolume: (Float) -> Unit = {},
    onTestSfx: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onTriggerImmediateCheck: () -> Unit = {},
    onRenamePet: (String) -> Unit,
    onResetPet: () -> Unit
) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionExplanation by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pet Profile & Renaming
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("settings_pet_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Identidade do Bichinho",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Nome Atual",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = pet?.name?.ifBlank { "Sem nome" } ?: "Sem nome",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Button(
                                onClick = { showRenameDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("open_rename_dialog_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alterar Nome")
                            }
                        }
                    }
                }
            }

            // Notification Settings Section
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("settings_notifications_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Notificações do Bichinho",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Avisos quando o app estiver fechado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !NotificationHelper.hasNotificationPermission(context)
                                        ) {
                                            showPermissionExplanation = true
                                        } else {
                                            onToggleNotifications(true)
                                        }
                                    } else {
                                        onToggleNotifications(false)
                                    }
                                },
                                modifier = Modifier.testTag("switch_master_notifications")
                            )
                        }

                        if (notificationsEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Hunger sub-toggle
                            NotificationSettingRow(
                                title = "Avisos de Fome",
                                description = "Alerta quando a fome atingir 20% ou menos",
                                checked = hungerNotifications,
                                onCheckedChange = onToggleHunger,
                                testTag = "switch_hunger_notifications"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Hygiene sub-toggle
                            NotificationSettingRow(
                                title = "Avisos de Higiene",
                                description = "Alerta quando a higiene atingir 20% ou menos",
                                checked = hygieneNotifications,
                                onCheckedChange = onToggleHygiene,
                                testTag = "switch_hygiene_notifications"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Energy sub-toggle
                            NotificationSettingRow(
                                title = "Avisos de Sono / Cansaço",
                                description = "Alerta quando a energia atingir 15% ou menos",
                                checked = energyNotifications,
                                onCheckedChange = onToggleEnergy,
                                testTag = "switch_energy_notifications"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Health sub-toggle
                            NotificationSettingRow(
                                title = "Avisos de Saúde (Prioritário)",
                                description = "Alerta de emergência quando a saúde atingir 30% ou menos",
                                checked = healthNotifications,
                                onCheckedChange = onToggleHealth,
                                testTag = "switch_health_notifications"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Longing sub-toggle
                            NotificationSettingRow(
                                title = "Avisos de Saudade",
                                description = "Mensagem afetuosa quando você passar muito tempo sem visitar",
                                checked = longingNotifications,
                                onCheckedChange = onToggleLonging,
                                testTag = "switch_longing_notifications"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Anti-Spam & Quiet Hours Info Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🛡️ Proteção Inteligente & Conforto:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Máximo de 3 notificações de cuidado por dia.\n• Horário Silencioso das 22h às 08h (sem som ou vibração).\n• Sem spam: cada necessidade alerta apenas 1 vez até você cuidar do bichinho.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Test Notification Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !NotificationHelper.hasNotificationPermission(context)
                                        ) {
                                            showPermissionExplanation = true
                                        } else {
                                            onSendTestNotification()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("send_test_notification_button")
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testar Notificação", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = onTriggerImmediateCheck,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("trigger_check_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verificar Agora", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Audio & Feedback
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("settings_audio_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Áudio e Música",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. Música de Fundo (BGM) Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Música de Fundo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("Trilhas temáticas por tela", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isMusicEnabled,
                                onCheckedChange = onToggleMusic,
                                modifier = Modifier.testTag("switch_music")
                            )
                        }

                        if (isMusicEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Volume da Música", style = MaterialTheme.typography.bodySmall)
                                    Text("${(musicVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = musicVolume,
                                    onValueChange = onChangeMusicVolume,
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth().testTag("slider_music_volume")
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        // 2. Efeitos Sonoros (SFX) Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Efeitos Sonoros (SFX)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("Sons do bichinho e ações", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isSfxEnabled,
                                onCheckedChange = onToggleSfx,
                                modifier = Modifier.testTag("switch_sfx")
                            )
                        }

                        if (isSfxEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Volume dos Efeitos", style = MaterialTheme.typography.bodySmall)
                                    Text("${(sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = sfxVolume,
                                    onValueChange = onChangeSfxVolume,
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth().testTag("slider_sfx_volume")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = onTestSfx,
                                modifier = Modifier.fillMaxWidth().testTag("test_sfx_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testar Efeito Sonoro")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                        // 3. Feedback Tátil
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Feedback Tátil (Vibração)", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { vibrationEnabled = it }
                            )
                        }
                    }
                }
            }

            // Game Info & Offline Database
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("settings_info_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sobre o Jogo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bichinho Virtual com Espécies Aleatórias",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 100% Offline com Room Database\n• WorkManager com agendamento inteligente de notificações\n• Proteção contra spam e horário silencioso\n• Versão 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Reset Creature Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("settings_danger_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Zona de Reinício",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deseja começar uma nova jornada e receber um novo ovo misterioso? O bichinho atual será resetado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reset_pet_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reiniciar e Receber Novo Ovo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Rename Dialog
        if (showRenameDialog) {
            MandatoryNamingDialog(
                initialName = pet?.name ?: "",
                title = "Alterar Nome do Bichinho",
                subtitle = "O nome é obrigatório para manter o vínculo com seu bichinho.",
                onConfirm = { newName ->
                    onRenamePet(newName)
                    showRenameDialog = false
                }
            )
        }

        // Permission Context Explanation Dialog
        if (showPermissionExplanation) {
            NotificationPermissionExplanationDialog(
                petName = pet?.name ?: "Bichinho",
                onDismiss = { showPermissionExplanation = false },
                onPermissionResult = { isGranted ->
                    if (isGranted) {
                        onToggleNotifications(true)
                    }
                }
            )
        }

        // Reset Confirmation Dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Confirmar Reinício?") },
                text = {
                    Text("Esta ação apagará o bichinho atual e concederá um novo ovo para você cuidar desde o início. Tem certeza?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            onResetPet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.testTag("confirm_reset_button")
                    ) {
                        Text("Sim, Reiniciar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun NotificationSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
