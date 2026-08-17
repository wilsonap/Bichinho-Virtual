package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InventoryEntity
import com.example.data.local.PetEntity
import com.example.data.model.FoodPreferenceCatalog
import com.example.data.model.FoodShopOrganization
import com.example.data.model.FoodShopSubcategory
import com.example.data.model.PetDisease
import com.example.data.model.PetHealthRules
import com.example.data.model.PetHealthState
import com.example.data.model.ItemCategory
import com.example.data.model.ShopCatalog
import com.example.data.model.ShopItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventory: List<InventoryEntity>,
    pet: PetEntity? = null,
    onUseItem: (ShopItem) -> Unit,
    onEquipItem: (InventoryEntity) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ItemCategory.ALIMENTO) }
    var selectedFoodSubcategory by remember { mutableStateOf(FoodShopSubcategory.FAVORITOS) }

    // Defensive grouping by itemId: guarantees a single card per distinct item
    val consolidatedInventory = remember(inventory) {
        inventory.groupBy { it.itemId }.values.mapNotNull { group ->
            val first = group.firstOrNull() ?: return@mapNotNull null
            val isReusable = first.category in listOf(
                ItemCategory.BRINQUEDO.name,
                ItemCategory.ROUPA.name,
                ItemCategory.ACESSORIO.name,
                ItemCategory.DECORACAO.name
            )
            val totalQuantity = if (isReusable) 1 else group.sumOf { it.quantity }
            val anyEquipped = group.any { it.isEquipped }
            first.copy(quantity = totalQuantity, isEquipped = anyEquipped)
        }
    }

    val filteredInventory = remember(
        consolidatedInventory,
        selectedCategory,
        selectedFoodSubcategory,
        pet?.speciesId
    ) {
        val inCategory = consolidatedInventory.filter {
            it.category == selectedCategory.name && it.quantity > 0
        }
        if (selectedCategory != ItemCategory.ALIMENTO) {
            inCategory
        } else {
            val allowedIds = FoodShopOrganization
                .filterShopFoods(selectedFoodSubcategory, pet?.speciesId)
                .map { it.id }
                .toSet()
            inCategory.filter { it.itemId in allowedIds }
        }
    }

    val catalogMap = remember { ShopCatalog.items.associateBy { it.id } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Mochila / Inventário",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs with smooth scroll and standardized styling
            ScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                edgePadding = 16.dp,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                },
                modifier = Modifier.fillMaxWidth().testTag("inventory_category_tabs")
            ) {
                ItemCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        text = {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            if (selectedCategory == ItemCategory.ALIMENTO) {
                FoodSubcategoryChipRow(
                    selected = selectedFoodSubcategory,
                    onSelect = { selectedFoodSubcategory = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("inventory_food_subcategory_row")
                )
            }

            // Medicine Status Header
            if (selectedCategory == ItemCategory.MEDICAMENTO && pet != null) {
                val healthState = PetHealthRules.getHealthState(pet.health)
                val disease = PetDisease.entries.find { it.name.equals(pet.disease, ignoreCase = true) } ?: PetDisease.NONE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (healthState) {
                        PetHealthState.SAUDAVEL -> Color(0xFFDCFCE7)
                        PetHealthState.INDISPOSTO -> Color(0xFFFEF3C7)
                        PetHealthState.DOENTE -> Color(0xFFFFEDD5)
                        PetHealthState.CRITICO -> Color(0xFFFEE2E2)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = when (healthState) {
                                PetHealthState.SAUDAVEL -> Color(0xFF15803D)
                                PetHealthState.INDISPOSTO -> Color(0xFFB45309)
                                PetHealthState.DOENTE -> Color(0xFFC2410C)
                                PetHealthState.CRITICO -> Color(0xFFB91C1C)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Saúde: ${pet.health}/100 • ${healthState.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (healthState) {
                                    PetHealthState.SAUDAVEL -> Color(0xFF15803D)
                                    PetHealthState.INDISPOSTO -> Color(0xFFB45309)
                                    PetHealthState.DOENTE -> Color(0xFFC2410C)
                                    PetHealthState.CRITICO -> Color(0xFFB91C1C)
                                }
                            )
                            Text(
                                text = if (disease != PetDisease.NONE) {
                                    "Diagnóstico: ${disease.displayName} • ${disease.recommendedCure}"
                                } else {
                                    "Bichinho 100% saudável! Medicamentos fortalecem a imunidade."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (filteredInventory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(
                            if (selectedCategory == ItemCategory.ALIMENTO &&
                                selectedFoodSubcategory == FoodShopSubcategory.FAVORITOS
                            ) {
                                "inventory_favorites_empty"
                            } else {
                                "inventory_category_empty"
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = "🎒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedCategory == ItemCategory.ALIMENTO &&
                                selectedFoodSubcategory == FoodShopSubcategory.FAVORITOS
                            ) {
                                "Nenhum alimento favorito encontrado para esta espécie."
                            } else {
                                "Nenhum item nesta categoria."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Visite a loja para adquirir novidades!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columnCount = if (maxWidth >= 600.dp) 3 else 2
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("inventory_items_grid"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredInventory) { invItem ->
                            val shopItem = ShopCatalog.findItemById(invItem.itemId)
                            val isFavoriteFood = selectedCategory == ItemCategory.ALIMENTO &&
                                pet != null &&
                                FoodPreferenceCatalog.isFavorite(pet.speciesId, invItem.itemId)

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (invItem.isEquipped) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (invItem.isEquipped) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6)) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth().testTag("inv_card_${invItem.itemId}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.size(62.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = shopItem?.iconEmoji ?: "📦", fontSize = 32.sp)
                                            }
                                        }

                                        if (isFavoriteFood) {
                                            Text(
                                                text = "❤️",
                                                fontSize = 14.sp,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .offset(x = (-2).dp, y = (-2).dp)
                                                    .testTag("inv_favorite_${invItem.itemId}")
                                            )
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

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = invItem.name.ifBlank { shopItem?.name ?: "Item" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )

                                    if (isFavoriteFood) {
                                        Text(
                                            text = "❤️ Favorito",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE11D48),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    if (selectedCategory == ItemCategory.MEDICAMENTO && shopItem != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val disease = PetDisease.entries.find { it.name.equals(pet?.disease, ignoreCase = true) } ?: PetDisease.NONE
                                        val isRecommended = when (shopItem.id) {
                                            "med_potion" -> disease != PetDisease.NONE || (pet?.health ?: 100) < 60
                                            "med_cold" -> disease == PetDisease.RESFRIADO
                                            "med_digestive" -> disease == PetDisease.INDIGESTAO
                                            "med_vitamin" -> disease == PetDisease.FADIGA || (pet?.energy ?: 100) < 40
                                            else -> false
                                        }

                                        if (isRecommended) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF3C7),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Text(
                                                    text = "⭐ Recomendado",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = shopItem.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    when (selectedCategory) {
                                        ItemCategory.ALIMENTO -> {
                                            Button(
                                                onClick = { shopItem?.let { onUseItem(it) } },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("feed_button_${invItem.itemId}")
                                            ) {
                                                Text(
                                                    text = "Alimentar",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        ItemCategory.BRINQUEDO -> {
                                            Button(
                                                onClick = { shopItem?.let { onUseItem(it) } },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("play_button_${invItem.itemId}")
                                            ) {
                                                Text(
                                                    text = "Brincar",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        ItemCategory.MEDICAMENTO -> {
                                            Button(
                                                onClick = { shopItem?.let { onUseItem(it) } },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("medicine_button_${invItem.itemId}")
                                            ) {
                                                Text(
                                                    text = "Usar",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        ItemCategory.DECORACAO -> {
                                            Button(
                                                onClick = { onEquipItem(invItem) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (invItem.isEquipped) Color(0xFF3B82F6) else Color(0xFF6366F1)
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("decor_button_${invItem.itemId}")
                                            ) {
                                                if (invItem.isEquipped) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Aplicado",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Aplicar",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        ItemCategory.ROUPA, ItemCategory.ACESSORIO -> {
                                            Button(
                                                onClick = { onEquipItem(invItem) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (invItem.isEquipped) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp).testTag("equip_button_${invItem.itemId}")
                                            ) {
                                                if (invItem.isEquipped) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Equipado",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Equipar",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
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
}
