package com.example.petmate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.R
import com.example.petmate.ui.theme.*

import androidx.compose.runtime.LaunchedEffect
import com.example.petmate.model.Pet
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient.apiService
import com.example.petmate.ui.components.AppHeader
import com.example.petmate.util.LocationHelper

@Composable
fun PetMarketScreen(
    onItemClick: (Pet) -> Unit = {},
    onNavigateToPostAd: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminDashboardClick: () -> Unit = {},
    onBlockedUsersClick: () -> Unit = {},
    onPostHistoryClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onOrgProfileClick: () -> Unit = {},
    onOrgDashboardClick: () -> Unit = {},
    onOrgRegistrationClick: () -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    currentUser: User? = null,
    blockedUserIds: List<Long> = emptyList()
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var apiPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Filter states
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterMaxPrice by remember { mutableFloatStateOf(20f) }
    var filterMaxDistance by remember { mutableFloatStateOf(100f) }
    var filterMinRating by remember { mutableFloatStateOf(0f) }
    var filterArea by remember { mutableStateOf("") }
    
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory, refreshTrigger) {
        if (refreshTrigger > 0) isRefreshing = true
        if (refreshTrigger == 0) isLoading = true
        try {
            val fetchedPets = apiService.getPets(
                category = if (selectedCategory == "ALL") null else selectedCategory
            )
            // Lọc ra thú cưng Có phí (Mua bán) và không thuộc người bị chặn
            apiPets = fetchedPets.filter {
                !it.price.isNullOrEmpty() && !it.price.lowercase().contains("miễn phí")
                        && it.price.trim() != "0" && it.price.trim() != "0 đ" &&
                (it.user?.id == null || !blockedUserIds.contains(it.user.id))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sample data for testing if server is down
            apiPets = listOf(
                Pet(
                    id = 1,
                    name = "Bé Beagle thuần chủng chân siêu cute",
                    breed = "Beagle",
                    age = "1.5 years old",
                    weight = "20 pounds",
                    sex = "Male",
                    about = "Bé siêu dễ thương.",
                    imageUrl = null,
                    price = "5.500.000 đ",
                    imageRes = R.drawable.beagle_dog
                )
            )
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    Surface(
        color = BackgroundBeige,
        modifier = Modifier.fillMaxSize()
    ) {
        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshTrigger++ },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AppHeader(
                    currentUser = currentUser,
                    onLogoutClick = onLogoutClick,
                    onProfileClick = onProfileClick,
                    onAdminDashboardClick = onAdminDashboardClick,
                    onBlockedUsersClick = onBlockedUsersClick,
                    onPostHistoryClick = onPostHistoryClick,
                    onNotificationsClick = onNotificationsClick,
                    onOrgProfileClick = onOrgProfileClick,
                    onOrgDashboardClick = onOrgDashboardClick,
                    onOrgRegistrationClick = onOrgRegistrationClick
                )
            
            // Search Bar
            DiscoverySearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onFilterClick = { showFilterSheet = true }
            )
            
            // Category List (Reused from PetAdoptScreen)
            CategoryList(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryPeach)
                        }
                    }
                    else -> {
                        val displayedPets = remember(apiPets, searchQuery, filterMaxPrice, filterMaxDistance, filterMinRating, filterArea, userLatitude, userLongitude) {
                            apiPets.filter {
                                val matchesSearch = searchQuery.isEmpty() ||
                                        (it.name?.contains(searchQuery, ignoreCase = true) == true) ||
                                        (it.breed?.contains(searchQuery, ignoreCase = true) == true)
                                
                                val numericPrice = it.price?.replace(Regex("[^0-9]"), "")?.toLongOrNull() ?: 0L
                                val priceInMillion = numericPrice / 1_000_000f
                                val matchesPrice = filterMaxPrice >= 20f || priceInMillion <= filterMaxPrice
                                
                                var matchesDistance = true
                                if (filterMaxDistance < 100f && userLatitude != null && userLongitude != null) {
                                    val distance = LocationHelper.calculateDistance(
                                        userLatitude, userLongitude,
                                        it.latitude, it.longitude
                                    )
                                    if (distance != null) {
                                        matchesDistance = distance <= filterMaxDistance
                                    } else {
                                        matchesDistance = false // No location data
                                    }
                                }

                                val rating = it.user?.averageRating ?: 0.0
                                val matchesRating = rating >= filterMinRating

                                val address = it.user?.address ?: ""
                                val matchesArea = filterArea.isEmpty() || address.contains(filterArea, ignoreCase = true)

                                matchesSearch && matchesPrice && matchesDistance && matchesRating && matchesArea
                            }
                        }
                        // Market Grid
                        MarketGrid(items = displayedPets, onItemClick = onItemClick,
                            currentUser = currentUser, userLatitude = userLatitude,
                            userLongitude = userLongitude)
                    }
                }
            }
        }

        }
        if (showFilterSheet) {
            FilterBottomSheet(
                currentMaxPrice = filterMaxPrice,
                currentMaxDistance = filterMaxDistance,
                currentMinRating = filterMinRating,
                currentArea = filterArea,
                onDismissRequest = { showFilterSheet = false },
                onApplyFilter = { price, dist, rate, area ->
                    filterMaxPrice = price
                    filterMaxDistance = dist
                    filterMinRating = rate
                    filterArea = area
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
fun MarketGrid(
    items: List<Pet>,
    onItemClick: (Pet) -> Unit,
    currentUser: User? = null,
    userLatitude: Double? = null,
    userLongitude: Double? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 80.dp), // Extra bottom padding for FAB
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = items, 
            key = { it.id },
            contentType = { "market_item" } // Optimize reuse
        ) { item ->
            com.example.petmate.ui.components.MarketItemCard(
                item = item,
                onClick = onItemClick,
                userLatitude = userLatitude,
                userLongitude = userLongitude
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetMarketScreenPreview() {
    PetMateTheme {
        PetMarketScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentMaxPrice: Float,
    currentMaxDistance: Float,
    currentMinRating: Float,
    currentArea: String,
    onDismissRequest: () -> Unit,
    onApplyFilter: (maxPrice: Float, maxDistance: Float, minRating: Float, area: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var maxPrice by remember { mutableFloatStateOf(currentMaxPrice) }
    var maxDistance by remember { mutableFloatStateOf(currentMaxDistance) }
    var minRating by remember { mutableFloatStateOf(currentMinRating) }
    var area by remember { mutableStateOf(currentArea) }

    // Optimization: Pre-calculate labels using derivedStateOf to prevent UI jank during slider dragging
    val priceLabel by remember {
        derivedStateOf {
            if (maxPrice >= 20f) "Không giới hạn" else "${maxPrice.toInt()} triệu"
        }
    }
    val distanceLabel by remember {
        derivedStateOf {
            if (maxDistance >= 100f) "Toàn quốc" else "${maxDistance.toInt()} km"
        }
    }
    val ratingLabel by remember {
        derivedStateOf {
            "Từ ${minRating.toInt()} sao"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CardWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PrimaryPeach) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bộ lọc nâng cao",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextGray
                )
                TextButton(onClick = {
                    maxPrice = 20f
                    maxDistance = 100f
                    minRating = 0f
                    area = ""
                }) {
                    Text("Đặt lại", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Price Filter
            FilterSection(
                title = "Mức giá tối đa",
                valueText = priceLabel
            ) {
                Slider(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    valueRange = 0f..20f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange,
                        inactiveTrackColor = SoftPeach
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Distance Filter
            FilterSection(
                title = "Khoảng cách tối đa",
                valueText = distanceLabel
            ) {
                Slider(
                    value = maxDistance,
                    onValueChange = { maxDistance = it },
                    valueRange = 1f..100f,
                    steps = 98,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange,
                        inactiveTrackColor = SoftPeach
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Rating Filter
            FilterSection(
                title = "Đánh giá người bán",
                valueText = ratingLabel
            ) {
                Slider(
                    value = minRating,
                    onValueChange = { minRating = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange,
                        inactiveTrackColor = SoftPeach
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Khu vực",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                placeholder = { Text("VD: Hà Nội, TP.HCM", color = IconGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange,
                    unfocusedBorderColor = InputBorder,
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground
                ),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentOrange) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onApplyFilter(maxPrice, maxDistance, minRating, area) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Áp dụng bộ lọc", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    valueText: String,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextGray)
            Text(valueText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentOrange)
        }
        content()
    }
}
