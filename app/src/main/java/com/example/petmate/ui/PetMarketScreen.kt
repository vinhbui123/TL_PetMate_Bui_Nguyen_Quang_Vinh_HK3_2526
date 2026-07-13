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
import androidx.compose.material.icons.filled.Add
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

@Composable
fun PetMarketScreen(
    onItemClick: (Pet) -> Unit = {},
    onNavigateToPostAd: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var apiPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedCategory) {
        isLoading = true
        try {
            val fetchedPets = com.example.petmate.network.RetrofitClient.apiService.getPets(
                category = if (selectedCategory == "ALL") null else selectedCategory
            )
            // Lọc ra thú cưng có giá (Mua bán)
            apiPets = fetchedPets.filter {
                !it.price.isNullOrEmpty() && !it.price.lowercase().contains("miễn phí") && it.price.trim() != "0" && it.price.trim() != "0 đ"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sample data for testing if server is down
            apiPets = listOf(
                Pet(
                    1, "Bé Beagle thuần chủng chân siêu cute", "Beagle", "1.5 years old", "20 pounds", "Male", "Q. Bình Thạnh, TP.HCM",
                    "Bé siêu dễ thương.", null, "5.500.000 đ", R.drawable.beagle_dog
                )
            )
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = BackgroundBeige,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToPostAd,
                containerColor = PrimaryPeach,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Đăng tin")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng tin", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Market Header
            MarketHeader()
            
            // Search Bar
            SearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            
            // Category List (Reused from PetAdoptScreen)
            CategoryList(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPeach)
                }
            } else {
                val displayedPets = apiPets.filter {
                    searchQuery.isEmpty() || 
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    it.breed.contains(searchQuery, ignoreCase = true)
                }
                // Market Grid
                MarketGrid(items = displayedPets, onItemClick = onItemClick)
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
fun MarketGrid(items: List<Pet>, onItemClick: (Pet) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 80.dp), // Extra bottom padding for FAB
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            MarketItemCard(item, onItemClick)
        }
    }
}

@Composable
fun MarketItemCard(item: Pet, onClick: (Pet) -> Unit) {
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
                    Text(
                        text = "Cách ${item.distance}",
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
