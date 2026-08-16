package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InventoryEntity
import com.example.data.model.ItemCategory
import com.example.data.model.ShopCatalog
import com.example.data.model.ShopItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventory: List<InventoryEntity>,
    onUseItem: (ShopItem) -> Unit,
    onEquipItem: (InventoryEntity) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ItemCategory.ALIMENTO) }

    val filteredInventory = remember(inventory, selectedCategory) {
        inventory.filter { it.category == selectedCategory.name && it.quantity > 0 }
    }

    val catalogMap = remember { ShopCatalog.items.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mochila / Inventário", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth().testTag("inventory_category_tabs")
            ) {
                ItemCategory.entries.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = {
                            Text(
                                text = category.displayName,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredInventory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum item nesta categoria.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Visite a loja para adquirir novidades!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("inventory_items_grid"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInventory) { invItem ->
                        val shopItem = ShopCatalog.findItemById(invItem.itemId)

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (invItem.isEquipped) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (invItem.isEquipped) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6)) else null,
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth().testTag("inv_card_${invItem.itemId}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(64.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = shopItem?.iconEmoji ?: "📦", fontSize = 36.sp)
                                        }
                                    }

                                    if (invItem.quantity > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        ) {
                                            Text(
                                                text = "x${invItem.quantity}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = invItem.name.ifBlank { shopItem?.name ?: "Item" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                when (selectedCategory) {
                                    ItemCategory.ALIMENTO -> {
                                        Button(
                                            onClick = { shopItem?.let { onUseItem(it) } },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("feed_button_${invItem.itemId}")
                                        ) {
                                            Text("Alimentar", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    ItemCategory.BRINQUEDO -> {
                                        Button(
                                            onClick = { shopItem?.let { onUseItem(it) } },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("play_button_${invItem.itemId}")
                                        ) {
                                            Text("Brincar", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    ItemCategory.MEDICAMENTO -> {
                                        Button(
                                            onClick = { shopItem?.let { onUseItem(it) } },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("medicine_button_${invItem.itemId}")
                                        ) {
                                            Text("Usar", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    ItemCategory.DECORACAO -> {
                                        Button(
                                            onClick = { onEquipItem(invItem) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (invItem.isEquipped) Color(0xFF3B82F6) else Color(0xFF6366F1)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("decor_button_${invItem.itemId}")
                                        ) {
                                            if (invItem.isEquipped) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Aplicado", fontWeight = FontWeight.Bold, color = Color.White)
                                            } else {
                                                Text("Aplicar", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    ItemCategory.ROUPA, ItemCategory.ACESSORIO -> {
                                        Button(
                                            onClick = { onEquipItem(invItem) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (invItem.isEquipped) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("equip_button_${invItem.itemId}")
                                        ) {
                                            if (invItem.isEquipped) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Equipado", fontWeight = FontWeight.Bold, color = Color.White)
                                            } else {
                                                Text("Equipar", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
