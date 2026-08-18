package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostApprovalScreen(onBack: () -> Unit, onPetClick: (Pet) -> Unit = {}) {
    var pendingPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var allPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chờ duyệt", "Tất cả bài đăng")
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var petToDelete by remember { mutableStateOf<Pet?>(null) }

    val reloadData = {
        coroutineScope.launch {
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
    }

    LaunchedEffect(Unit) {
        reloadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Quản lý bài đăng", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Quay lại",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF6366F1),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF6366F1)
                    )
                },
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (selectedTab == index) Color(0xFF6366F1) else Color(0xFF64748B)
                            ) 
                        }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center), 
                            color = Color(0xFF6366F1)
                        )
                    }
                    else -> {
                        val displayList = if (selectedTab == 0) pendingPets else allPets
                        if (displayList.isEmpty()) {
                            Text(
                                text = if (selectedTab == 0) "Không có bài đăng nào chờ duyệt." else "Không có bài đăng nào.",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(displayList, key = { it.id }) { pet ->
                                    if (selectedTab == 0) {
                                        PendingPetCard(
                                            pet = pet,
                                            onStatusChange = { newStatus ->
                                                coroutineScope.launch {
                                                    try {
                                                        NetworkClient.apiService.updatePetStatus(pet.id, newStatus)
                                                        Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                                                        reloadData()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onPetClick = { onPetClick(pet) }
                                        )
                                    } else {
                                        AdminPetCard(
                                            pet = pet,
                                            onStatusChange = { newStatus ->
                                                coroutineScope.launch {
                                                    try {
                                                        NetworkClient.apiService.updatePetStatus(pet.id, newStatus)
                                                        Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                                                        reloadData()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onDeleteClick = { petToDelete = pet },
                                            onUnlockClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        NetworkClient.apiService.unlockRedListPet(pet.id)
                                                        Toast.makeText(context, "Mở khóa thành công!", Toast.LENGTH_SHORT).show()
                                                        reloadData()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, "Lỗi khi mở khóa!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onPetClick = { onPetClick(pet) }
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
            title = { Text("Xóa bài đăng", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            text = { 
                Text(
                    "Bạn có chắc chắn muốn xóa bài đăng '${petToDelete?.name}' không? Hành động này không thể hoàn tác.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                NetworkClient.apiService.deletePet(petToDelete!!.id)
                                Toast.makeText(context, "Đã xóa bài đăng", Toast.LENGTH_SHORT).show()
                                reloadData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Lỗi khi xóa bài đăng!", Toast.LENGTH_SHORT).show()
                            } finally {
                                petToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { petToDelete = null }) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
fun PendingPetCard(pet: Pet, onStatusChange: (String) -> Unit, onPetClick: () -> Unit = {}) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val priceVal = pet.price?.toDoubleOrNull() ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPetClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = "Pet Image",
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = "Không có ảnh", tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pet.name ?: "Chưa có tên", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Giống: ${pet.breed ?: "Không rõ"}${if (pet.age != null) " - Tuổi: ${pet.age}" else ""}", 
                        fontSize = 13.sp, 
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (priceVal > 0) formatter.format(priceVal) else "Miễn phí (Nhận nuôi)",
                        fontWeight = FontWeight.Bold,
                        color = if (priceVal > 0) Color(0xFF6366F1) else Color(0xFF10B981),
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.user?.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.user?.avatarUrl,
                        contentDescription = "Seller",
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đăng bởi: ${pet.user?.fullName ?: "Ẩn danh"}", 
                    fontWeight = FontWeight.Medium, 
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onStatusChange("AVAILABLE") },
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Duyệt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { onStatusChange("REJECTED") },
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Từ chối", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AdminPetCard(
    pet: Pet, 
    onStatusChange: (String) -> Unit, 
    onDeleteClick: () -> Unit, 
    onUnlockClick: () -> Unit = {}, 
    onPetClick: () -> Unit = {}
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val priceVal = pet.price?.toDoubleOrNull() ?: 0.0

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
        "PENDING" -> Color(0xFFF59E0B)
        "AVAILABLE" -> Color(0xFF10B981)
        "REJECTED" -> Color(0xFFEF4444)
        "REQUIRES_REVIEW" -> Color(0xFF8B5CF6)
        else -> Color(0xFF64748B)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPetClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = "Pet Image",
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = "Không có ảnh", tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            pet.name ?: "Chưa có tên", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Giống: ${pet.breed ?: "Không rõ"}${if (pet.age != null) " - Tuổi: ${pet.age}" else ""}", 
                        fontSize = 13.sp, 
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (priceVal > 0) formatter.format(priceVal) else "Miễn phí (Nhận nuôi)",
                        fontWeight = FontWeight.Bold,
                        color = if (priceVal > 0) Color(0xFF6366F1) else Color(0xFF10B981),
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9), thickness = 1.dp)

            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!pet.user?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = pet.user?.avatarUrl,
                            contentDescription = "Seller",
                            modifier = Modifier.size(26.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pet.user?.fullName ?: "Ẩn danh", 
                        fontWeight = FontWeight.Medium, 
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pet.status == "REJECTED") {
                    Button(
                        onClick = onUnlockClick,
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mở khóa", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (pet.status != "HIDDEN" && pet.status != "REQUIRES_REVIEW") {
                    OutlinedButton(
                        onClick = { onStatusChange("REJECTED") },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Khóa bài", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xóa bài", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
