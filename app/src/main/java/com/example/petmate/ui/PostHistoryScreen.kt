package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.PrimaryPeach
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostHistoryScreen(onBack: () -> Unit, onPetClick: (Pet) -> Unit = {}) {
    var myPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            myPets = NetworkClient.apiService.getMyPets()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lí tin đăng") },
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
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPeach)
                }
                myPets.isEmpty() -> {
                    Text(
                        text = "Bạn chưa đăng tin nào.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(myPets) { pet ->
                            PostHistoryCard(pet = pet, onClick = { onPetClick(pet) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostHistoryCard(pet: Pet, onClick: () -> Unit = {}) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    
    val statusText = when (pet.status) {
        "PENDING" -> "Chờ duyệt"
        "AVAILABLE" -> "Đang hiển thị"
        "REJECTED" -> "Bị từ chối"
        "SOLD" -> "Đã giao dịch"
        "HIDDEN" -> "Đã ẩn"
        else -> pet.status ?: "Không rõ"
    }

    val statusColor = when (pet.status) {
        "PENDING" -> Color(0xFFFFA000)
        "AVAILABLE" -> Color(0xFF4CAF50)
        "REJECTED" -> Color.Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = "Pet Image",
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(pet.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Giống: ${pet.breed} - Tuổi: ${pet.age}", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = if ((pet.price?.toDoubleOrNull() ?: 0.0) > 0) formatter.format(pet.price?.toDoubleOrNull() ?: 0.0) else "Miễn phí (Nhận nuôi)",
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPeach,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trạng thái:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(statusText, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 14.sp)
            }
        }
    }
}
