package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import com.example.petmate.util.TimeHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostHistoryScreen(onBack: () -> Unit, onPetClick: (Pet) -> Unit = {}) {
    var myPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Đang hiển thị", "Chờ duyệt", "Tin khác")

    LaunchedEffect(Unit) {
        try {
            myPets = NetworkClient.apiService.getMyPets()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredPets = remember(myPets, selectedTab) {
        when (selectedTab) {
            0 -> myPets.filter { it.status == "AVAILABLE" }
            1 -> myPets.filter { it.status == "PENDING" }
            else -> myPets.filter { it.status != "AVAILABLE" && it.status != "PENDING" }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý tin đăng", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tab Header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryPeach,
                divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontSize = 14.sp, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            ) 
                        },
                        selectedContentColor = PrimaryPeach,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPeach)
                    }
                    filteredPets.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Không tìm thấy tin đăng nào.",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredPets) { pet ->
                                PostHistoryCard(pet = pet, onClick = { onPetClick(pet) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostHistoryCard(pet: Pet, onClick: () -> Unit = {}) {
    val statusLabel = when (pet.status) {
        "PENDING" -> "Chờ duyệt"
        "AVAILABLE" -> "Đang hiển thị"
        "REJECTED" -> "Bị từ chối"
        "SOLD" -> "Đã bán"
        "HIDDEN" -> "Đã ẩn"
        "REQUIRES_REVIEW" -> "Cần kiểm duyệt"
        else -> pet.status ?: "Không rõ"
    }

    val statusColor = when (pet.status) {
        "PENDING" -> Color(0xFFFFA000)
        "AVAILABLE" -> SuccessGreen
        "REJECTED" -> ErrorRed
        "SOLD" -> Color.Gray
        "HIDDEN" -> Color.Gray
        else -> Color.DarkGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Pet Image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = "Ảnh thú cưng",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).align(Alignment.Center),
                        tint = Color.LightGray
                    )
                }
                
                // Status Badge Overlay
                Surface(
                    color = statusColor.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                ) {
                    Text(
                        text = statusLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Information Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name ?: "Chưa có tên",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val priceText = remember(pet.price) {
                    if (pet.price.isNullOrBlank() || pet.price == "Miễn phí" || pet.price == "0" || pet.price == "0.0") {
                        "Miễn phí"
                    } else {
                        try {
                            val amount = pet.price.replace(Regex("[^0-9]"), "").toLong()
                            val formatterPrice = java.text.DecimalFormat("#,###")
                            formatterPrice.format(amount).replace(",", ".") + " đ"
                        } catch (_: Exception) {
                            pet.price
                        }
                    }
                }
                
                Text(
                    text = priceText,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (priceText == "Miễn phí") SuccessGreen else Color(0xFFE53935),
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ngày đăng: ${TimeHelper.getRelativeTime(pet.createdAt)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    if (pet.status == "REJECTED" && !pet.redListNote.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Lý do",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
