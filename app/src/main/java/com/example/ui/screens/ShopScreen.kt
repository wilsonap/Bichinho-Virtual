package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InventoryEntity
import com.example.data.local.PetEntity
import com.example.data.model.ItemCategory
import com.example.data.model.ShopCatalog
import com.example.data.model.ShopItem
import com.example.ui.components.CoinBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    userCoins: Int,
    pet: PetEntity? = null,
    inventory: List<InventoryEntity> = emptyList(),
    onBuyItem: (ShopItem, Boolean) -> Unit = { _, _ -> },
    onEquipItem: (ShopItem) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(ItemCategory.ALIMENTO) }
    var itemToConfirmEquip by remember { mutableStateOf<ShopItem?>(null) }

    val filteredItems = remember(selectedCategory) {
        ShopCatalog.items.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loja do Bichinho", fontWeight = FontWeight.Bold) },
                actions = {
                    CoinBadge(coins = userCoins, modifier = Modifier.padding(end = 16.dp))
                }
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
                modifier = Modifier.fillMaxWidth().testTag("shop_category_tabs")
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

            // Products Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("shop_items_grid"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems) { item ->
                    val canAfford = userCoins >= item.price
                    val ownedItem = inventory.find { it.itemId == item.id }
                    val isOwned = ownedItem != null && ownedItem.quantity > 0
                    val isEquipped = when (item.category) {
                        ItemCategory.ROUPA -> pet?.equippedHat == item.id
                        ItemCategory.ACESSORIO -> pet?.equippedAccessory == item.id
                        ItemCategory.DECORACAO -> pet?.roomTheme == item.id
                        else -> false
                    }
                    val isWearable = item.category in listOf(ItemCategory.ROUPA, ItemCategory.ACESSORIO, ItemCategory.DECORACAO)

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEquipped) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isEquipped) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6)) else null,
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("shop_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = item.iconEmoji, fontSize = 36.sp)
                                    }
                                }

                                if (isEquipped) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Equipado",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp).padding(2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                minLines = 2,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isWearable && isOwned) {
                                Button(
                                    onClick = { onEquipItem(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEquipped) Color(0xFF3B82F6) else Color(0xFF10B981)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("buy_button_${item.id}")
                                ) {
                                    Text(
                                        text = if (isEquipped) "✓ Equipado" else "Equipar",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (isWearable) {
                                            itemToConfirmEquip = item
                                        } else {
                                            onBuyItem(item, false)
                                        }
                                    },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        disabledContainerColor = Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("buy_button_${item.id}")
                                ) {
                                    Text(
                                        text = "🪙 ${item.price}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (canAfford) Color(0xFF78350F) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for confirming whether to equip immediately on purchase
    itemToConfirmEquip?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToConfirmEquip = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Comprar ${item.name}", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Preço: 🪙 ${item.price} moedas.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Deseja equipar este item no seu bichinho imediatamente após a compra?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyItem(item, true)
                        itemToConfirmEquip = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Comprar e Equipar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onBuyItem(item, false)
                        itemToConfirmEquip = null
                    }
                ) {
                    Text("Apenas Comprar")
                }
            }
        )
    }
}
