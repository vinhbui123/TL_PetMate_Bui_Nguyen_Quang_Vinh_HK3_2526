    package com.example.petmate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.R
import com.example.petmate.ui.theme.*

import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient.apiService
import com.example.petmate.util.LocationHelper
import com.example.petmate.util.TimeHelper

    @Composable
fun PetMarketScreen(
    onItemClick: (Pet) -> Unit = {},
    onNavigateToPostAd: () -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    currentUser: User? = null,
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
        }
    }

    Scaffold(
        containerColor = BackgroundBeige
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Market Header
            MarketHeader()
            
            // Search Bar
            DiscoverySearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            
            // Category List (Reused from PetAdoptScreen)
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
                    // Market Grid
                    MarketGrid(items = displayedPets, onItemClick = onItemClick,
                        currentUser = currentUser, userLatitude = userLatitude,
                        userLongitude = userLongitude)
                }
            }
        }
    }
}

@Composable
fun MarketHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Chợ Thú Cưng",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextGray
        )
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
        items(items, key = { it.id }) { item ->
            MarketItemCard(item, onItemClick, currentUser, userLatitude, userLongitude)
        }
    }
}

@Composable
fun MarketItemCard(
    item: Pet,
    onClick: (Pet) -> Unit,
    currentUser: User? = null,
    userLatitude: Double? = null,
    userLongitude: Double? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        shape = RoundedCornerShape(12.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square image
                    .background(Color.LightGray)
            ) {
                if (!item.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(item.imageRes),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Details
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.height(36.dp) // Fixed height for 2 lines
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = item.price ?: "Thỏa thuận",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F) // Red for price
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = IconGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val locationText = remember(item.latitude, item.longitude, userLatitude, userLongitude) {
                        LocationHelper.getDistanceText(
                            userLatitude, userLongitude,
                            item.latitude,
                            item.longitude
                        )?.let { "Cách $it" } ?: "Chưa rõ khoảng cách"
                    }
                    val timeText = remember(item.createdAt) {
                        TimeHelper.getRelativeTime(item.createdAt)
                    }
                    Text(
                        text = "$locationText • $timeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = IconGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
