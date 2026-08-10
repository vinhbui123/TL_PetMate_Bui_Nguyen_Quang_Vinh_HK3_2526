package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.R
import com.example.petmate.model.Pet
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient.apiService
import com.example.petmate.ui.theme.*
import com.example.petmate.util.LocationHelper
import com.example.petmate.util.TimeHelper
import com.example.petmate.util.NotificationStorage

@Composable
fun PetDiscoveryScreen(
    currentUser: User? = null,
    onPetClick: (Pet) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminDashboardClick: () -> Unit = {},
    onNavigateToPostAd: () -> Unit = {},
    onBlockedUsersClick: () -> Unit = {},
    onPostHistoryClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onOrgProfileClick: () -> Unit = {},
    onOrgDashboardClick: () -> Unit = {},
    onOrgRegistrationClick: () -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    blockedUserIds: List<Long> = emptyList()
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var apiPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Filter states
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterMaxDistance by remember { mutableFloatStateOf(100f) }
    var filterArea by remember { mutableStateOf("") }

    var refreshTrigger by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory, refreshTrigger) {
        if (refreshTrigger > 0) isRefreshing = true
        if (refreshTrigger == 0) isLoading = true
        try {
            val fetchedPets = apiService.getPets(
                category = if (selectedCategory == "ALL") null else selectedCategory
            )
            // Lọc ra thú cưng Miễn phí (Nhận nuôi) và không thuộc người bị chặn
            apiPets = fetchedPets.filter {
                (it.price.isNullOrEmpty() || it.price.lowercase().contains("miễn phí") || it.price.trim() == "0" || it.price.trim() == "0 đ") &&
                (it.user?.id == null || !blockedUserIds.contains(it.user.id))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sample data for testing if server is down
            apiPets = listOf(
                Pet(
                    1, "PETER", "Beagle Dog", "1.5 years old", "20 pounds", "Male", "2.5km",
                    "Peter is a super friendly and needs so much love and attention..Peter love to play.",
                    null, null,
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
                com.example.petmate.ui.components.AppHeader(
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
                val context = LocalContext.current
                DiscoverySearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFilterClick = {
                        if (currentUser != null) {
                            showFilterSheet = true
                        } else {
                            Toast.makeText(context, "Vui lòng đăng nhập để sử dụng tìm kiếm nâng cao!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
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
                    val displayedPets = remember(apiPets, searchQuery, filterMaxDistance, filterArea, userLatitude, userLongitude) {
                        apiPets.filter {
                            val matchesSearch = searchQuery.isEmpty() ||
                                    (it.name?.contains(searchQuery, ignoreCase = true) == true) ||
                                    (it.breed?.contains(searchQuery, ignoreCase = true) == true)
                            
                            var matchesDistance = true
                            if (filterMaxDistance < 100f && userLatitude != null && userLongitude != null) {
                                val distance = LocationHelper.calculateDistance(
                                    userLatitude, userLongitude,
                                    it.latitude, it.longitude
                                )
                                if (distance != null) {
                                    matchesDistance = distance <= filterMaxDistance
                                } else {
                                    matchesDistance = false
                                }
                            }

                            val address = it.user?.address ?: ""
                            val matchesArea = filterArea.isEmpty() || address.contains(filterArea, ignoreCase = true)

                            matchesSearch && matchesDistance && matchesArea
                        }
                    }
                    PetList(displayedPets, onPetClick, currentUser, userLatitude, userLongitude)
                }
            }
            }
        }
    }
        if (showFilterSheet) {
            DiscoveryFilterBottomSheet(
                currentMaxDistance = filterMaxDistance,
                currentArea = filterArea,
                onDismissRequest = { showFilterSheet = false },
                onApplyFilter = { dist, area ->
                    filterMaxDistance = dist
                    filterArea = area
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
fun DiscoverySearchBar(
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {},
    onFilterClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp, Color(0xFFE8E0D8)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = PrimaryPeach,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Tìm thú cưng yêu thích...",
                        color = IconGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextGray),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onFilterClick() },
                shape = RoundedCornerShape(10.dp),
                color = PrimaryPeach
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_sort_by_size),
                        contentDescription = "Bộ lọc",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private val CATEGORIES = listOf(
    "ALL" to "🐾 Tất cả",
    "DOGS" to "🐶 Chó",
    "CATS" to "🐱 Mèo",
    "BIRDS" to "🦜 Chim cảnh",
    "FISH" to "🐟 Cá cảnh",
    "HAMSTERS" to "🐹 Hamster",
    "RABBITS" to "🐰 Thỏ",
    "POULTRY" to "🐔 Gia cầm",
    "OTHER" to "🦎 Khác"
)

@Composable
fun CategoryList(
    selectedCategory: String = "ALL",
    onCategorySelected: (String) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(CATEGORIES, key = { it.first }) { (code, label) ->
            val isSelected = code == selectedCategory
            val bgColor = if (isSelected) PrimaryPeach else CardWhite
            val textColor = if (isSelected) Color.White else TextGray

            Surface(
                modifier = Modifier
                    .height(42.dp)
                    .clickable { onCategorySelected(code) },
                shape = RoundedCornerShape(21.dp),
                color = bgColor,
                shadowElevation = if (isSelected) 6.dp else 1.dp,
                border = if (!isSelected) {
                    BorderStroke(1.dp, Color(0xFFE0E0E0))
                } else null
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun PetList(
    pets: List<Pet>,
    onPetClick: (Pet) -> Unit,
    currentUser: User? = null,
    userLatitude: Double? = null,
    userLongitude: Double? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = pets, 
            key = { it.id },
            contentType = { "pet_card" } // Helps LazyColumn reuse compositions efficiently
        ) { pet ->
            PetCard(pet, onPetClick, currentUser, userLatitude, userLongitude)
        }
    }
}

@Composable
fun PetCard(
    pet: Pet,
    onPetClick: (Pet) -> Unit,
    currentUser: User? = null,
    userLatitude: Double? = null,
    userLongitude: Double? = null
) {
    val isOrg = pet.organization != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onPetClick(pet) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Container
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(pet.imageUrl)
                            .crossfade(true)
                            .size(300)
                            .build(),
                        contentDescription = pet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(pet.imageRes),
                        contentDescription = pet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Badge overlay at top-left of image
                Surface(
                    color = (if (isOrg) SuccessGreen else AccentOrange).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = if (isOrg) Icons.Default.Business else Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp).size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pet.name ?: "Chưa có tên",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepBrown,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pet.breed ?: "Chưa rõ",
                            style = MaterialTheme.typography.bodySmall,
                            color = IconGray
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (pet.likeCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null, 
                            tint = if (pet.likeCount > 0) HeartRed else IconGray, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = pet.likeCount.toString(), style = MaterialTheme.typography.labelSmall, color = TextGray, fontWeight = FontWeight.Bold)
                    }
                }

                Column {
                    val isFree = pet.price.isNullOrEmpty() || pet.price == "0" || pet.price == "0.0" || pet.price.lowercase().contains("miễn phí")
                    val displayPrice = if (isFree) "Miễn phí" else "${pet.price}đ"
                    
                    Text(
                        text = displayPrice,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFree) PrimaryPeach else ErrorRed
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = IconGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        val locationText = remember(pet.latitude, pet.longitude, userLatitude, userLongitude) {
                            LocationHelper.getDistanceText(
                                userLatitude, userLongitude,
                                pet.latitude,
                                pet.longitude
                            )?.let { "Cách $it" } ?: "Chưa rõ"
                        }
                        val timeText = remember(pet.createdAt) {
                            TimeHelper.getRelativeTime(pet.createdAt)
                        }

                        Text(
                            text = "$locationText • $timeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = IconGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryFilterBottomSheet(
    currentMaxDistance: Float,
    currentArea: String,
    onDismissRequest: () -> Unit,
    onApplyFilter: (maxDistance: Float, area: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var maxDistance by remember { mutableFloatStateOf(currentMaxDistance) }
    var area by remember { mutableStateOf(currentArea) }

    // Optimization: Use derivedStateOf to prevent excessive UI re-calculation during slider drags
    val distanceLabel by remember {
        derivedStateOf {
            if (maxDistance >= 100f) "Toàn quốc" else "${maxDistance.toInt()} km"
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
                    maxDistance = 100f
                    area = ""
                }) {
                    Text("Đặt lại", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Distance Filter
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Khoảng cách tối đa", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextGray)
                    Text(
                        distanceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }
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

            Spacer(modifier = Modifier.height(24.dp))
            
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
                onClick = { onApplyFilter(maxDistance, area) },
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PetDiscoveryScreenPreview() {
    PetMateTheme {
        PetDiscoveryScreen()
    }
}
