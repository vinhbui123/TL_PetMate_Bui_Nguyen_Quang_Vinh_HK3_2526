package com.example.petmate.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.petmate.R
import com.example.petmate.model.Pet
import com.example.petmate.network.NetworkClient.apiService
import com.example.petmate.ui.theme.*
import com.example.petmate.util.LocationHelper
import com.example.petmate.util.TimeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailsScreen(
    pet: Pet,
    onBackClick: () -> Unit,
    onViewSellerProfile: (com.example.petmate.model.User) -> Unit = {},
    onChatClick: (com.example.petmate.model.User) -> Unit = {},
    onAdoptClick: () -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    currentUserId: Long? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showCancelAdoptionDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
    var adoptionStatus by remember { mutableStateOf<String?>(null) }
    var adoptionId by remember { mutableStateOf<Long?>(null) }
    var isCheckingAdoption by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(pet.id, currentUserId) {
        if (currentUserId != null) {
            try {
                val myApps = apiService.getMyAdoptionApplications()
                val app = myApps.find { it.petId == pet.id.toLong() }
                if (app != null) {
                    adoptionStatus = app.status
                    adoptionId = app.id
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingAdoption = false
            }
        } else {
            isCheckingAdoption = false
        }
    }

    var showFullScreenImage by remember { mutableStateOf(false) }

    if (showFullScreenImage) {
       Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = pet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(pet.imageRes),
                        contentDescription = pet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Close Button
                IconButton(
                    onClick = { showFullScreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 32.dp, end = 16.dp) // padding for status bar
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                }
            }
        }
    }

    if (showReportDialog) {
        com.example.petmate.ui.components.ReportDialog(
            reportedPetId = pet.id.toLong(),
            onDismissRequest = { showReportDialog = false },
            onSuccess = {
                Toast.makeText(context, "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét sớm nhất!", android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showCancelAdoptionDialog && adoptionId != null) {
        AlertDialog(
            onDismissRequest = { showCancelAdoptionDialog = false },
            title = { Text("Hủy yêu cầu nhận nuôi") },
            text = { Text("Bạn có chắc chắn muốn rút lại đơn xin nhận nuôi bé thú cưng này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                apiService.cancelAdoptionApplication(adoptionId!!)
                                adoptionStatus = null
                                adoptionId = null
                                Toast.makeText(context, "Đã hủy đơn thành công!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                showCancelAdoptionDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hủy Đơn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAdoptionDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            val isFree = pet.price.isNullOrBlank() || pet.price == "Miễn phí"
            BottomActionBar(
                isFree = isFree,
                adoptionStatus = adoptionStatus,
                onAdoptClick = onAdoptClick,
                onCancelAdoptionClick = { showCancelAdoptionDialog = true },
                onChatClick = {
                    if (pet.user != null) {
                        val user = com.example.petmate.model.User(
                            id = pet.user.id ?: 0L,
                            fullName = pet.user.fullName ?: "",
                            email = pet.user.email ?: "",
                            avatarUrl = pet.user.avatarUrl,
                            role = pet.user.role ?: "MEMBER",
                            phone = pet.user.phone,
                            address = pet.user.address,
                            latitude = pet.user.latitude,
                            longitude = pet.user.longitude
                        )
                        onChatClick(user)
                    }
                }
            )
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
                    .clickable { showFullScreenImage = true }
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
                    Row {
                        IconButton(
                            onClick = { 
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Hãy xem bé ${pet.name} (${pet.breed}) đang tìm chủ trên ứng dụng PetMate!\nLink: https://petmate.vn/pet/${pet.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ bài đăng")
                                context.startActivity(shareIntent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { /* Favorite */ },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Lưu tin", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showReportDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Báo cáo", tint = Color.White)
                        }
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
                        Text(
                            text = LocationHelper.getDistanceText(
                                userLatitude, userLongitude, 
                                pet.latitude, 
                                pet.longitude
                            )?.let { "Cách $it" } ?: "Chưa rõ khoảng cách",
                            style = MaterialTheme.typography.bodyMedium, color = Color.Gray
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tin đăng ${TimeHelper.getRelativeTime(pet.createdAt).lowercase()}",
                            style = MaterialTheme.typography.bodyMedium, 
                            color = Color.Gray
                        )
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    if (!pet.user?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = pet.user.avatarUrl,
                            contentDescription = "Seller Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = "Seller Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pet.user?.fullName ?: "Người dùng ẩn danh",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pet.user?.address ?: "Chưa cập nhật địa chỉ",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                if (currentUserId != null && pet.user != null && currentUserId == pet.user.id) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Xoá bài", color = Color.Red)
                    }
                } else {
                    Button(
                        onClick = { 
                            if (pet.user != null) {
                                val user = com.example.petmate.model.User(
                                    id = pet.user.id ?: 0L,
                                    fullName = pet.user.fullName ?: "",
                                    email = pet.user.email ?: "",
                                    avatarUrl = pet.user.avatarUrl,
                                    role = pet.user.role ?: "MEMBER",
                                    phone = pet.user.phone,
                                    address = pet.user.address,
                                    latitude = pet.user.latitude,
                                    longitude = pet.user.longitude
                                )
                                onViewSellerProfile(user)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPeach.copy(alpha = 0.1f),
                            contentColor = PrimaryPeach
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        elevation = null // Flat look
                    ) {
                        Text("Xem trang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Other pets by this seller
            var sellerPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
            var isLoadingPets by remember { mutableStateOf(false) }
            LaunchedEffect(pet.user?.id) {
                if (pet.user?.id != null) {
                    isLoadingPets = true
                    try {
                        val allPets = apiService.getPetsByUser(pet.user.id)
                        sellerPets = allPets.filter { it.id != pet.id } // exclude current pet
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoadingPets = false
                    }
                }
            }

            if (isLoadingPets) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryPeach)
                }
            } else if (sellerPets.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Tin đăng khác của người bán",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sellerPets) { otherPet ->
                            Surface(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                                    .clickable { /* Tạm thời chỉ xem, hoặc bạn có thể gọi một hàm onPetClick mới */ },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(Color.LightGray)
                                    ) {
                                        if (!otherPet.imageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = otherPet.imageUrl,
                                                contentDescription = otherPet.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(otherPet.imageRes),
                                                contentDescription = otherPet.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = otherPet.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 14.sp,
                                            modifier = Modifier.height(30.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = otherPet.price ?: "Miễn phí",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delete Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                    title = { Text("Xác nhận xoá") },
                    text = { Text("Bạn có chắc chắn muốn xoá bài đăng này không? Hành động này không thể hoàn tác.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDeleting = true
                                coroutineScope.launch {
                                    try {
                                        apiService.deletePet(pet.id)
                                        Toast.makeText(context, "Đã xoá bài đăng", Toast.LENGTH_SHORT).show()
                                        showDeleteDialog = false
                                        onBackClick() // Go back after delete
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isDeleting = false
                                    }
                                }
                            },
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Xoá", color = Color.Red)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            enabled = !isDeleting
                        ) {
                            Text("Hủy", color = Color.Gray)
                        }
                    }
                )
            }
            
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
                    text = pet.about,
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
fun BottomActionBar(
    isFree: Boolean, 
    adoptionStatus: String?, 
    onAdoptClick: () -> Unit, 
    onCancelAdoptionClick: () -> Unit,
    onChatClick: () -> Unit
) {
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
            Button(
                onClick = onChatClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryPeach
                ),
                border = BorderStroke(1.dp, PrimaryPeach)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            if (isFree) {
                when (adoptionStatus) {
                    "PENDING" -> {
                        Button(
                            onClick = onCancelAdoptionClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Đã gửi", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đã gửi đơn", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    "APPROVED" -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFF4CAF50),
                                disabledContentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Đã duyệt", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đã duyệt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    "REJECTED" -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color.Red.copy(alpha = 0.7f),
                                disabledContentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Từ chối", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bị từ chối", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    else -> {
                        // Adopt Button
                        Button(
                            onClick = onAdoptClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPeach,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Nhận nuôi", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nhận nuôi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                // Call Button
                Button(
                    onClick = { /* Call logic */ },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), // Green for calling
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Gọi ngay", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gọi ngay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PetDetailsScreenPreview() {
    val samplePet = Pet(
        id = 1,
        name = "Bé Mèo Nga lai Ta rất ngoan và dể nuôi",
        breed = "Mèo Nga lai",
        age = "1.5 năm",
        weight = "2.5 kg",
        sex = "Cái",
        about = "Bé mèo Nga lai ta, màu trắng xám, được 1 tuổi rưỡi, cân nặng khoảng 2.5kg. Bé ăn được hạt và pate, đi vệ sinh đúng chỗ trong thau cát. Bé rất ngoan, quấn chủ, không cào cắn đồ đạc. Do chuyển trọ không cho nuôi chó mèo nên mình cần tìm chủ mới yêu thương bé.",
        imageUrl = null,
        price = "Miễn phí",
        imageRes = R.drawable.beagle_dog
    )
    PetMateTheme {
        PetDetailsScreen(pet = samplePet, onBackClick = {})
    }
}
