package com.example.petmate.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    blockedUserIds: List<Long> = emptyList()
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var apiPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedCategory) {
        isLoading = true
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
        }
    }


    Scaffold(
        containerColor = BackgroundBeige,
        topBar = {
            Header(
                currentUser = currentUser,
                onLogoutClick = onLogoutClick,
                onProfileClick = onProfileClick,
                onAdminDashboardClick = onAdminDashboardClick,
                onBlockedUsersClick = onBlockedUsersClick,
                onPostHistoryClick = onPostHistoryClick,
                onNotificationsClick = onNotificationsClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            DiscoverySearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            CategoryList(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPeach)
                    }
                }
                else -> {
                    val displayedPets = remember(apiPets, searchQuery) {
                        apiPets.filter {
                            searchQuery.isEmpty() ||
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.breed.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    PetList(displayedPets, onPetClick, currentUser, userLatitude, userLongitude)
                }
            }
        }
    }
}

@Composable
fun Header(
    currentUser: User? = null,
    onLogoutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminDashboardClick: () -> Unit = {},
    onBlockedUsersClick: () -> Unit = {},
    onPostHistoryClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var unreadCount by remember { mutableStateOf(0) }

    // Polling cập nhật số thông báo chưa đọc mỗi 3 giây
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val storage = NotificationStorage(context)
            while (true) {
                unreadCount = storage.getUnreadCount()
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (currentUser == null) {
                Modifier.clickable { onLogoutClick() }.weight(1f)
            } else {
                Modifier.weight(1f)
            }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            ) {
                if (!currentUser?.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentUser.avatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser?.fullName ?: "Khách vãng lai",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    maxLines = 1
                )
                if (currentUser == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = PrimaryPeach,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đăng nhập ngay",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPeach,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryPeach,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentUser.address ?: "Chưa cập nhật địa chỉ",
                            style = MaterialTheme.typography.bodySmall,
                            color = IconGray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        if (currentUser != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clickable { onNotificationsClick() }.padding(end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextGray,
                        modifier = Modifier.size(28.dp)
                    )
                    if (unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Red,
                            modifier = Modifier.size(16.dp).offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Box {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextGray,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { expanded = true }
                    )
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.White)) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White).width(180.dp)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Person, 
                                    contentDescription = null, 
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            text = { Text("Hồ sơ cá nhân", color = TextGray, fontWeight = FontWeight.Medium) },
                            onClick = {
                                expanded = false
                                onProfileClick()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Menu, // Or use History icon if available
                                    contentDescription = null, 
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            text = { Text("Lịch sử đăng tin", color = TextGray, fontWeight = FontWeight.Medium) },
                            onClick = {
                                expanded = false
                                onPostHistoryClick()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null, 
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            text = { Text("Tài khoản bị chặn", color = TextGray, fontWeight = FontWeight.Medium) },
                            onClick = {
                                expanded = false
                                onBlockedUsersClick()
                            }
                        )
                        if (currentUser.role == "ADMIN") {
                            DropdownMenuItem(
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Dashboard, 
                                        contentDescription = null, 
                                        tint = TextGray,
                                        modifier = Modifier.size(20.dp)
                                    ) 
                                },
                                text = { Text("Quản trị hệ thống", color = TextGray, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    expanded = false
                                    onAdminDashboardClick()
                                }
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        DropdownMenuItem(
                            text = { Text("Đăng xuất", color = Color.Red) },
                            onClick = {
                                expanded = false
                                onLogoutClick()
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun DiscoverySearchBar(
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {}
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
                modifier = Modifier.size(36.dp),
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
        items(pets, key = { it.id }) { pet ->
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp) // Kéo dài khung Surface
            .padding(vertical = 4.dp)
            .clickable { onPetClick(pet) },
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = Color.LightGray
            ) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
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
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pet.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextGray,
                            maxLines = 1
                        )
                        Text(
                            text = pet.breed,
                            style = MaterialTheme.typography.bodySmall,
                            color = IconGray
                        )
                    }
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = HeartRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = pet.age,
                        style = MaterialTheme.typography.bodySmall,
                        color = IconGray
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryPeach,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val locationText = remember(pet.latitude, pet.longitude, userLatitude, userLongitude) {
                            LocationHelper.getDistanceText(
                                userLatitude, userLongitude,
                                pet.latitude,
                                pet.longitude
                            )?.let { "Cách $it" } ?: "Chưa rõ khoảng cách"
                        }
                        val timeText = remember(pet.createdAt) {
                            TimeHelper.getRelativeTime(pet.createdAt)
                        }
                        Text(
                            text = "$locationText • $timeText",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                    }
                }
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
