package com.example.petmate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.R
import com.example.petmate.model.Pet
import com.example.petmate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailsScreen(
    pet: Pet,
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            BottomActionBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Image Box with Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
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
                
                // Back Button overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    IconButton(
                        onClick = { /* Favorite */ },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Lưu tin", tint = Color.White)
                    }
                }
            }
            
            // Price & Title Section (White bg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val priceText = pet.price?.takeIf { it.isNotBlank() && it != "Miễn phí" } ?: "Miễn phí"
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935) // Red price like Cho Tot
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = pet.distance ?: "Chưa rõ khoảng cách", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Tin đăng 2 giờ trước", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp)) // Divider gap
            
            // Seller Profile Section (White bg)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo), // Mock avatar
                    contentDescription = "Seller Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Trạm Cứu Hộ Thú Cưng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Đang hoạt động", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                OutlinedButton(
                    onClick = { /* View Profile */ },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Xem trang", color = PrimaryPeach)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Characteristics (Minimalist list without heavy icons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text("Đặc điểm thú cưng", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SpecRow("Giống", pet.breed)
                        SpecRow("Độ tuổi", pet.age)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SpecRow("Giới tính", pet.sex)
                        SpecRow("Trọng lượng", pet.weight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Description Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text("Mô tả chi tiết", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = pet.about ?: "Đang cập nhật",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Safety Warning Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF8E1)) // Light yellow warning
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = "Security", tint = Color(0xFFFBC02D))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Giao dịch an toàn: Tuyệt đối không chuyển tiền cọc trước khi gặp mặt và kiểm tra thú cưng.",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SpecRow(label: String, value: String?) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value ?: "Đang cập nhật", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BottomActionBar() {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat Button
            OutlinedButton(
                onClick = { /* Chat */ },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPeach)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat", fontWeight = FontWeight.Bold)
            }
            
            // Adopt/Buy Button
            Button(
                onClick = { /* Buy */ },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) {
                Text("Giao dịch", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PetDetailsScreenPreview() {
    val samplePet = Pet(
        1, "Bé Mèo Nga lai Ta rất ngoan và dể nuôi", "Mèo Nga lai", "1.5 năm", "2.5 kg", "Cái", "2.5 km",
        "Bé mèo Nga lai ta, màu trắng xám, được 1 tuổi rưỡi, cân nặng khoảng 2.5kg. Bé ăn được hạt và pate, đi vệ sinh đúng chỗ trong thau cát. Bé rất ngoan, quấn chủ, không cào cắn đồ đạc. Do chuyển trọ không cho nuôi chó mèo nên mình cần tìm chủ mới yêu thương bé.",
        null, "Miễn phí", R.drawable.beagle_dog
    )
    PetMateTheme {
        PetDetailsScreen(pet = samplePet)
    }
}
