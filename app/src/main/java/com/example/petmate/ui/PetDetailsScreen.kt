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
import androidx.compose.ui.text.style.TextAlign
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
import com.example.petmate.ui.components.MarketItemCard

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailsScreen(
    initialPet: Pet,
    onBackClick: () -> Unit,
    onViewSellerProfile: (com.example.petmate.model.User, Boolean) -> Unit = { _, _ -> },
    onChatClick: (com.example.petmate.model.User) -> Unit = {},
    onAdoptClick: () -> Unit = {},
    onEditClick: (Pet) -> Unit = {},
    onPetClick: (Pet) -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    currentUserId: Long? = null,
    currentUserRole: String? = null
) {
    var pet by remember(initialPet.id) { mutableStateOf(initialPet) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPetReportDialog by remember { mutableStateOf(false) }
    var showCancelAdoptionDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var currentPetStatus by remember(initialPet.id) { mutableStateOf(initialPet.status ?: "AVAILABLE") }
    
    var adoptionStatus by remember(initialPet.id) { mutableStateOf<String?>(null) }
    var adoptionId by remember(initialPet.id) { mutableStateOf<Long?>(null) }
    var isCheckingAdoption by remember(initialPet.id) { mutableStateOf(true) }

    var isSaved by remember(initialPet.id) { mutableStateOf(false) }
    var isLiked by remember(initialPet.id) { mutableStateOf(false) }
    var likeCount by remember(initialPet.id) { mutableIntStateOf(initialPet.likeCount) }

    var ratingSummary by remember(initialPet.id) { mutableStateOf<com.example.petmate.model.SellerRatingSummary?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    
    var sellerOrgProfile by remember(initialPet.id) { mutableStateOf<com.example.petmate.model.OrganizationProfileDto?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(pet.organization, pet.user?.id) {
        if (pet.organization != null) {
            sellerOrgProfile = pet.organization
        } else {
            sellerOrgProfile = null
        }
    }

    if (showRatingDialog && pet.user?.id != null) {
        com.example.petmate.ui.components.RatingDialog(
            initialScore = ratingSummary?.currentUserRating?.score ?: 5.0,
            initialComment = ratingSummary?.currentUserRating?.comment ?: "",
            onDismissRequest = { showRatingDialog = false },
            onSubmit = { score, comment ->
                showRatingDialog = false
                coroutineScope.launch {
                    try {
                        val request = com.example.petmate.model.RatingRequest(score, pet.id.toLong(), comment)
                        apiService.rateUser(pet.user!!.id!!, request)
                        Toast.makeText(context, "Đánh giá thành công!", Toast.LENGTH_SHORT).show()
                        ratingSummary = apiService.getSellerRatingSummary(pet.user!!.id!!)
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 403) {
                            Toast.makeText(context, "Bạn chỉ có thể đánh giá sau khi nhận nuôi thú cưng thành công", Toast.LENGTH_LONG).show()
                        } else {
                            val errorBody = e.response()?.errorBody()?.string()
                            val errorMessage = try {
                                org.json.JSONObject(errorBody ?: "").getString("message")
                            } catch (ex: Exception) {
                                "Có lỗi xảy ra khi đánh giá"
                            }
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    LaunchedEffect(initialPet.id, currentUserId) {
        // Fetch latest pet details
        try {
            val updatedPet = apiService.getPetById(initialPet.id)
            pet = updatedPet
            currentPetStatus = updatedPet.status ?: "AVAILABLE"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (pet.user?.id != null) {
            try {
                ratingSummary = apiService.getSellerRatingSummary(pet.user!!.id!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fetch Save Status
        if (currentUserId != null) {
            try {
                val saveStatus = apiService.getSaveStatus(pet.id.toLong())
                isSaved = saveStatus.isSaved
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val likeStatus = apiService.getLikeStatus(pet.id.toLong())
                isLiked = likeStatus.liked
                likeCount = likeStatus.likeCount
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Không có ảnh",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(100.dp)
                        )
                    }
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

    if (showPetReportDialog) {
        com.example.petmate.ui.components.ReportDialog(
            reportedPetId = pet.id.toLong(),
            onDismissRequest = { showPetReportDialog = false },
            onSuccess = {
                Toast.makeText(context, "Cảm ơn bạn đã báo cáo bài đăng. Chúng tôi sẽ xem xét sớm nhất!", android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showCancelAdoptionDialog && adoptionId != null) {
        AlertDialog(
            onDismissRequest = { showCancelAdoptionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Hủy yêu cầu nhận nuôi",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = DeepBrown,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn rút lại đơn xin nhận nuôi bé thú cưng này không? Hành động này không thể hoàn tác.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth()
                )
            },
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
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Hủy Đơn", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelAdoptionDialog = false }
                ) {
                    Text("Quay lại", color = TextGray, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            val petUser = pet.user
            val isOwner = currentUserId != null && petUser != null && currentUserId == petUser.id
            if (!isOwner) {
                val isFree = pet.price.isNullOrBlank() || pet.price == "Miễn phí"
                BottomActionBar(
                    isFree = isFree,
                    adoptionStatus = adoptionStatus,
                    onAdoptClick = onAdoptClick,
                    onCancelAdoptionClick = { showCancelAdoptionDialog = true },
                    onChatClick = {
                        if (petUser != null) {
                            val user = com.example.petmate.model.User(
                                id = petUser.id ?: 0L,
                                fullName = petUser.fullName ?: "",
                                email = petUser.email ?: "",
                                avatarUrl = petUser.avatarUrl,
                                role = petUser.role ?: "MEMBER",
                                phone = petUser.phone,
                                address = petUser.address,
                                latitude = petUser.latitude,
                                longitude = petUser.longitude,
                                trustScore = petUser.trustScore
                            )
                            onChatClick(user)
                        }
                    }
                )
            }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showFullScreenImage = true },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Không có ảnh",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                
                // Status Overlay
                if (currentPetStatus == "SOLD") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ĐÃ GIAO DỊCH",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .background(Color.Red, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else if (currentPetStatus == "HIDDEN") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BÀI ĐĂNG ĐÃ ẨN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .background(Color.Gray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else if (currentPetStatus == "REJECTED") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Surface(
                                color = Color.Red,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "BỊ TỪ CHỐI",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            val note = pet.redListNote
                            if (!note.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val translatedNote = note
                                    .replace("EXACT", "CHÍNH XÁC")
                                    .replace("PARTIAL", "MỘT PHẦN")
                                
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Lý do: $translatedNote",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            if (currentUserRole == "ADMIN") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                com.example.petmate.network.NetworkClient.apiService.unlockRedListPet(pet.id)
                                                android.widget.Toast.makeText(context, "Mở khóa thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                                currentPetStatus = "AVAILABLE"
                                                pet = pet.copy(status = "AVAILABLE", redListNote = null)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Lỗi khi mở khóa!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("Mở khóa (Admin)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Gradient Overlay for top buttons visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )

                // Top Controls (Back, Share, Like, Bookmark, Report)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailActionIcon(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackClick
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailActionIcon(
                            icon = Icons.Default.Share,
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Hãy xem bé ${pet.name} (${pet.breed}) đang tìm chủ trên ứng dụng PetMate!\nLink: https://test-mobile-app-8c2ce.web.app/pet/${pet.id}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ bài đăng")
                                context.startActivity(shareIntent)
                            }
                        )
                        
                        DetailActionIcon(
                            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (isLiked) Color.Red else Color.White,
                            badgeText = if (likeCount > 0) likeCount.toString() else null,
                            onClick = {
                                if (currentUserId == null) {
                                    Toast.makeText(context, "Vui lòng đăng nhập để thích bài viết", Toast.LENGTH_SHORT).show()
                                    return@DetailActionIcon
                                }
                                coroutineScope.launch {
                                    try {
                                        val newStatus = apiService.toggleLike(pet.id.toLong())
                                        isLiked = newStatus.liked
                                        likeCount = newStatus.likeCount
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Có lỗi xảy ra khi thả tim", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        DetailActionIcon(
                            icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            tint = if (isSaved) PrimaryPeach else Color.White,
                            onClick = {
                                if (currentUserId == null) {
                                    Toast.makeText(context, "Vui lòng đăng nhập để lưu tin", Toast.LENGTH_SHORT).show()
                                    return@DetailActionIcon
                                }
                                coroutineScope.launch {
                                    try {
                                        val newStatus = apiService.toggleSave(pet.id.toLong())
                                        isSaved = newStatus.isSaved
                                        if (isSaved) {
                                            Toast.makeText(context, "Đã lưu tin đăng", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Đã bỏ lưu tin", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Có lỗi xảy ra khi lưu tin", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        DetailActionIcon(
                            icon = Icons.Default.Warning,
                            onClick = { showPetReportDialog = true }
                        )
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
                    text = pet.name ?: "Chưa có tên",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val priceText = remember(pet.price) {
                    val p = pet.price
                    if (p.isNullOrBlank() || p == "Miễn phí" || p == "0" || p == "0.0") {
                        "Miễn phí"
                    } else {
                        try {
                            val amount = p.replace(Regex("[^0-9]"), "").toLong()
                            val formatter = java.text.DecimalFormat("#,###")
                            formatter.format(amount).replace(",", ".") + " đ"
                        } catch (e: Exception) {
                            p
                        }
                    }
                }
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (priceText == "Miễn phí") SuccessGreen else Color(0xFFE53935)
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
            
            // Seller Profile Section (Cho Tot Style)
            val baseSeller = pet.user
            val seller = if (sellerOrgProfile != null && baseSeller != null) {
                baseSeller.copy(
                    fullName = sellerOrgProfile?.name ?: baseSeller.fullName,
                    avatarUrl = sellerOrgProfile?.logoUrl ?: baseSeller.avatarUrl,
                    address = sellerOrgProfile?.address ?: baseSeller.address
                )
            } else {
                baseSeller
            }
            com.example.petmate.ui.components.SellerInfoCard(
                seller = seller,
                ratingSummary = ratingSummary,
                currentUserId = currentUserId,
                onViewProfile = {
                    seller?.let { s ->
                        val user = com.example.petmate.model.User(
                            id = s.id ?: 0L,
                            fullName = s.fullName ?: "",
                            email = s.email ?: "",
                            avatarUrl = s.avatarUrl,
                            role = s.role ?: "MEMBER",
                            phone = s.phone,
                            address = s.address,
                            latitude = s.latitude,
                            longitude = s.longitude,
                            trustScore = s.trustScore
                        )
                        onViewSellerProfile(user, sellerOrgProfile != null)
                    }
                },
                onWriteReview = { showRatingDialog = true }
            )

            // Management Actions Section (Wrapped in Column for spacing/bg)
            if (currentUserId != null && seller != null && currentUserId == seller.id) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEditClick(pet) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF424242)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sửa tin", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        OutlinedButton(
                            onClick = { showStatusDialog = true },
                            modifier = Modifier.weight(1.2f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PrimaryPeach.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPeach),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đổi trạng thái", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(0.8f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFEBEE)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xoá", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Other pets by this seller
            var sellerPets by remember(initialPet.id) { mutableStateOf<List<Pet>>(emptyList()) }
            var isLoadingPets by remember(initialPet.id) { mutableStateOf(false) }
            val sellerId = pet.user?.id
            LaunchedEffect(sellerId, initialPet.id) {
                if (sellerId != null) {
                    isLoadingPets = true
                    try {
                        val allPets = apiService.getPetsByUser(sellerId)
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
                        text = if (pet.user?.role == "RESCUE_ORG") "Tin đăng khác của Tổ chức" else "Tin đăng khác của người bán",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sellerPets) { otherPet ->
                            Box(modifier = Modifier.width(160.dp)) {
                                MarketItemCard(
                                    item = otherPet,
                                    onClick = onPetClick,
                                    userLatitude = userLatitude,
                                    userLongitude = userLongitude
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Change Dialog
            if (showStatusDialog) {
                var selectedStatus by remember { mutableStateOf(currentPetStatus) }
                var isUpdatingStatus by remember { mutableStateOf(false) }
                
                AlertDialog(
                    onDismissRequest = { if (!isUpdatingStatus) showStatusDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White,
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryPeach, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cập nhật trạng thái", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    text = {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            val options = listOf(
                                "AVAILABLE" to "Đang hiển thị" to "Mọi người có thể tìm thấy tin của bạn",
                                "SOLD" to "Đã giao dịch" to "Đánh dấu bé đã tìm được chủ mới",
                                "HIDDEN" to "Ẩn tin" to "Tạm thời không hiển thị với mọi người"
                            )
                            options.forEach { (data, subtitle) ->
                                val (code, label) = data
                                Surface(
                                    onClick = { selectedStatus = code },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedStatus == code) PrimaryPeach.copy(alpha = 0.08f) else Color.Transparent,
                                    border = if (selectedStatus == code) BorderStroke(1.dp, PrimaryPeach.copy(alpha = 0.5f)) else null,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedStatus == code,
                                            onClick = { selectedStatus = code },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryPeach)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selectedStatus == code) PrimaryPeach else Color.Black)
                                            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isUpdatingStatus = true
                                coroutineScope.launch {
                                    try {
                                        apiService.updatePetStatus(pet.id, selectedStatus)
                                        currentPetStatus = selectedStatus
                                        Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                        showStatusDialog = false
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isUpdatingStatus = false
                                    }
                                }
                            },
                            enabled = !isUpdatingStatus,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
                        ) {
                            if (isUpdatingStatus) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Xác nhận", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showStatusDialog = false },
                            enabled = !isUpdatingStatus
                        ) {
                            Text("Hủy", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }

            // Delete Confirmation Dialog
            if (showDeleteDialog) {
                var isDeletingThisPet by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { if (!isDeletingThisPet) showDeleteDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                        }
                    },
                    title = { 
                        Text("Xoá bài đăng này?", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    },
                    text = { 
                        Text(
                            "Hành động này sẽ xoá vĩnh viễn tin đăng của bé và không thể khôi phục lại. Bạn có chắc chắn không?",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isDeletingThisPet = true
                                coroutineScope.launch {
                                    try {
                                        apiService.deletePet(pet.id)
                                        Toast.makeText(context, "Đã xoá bài đăng", Toast.LENGTH_SHORT).show()
                                        showDeleteDialog = false
                                        onBackClick()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isDeletingThisPet = false
                                    }
                                }
                            },
                            enabled = !isDeletingThisPet,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isDeletingThisPet) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Xoá ngay", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            enabled = !isDeletingThisPet,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Quay lại", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }
            
            // Characteristics (Icon-based list)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text("Thông tin chi tiết", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SpecRow(Icons.Default.Pets, "Giống", pet.breed)
                        SpecRow(Icons.Default.Cake, "Độ tuổi (tháng)", pet.age)
                        SpecRow(Icons.Default.Vaccines, "Tiêm phòng", if (pet.isVaccinated) "Đã tiêm" else "Chưa tiêm")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displaySex = when (pet.sex?.lowercase()) {
                            "male" -> "Đực"
                            "female" -> "Cái"
                            else -> pet.sex ?: "Chưa rõ"
                        }
                        SpecRow(Icons.Default.Transgender, "Giới tính", displaySex)
                        SpecRow(Icons.Default.MonitorWeight, "Trọng lượng (kg)", pet.weight)
                        SpecRow(Icons.Default.ContentCut, "Triệt sản", if (pet.isNeutered) "Đã triệt sản" else "Chưa")
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
                    text = pet.about ?: "Chưa có thông tin",
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
                Icon(Icons.Default.Shield, contentDescription = "Bảo mật", tint = Color(0xFFFBC02D))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Giao dịch an toàn: Tuyệt đối không chuyển tiền cọc trước khi gặp mặt và kiểm tra thú cưng.",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetailActionIcon(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = Color.White,
    badgeText: String? = null
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        modifier = if (badgeText != null) Modifier.height(36.dp) else Modifier.size(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (badgeText != null) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SpecRow(icon: ImageVector, label: String, value: String?) {
    Row(
        modifier = Modifier.padding(bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF5F5F5),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Text(
                text = value ?: "Chưa rõ", 
                fontSize = 14.sp, 
                color = Color.Black, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            if (isFree) {
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
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Trò chuyện", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trò chuyện", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
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
                            Icon(Icons.Default.Pets, contentDescription = "Nhận nuôi", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nhận nuôi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                // Chat Button for paid pets - Trò chuyện trực tiếp với người bán
                Button(
                    onClick = onChatClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE6D7CB), // Beige color like in image
                        contentColor = Color(0xFF5D4037)  // Deep brown for text/icon
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Trò chuyện", modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Trò chuyện trực tiếp với người bán", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        age = "36 tháng",
        weight = "2.5 kg",
        sex = "Cái",
        about = "Bé mèo Nga lai ta, màu trắng xám, được 1 tuổi rưỡi, cân nặng khoảng 2.5kg. Bé ăn được hạt và pate, đi vệ sinh đúng chỗ trong thau cát. Bé rất ngoan, quấn chủ, không cào cắn đồ đạc. Do chuyển trọ không cho nuôi chó mèo nên mình cần tìm chủ mới yêu thương bé.",
        imageUrl = null,
        price = "Miễn phí",
        imageRes = R.drawable.beagle_dog
    )
    PetMateTheme {
        PetDetailsScreen(initialPet = samplePet, onBackClick = {})
    }
}
