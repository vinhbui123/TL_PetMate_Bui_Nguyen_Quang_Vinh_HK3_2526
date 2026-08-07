package com.example.petmate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.petmate.model.SellerRatingSummary
import com.example.petmate.ui.components.ReportDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.R
import com.example.petmate.model.Pet
import com.example.petmate.model.PetUser
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.components.MarketItemCard
import com.example.petmate.ui.components.VerifiedBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    sellerId: Long,
    sellerInfo: PetUser?, // Passed from the PetDetailsScreen to avoid fetching if possible
    onBack: () -> Unit,
    onPetClick: (Pet) -> Unit,
    currentUserId: Long? = null,
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    isOrgProfile: Boolean = false,
    blockedUserIds: List<Long> = emptyList(),
    onBlockStatusChanged: () -> Unit = {},
    onViewFollowers: (Long, Int) -> Unit = { _, _ -> }
) {
    val isSelf = currentUserId != null && currentUserId == sellerId
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var ratingSummary by remember { mutableStateOf<SellerRatingSummary?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isBlocking by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var followersCount by remember { mutableStateOf(0L) }
    var followingCount by remember { mutableStateOf(0L) }
    var isFollowLoading by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(blockedUserIds.contains(sellerId)) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showReportDialog) {
        ReportDialog(
            reportedUserId = sellerId,
            onDismissRequest = { showReportDialog = false },
            onSuccess = {
                android.widget.Toast.makeText(context, "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét sớm nhất!", android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }

    LaunchedEffect(sellerId) {
        isLoading = true
        try {
            // Fetch follow stats
            val stats = NetworkClient.apiService.getUserFollowStats(sellerId)
            followersCount = stats["followers"] ?: 0L
            followingCount = stats["following"] ?: 0L
            
            // Check if current user is following this seller
            isFollowing = NetworkClient.apiService.checkFollowStatus(sellerId)
            
            // Fetch seller's pets and filter based on profile type
            val allPets = NetworkClient.apiService.getPetsByUser(sellerId)
            pets = if (isOrgProfile) {
                allPets.filter { it.organization != null }
            } else {
                allPets.filter { it.organization == null }
            }
            try {
                ratingSummary = NetworkClient.apiService.getSellerRatingSummary(sellerId)
            } catch (e: Exception) {
                // Ignore
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = "Không thể tải thông tin người dùng"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang cá nhân") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    // Chỉ hiện nút Chặn và Báo cáo nếu KHÔNG phải đang xem chính mình
                    if (!isSelf) {
                        IconButton(
                            onClick = {
                                if (!isBlocking) {
                                    isBlocking = true
                                    coroutineScope.launch {
                                        try {
                                            if (isBlocked) {
                                                NetworkClient.apiService.unblockUser(sellerId)
                                                android.widget.Toast.makeText(context, "Đã bỏ chặn người dùng này", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                NetworkClient.apiService.blockUser(sellerId)
                                                android.widget.Toast.makeText(context, "Đã chặn người dùng này", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            onBlockStatusChanged()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Lỗi khi thực hiện thao tác", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isBlocking = false
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isBlocking) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Red, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Block, contentDescription = "Chặn", tint = if (isBlocked) Color.Red else Color.Gray)
                            }
                        }
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Warning, contentDescription = "Báo cáo", tint = Color.Gray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Seller Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Avatar and Name/Followers
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        ) {
                            if (!sellerInfo?.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = sellerInfo?.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.app_logo),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sellerInfo?.fullName ?: "Người dùng ẩn danh",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (sellerInfo?.identityVerified == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                VerifiedBadge(size = 20.dp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$followersCount người theo dõi",
                                    fontSize = 14.sp,
                                    color = com.example.petmate.ui.theme.PrimaryPeach,
                                    modifier = Modifier.clickable { onViewFollowers(sellerId, 0) }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "$followingCount đang theo dõi",
                                    fontSize = 14.sp,
                                    color = com.example.petmate.ui.theme.PrimaryPeach,
                                    modifier = Modifier.clickable { onViewFollowers(sellerId, 1) }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Status and Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sellerInfo?.status ?: "Đang hoạt động",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                    
                    if (!sellerInfo?.address.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Vị trí",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sellerInfo?.address ?: "",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Follow Button - Chỉ hiện nếu KHÔNG phải chính mình
                        if (!isSelf) {
                            Button(
                                onClick = {
                                    if (isFollowLoading) return@Button
                                    isFollowLoading = true
                                    coroutineScope.launch {
                                        try {
                                            if (isFollowing) {
                                                NetworkClient.apiService.unfollowUser(sellerId)
                                                isFollowing = false
                                                followersCount -= 1
                                            } else {
                                                NetworkClient.apiService.followUser(sellerId)
                                                isFollowing = true
                                                followersCount += 1
                                            }
                                        } catch (e: retrofit2.HttpException) {
                                            val errorBody = e.response()?.errorBody()?.string()
                                            val errorMessage = try {
                                                org.json.JSONObject(errorBody ?: "").getString("message")
                                            } catch (ex: Exception) {
                                                "Lỗi khi thao tác"
                                            }
                                            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Lỗi khi thao tác", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isFollowLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFFF0F0F0) else com.example.petmate.ui.theme.PrimaryPeach,
                                    contentColor = if (isFollowing) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                if (isFollowLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = if (isFollowing) "Đang theo dõi" else "Theo dõi", 
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                        
                        // Call Button
                        if (!sellerInfo?.phone.isNullOrEmpty()) {
                            Button(
                                onClick = { 
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                                    intent.data = android.net.Uri.parse("tel:${sellerInfo?.phone}")
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Gọi điện",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gọi điện", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = com.example.petmate.ui.theme.PrimaryPeach,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Cửa hàng", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Đánh giá", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTabIndex == 0) {
                Text(
                    text = "Tin đang đăng (${pets.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = com.example.petmate.ui.theme.PrimaryPeach)
                    }
                } else if (pets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Người dùng này chưa có bài đăng nào.", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pets) { pet ->
                            MarketItemCard(
                                item = pet,
                                onClick = onPetClick,
                                userLatitude = userLatitude,
                                userLongitude = userLongitude
                            )
                        }
                    }
                }
            } else {
                // Review Tab
                val reviews = ratingSummary?.recentReviews ?: emptyList()
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = com.example.petmate.ui.theme.PrimaryPeach)
                    }
                } else if (reviews.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có đánh giá nào.", color = Color.Gray)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reviews.size) { index ->
                            val review = reviews[index]
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEEEEEE))
                                    ) {
                                        if (!review.raterAvatarUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = review.raterAvatarUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(review.raterName ?: "Người dùng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            repeat(review.score.toInt()) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(com.example.petmate.util.TimeHelper.getRelativeTime(review.createdAt), fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                
                                if (!review.comment.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(review.comment, fontSize = 14.sp, color = Color(0xFF212121))
                                }

                                if (!review.petName.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!review.petImageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = review.petImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Column {
                                            Text(review.petName, fontSize = 13.sp, color = Color(0xFF424242), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (!review.petPrice.isNullOrEmpty()) {
                                                Text(review.petPrice, fontSize = 13.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            if (index < reviews.size - 1) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                            }
                        }
                    }
                }
            }
        }
    }
}
