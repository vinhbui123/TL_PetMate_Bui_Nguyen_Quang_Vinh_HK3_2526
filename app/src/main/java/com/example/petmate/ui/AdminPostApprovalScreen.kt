package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostApprovalScreen(onBack: () -> Unit) {
    var pendingPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var allPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chờ duyệt", "Tất cả")
    val coroutineScope = rememberCoroutineScope()

    var petToDelete by remember { mutableStateOf<Pet?>(null) }

    LaunchedEffect(Unit) {
        try {
            pendingPets = NetworkClient.apiService.getPendingPets()
                .filter { it.status != "REQUIRES_REVIEW" }
            allPets = NetworkClient.apiService.getAllPetsAdmin()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý bài đăng") },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryPeach
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) },
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
                    else -> {
                        val displayList = if (selectedTab == 0) pendingPets else allPets
                        if (displayList.isEmpty()) {
                            Text(
                                text = if (selectedTab == 0) "Không có tin đăng nào chờ duyệt." else "Không có bài đăng nào.",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(displayList) { pet ->
                                    if (selectedTab == 0) {
                                        PendingPetCard(
                                            pet = pet,
                                            onStatusChange = { newStatus ->
                                                coroutineScope.launch {
                                                    try {
                                                        NetworkClient.apiService.updatePetStatus(pet.id, newStatus)
                                                        pendingPets = pendingPets.filter { it.id != pet.id }
                                                        allPets = NetworkClient.apiService.getAllPetsAdmin()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        AdminPetCard(
                                            pet = pet,
                                            onStatusChange = { newStatus ->
                                                coroutineScope.launch {
                                                    try {
                                                        NetworkClient.apiService.updatePetStatus(pet.id, newStatus)
                                                        allPets = NetworkClient.apiService.getAllPetsAdmin()
                                                        pendingPets = NetworkClient.apiService.getPendingPets()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            },
                                            onDeleteClick = { petToDelete = pet }
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

    if (petToDelete != null) {
        AlertDialog(
            onDismissRequest = { petToDelete = null },
            title = { Text("Xóa bài đăng") },
            text = { Text("Bạn có chắc chắn muốn xóa bài đăng '${petToDelete?.name}' không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                NetworkClient.apiService.deletePet(petToDelete!!.id)
                                allPets = NetworkClient.apiService.getAllPetsAdmin()
                                pendingPets = NetworkClient.apiService.getPendingPets()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                petToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { petToDelete = null }) {
                    Text("Hủy", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun PendingPetCard(pet: Pet, onStatusChange: (String) -> Unit) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(pet.name ?: "Chưa có tên", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.user?.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.user?.avatarUrl,
                        contentDescription = "Seller",
                        modifier = Modifier.size(30.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.Gray))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(pet.user?.fullName ?: "Người dùng ẩn danh", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onStatusChange("AVAILABLE") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Duyệt")
                }
                OutlinedButton(
                    onClick = { onStatusChange("REJECTED") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Từ chối")
                }
            }
        }
    }
}

@Composable
fun AdminPetCard(pet: Pet, onStatusChange: (String) -> Unit, onDeleteClick: () -> Unit) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    
    val statusText = when (pet.status) {
        "PENDING" -> "Chờ duyệt"
        "AVAILABLE" -> "Đang hiển thị"
        "REJECTED" -> "Bị từ chối"
        "SOLD" -> "Đã giao dịch"
        "HIDDEN" -> "Đã ẩn"
        "REQUIRES_REVIEW" -> "Danh sách đỏ"
        else -> pet.status ?: "Không rõ"
    }

    val statusColor = when (pet.status) {
        "PENDING" -> Color(0xFFFFA000)
        "AVAILABLE" -> Color(0xFF4CAF50)
        "REJECTED" -> Color.Red
        "REQUIRES_REVIEW" -> Color(0xFF795548)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(pet.name ?: "Chưa có tên", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!pet.user?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = pet.user?.avatarUrl,
                            contentDescription = "Seller",
                            modifier = Modifier.size(30.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.Gray))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(pet.user?.fullName ?: "Người dùng ẩn danh", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
                Text(statusText, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pet.status != "REJECTED" && pet.status != "HIDDEN" && pet.status != "REQUIRES_REVIEW") {
                    OutlinedButton(
                        onClick = { onStatusChange("REJECTED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFA000)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Khóa bài")
                    }
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa bài")
                }
            }
        }
    }
}
