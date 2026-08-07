package com.example.petmate.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onViewFollowers: (Long, Int) -> Unit = { _, _ -> },
    onManageAdoptions: () -> Unit = {},
    onViewSavedPets: () -> Unit = {},
    onOrgRegistrationClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {}
) {
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var followersCount by remember { mutableStateOf(0L) }
    var followingCount by remember { mutableStateOf(0L) }
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val fetchedUser = NetworkClient.apiService.getProfile()
            user = fetchedUser
            fullName = fetchedUser.fullName
            phone = fetchedUser.phone ?: ""
            address = fetchedUser.address ?: ""
            cccd = fetchedUser.cccd ?: ""
            avatarUrl = fetchedUser.avatarUrl
            try {
                val stats = NetworkClient.apiService.getUserFollowStats(fetchedUser.id)
                followersCount = stats["followers"] ?: 0L
                followingCount = stats["following"] ?: 0L
            } catch (e: Exception) { e.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
            snackbarHostState.showSnackbar("Không thể tải thông tin hồ sơ")
        } finally {
            isLoading = false
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            isUploadingAvatar = true
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "avatar_upload.jpg")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                    
                    val updatedUser = NetworkClient.apiService.uploadAvatar(body)
                    avatarUrl = updatedUser.avatarUrl
                    user = updatedUser
                    Toast.makeText(context, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ cá nhân", fontWeight = FontWeight.Bold, color = TextGray) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = BackgroundBeige,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPeach)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Avatar UI
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(SoftPeach)
                        .clickable(enabled = !isUploadingAvatar) {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null || !avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = selectedImageUri ?: avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Default Avatar",
                            modifier = Modifier.size(70.dp),
                            tint = IconGray
                        )
                    }
                    
                    // Camera Overlay Icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Followers/Following
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(count = followersCount, label = "Người theo dõi") {
                        user?.id?.let { onViewFollowers(it, 0) }
                    }
                    VerticalDivider(
                        modifier = Modifier.height(30.dp).padding(horizontal = 32.dp),
                        color = InputBorder
                    )
                    StatItem(count = followingCount, label = "Đang theo dõi") {
                        user?.id?.let { onViewFollowers(it, 1) }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileTextField(value = fullName, onValueChange = { fullName = it }, label = "Họ và tên")
                    ProfileTextField(value = phone, onValueChange = { phone = it }, label = "Số điện thoại", keyboardType = KeyboardType.Phone)
                    ProfileTextField(value = cccd, onValueChange = { cccd = it }, label = "Số CCCD", keyboardType = KeyboardType.Number)
                    ProfileTextField(value = address, onValueChange = { address = it }, label = "Địa chỉ")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Primary Action Button
                Button(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val updatedUser = User(
                                    id = user?.id ?: 0,
                                    email = user?.email ?: "",
                                    fullName = fullName,
                                    role = user?.role ?: "MEMBER",
                                    phone = phone,
                                    address = address,
                                    cccd = cccd
                                )
                                NetworkClient.apiService.updateProfile(updatedUser)
                                Toast.makeText(context, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show()
                                onSaveSuccess()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Lỗi khi lưu: ${e.localizedMessage}")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Lưu thay đổi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = InputBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                // Secondary Actions Group
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionRowButton(icon = Icons.Default.Bookmark, label = "Tin đã lưu", onClick = onViewSavedPets)
                    ActionRowButton(icon = Icons.Default.Lock, label = "Đổi mật khẩu", onClick = onChangePasswordClick)
                    
                    ActionRowButton(
                        icon = Icons.AutoMirrored.Filled.ExitToApp, 
                        label = "Đăng xuất", 
                        onClick = onLogoutClick,
                        contentColor = TextGray.copy(alpha = 0.7f)
                    )

                    ActionRowButton(
                        icon = Icons.Default.DeleteForever, 
                        label = "Xóa tài khoản", 
                        onClick = { showDeleteDialog = true },
                        contentColor = Color(0xFFE57373) // Soft red
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    
    if (showDeleteDialog) {
        Dialog(onDismissRequest = { if (!isDeleting) showDeleteDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Xác nhận xóa tài khoản",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Bạn có chắc chắn muốn xóa tài khoản không? Hành động này không thể hoàn tác.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                            enabled = !isDeleting
                        ) {
                            Text("Hủy")
                        }

                        Button(
                            onClick = {
                                isDeleting = true
                                coroutineScope.launch {
                                    try {
                                        NetworkClient.apiService.deleteAccount()
                                        Toast.makeText(context, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show()
                                        showDeleteDialog = false
                                        onLogoutClick()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Lỗi khi xóa tài khoản")
                                    } finally {
                                        isDeleting = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Xóa", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(count: Long, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextGray
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = IconGray
        )
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPeach,
            unfocusedBorderColor = InputBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
            focusedLabelColor = PrimaryPeach,
            cursorColor = PrimaryPeach
        )
    )
}

@Composable
fun ActionRowButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color = TextGray
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = IconGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
