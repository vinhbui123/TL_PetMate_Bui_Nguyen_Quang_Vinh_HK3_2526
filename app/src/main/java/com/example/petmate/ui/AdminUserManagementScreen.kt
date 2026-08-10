package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
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
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadUsers = {
        coroutineScope.launch {
            isLoading = true
            try {
                users = NetworkClient.apiService.getAllUsersAdmin()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Lỗi khi tải danh sách người dùng: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Người dùng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                    Button(onClick = { loadUsers() }) {
                        Text("Thử lại")
                    }
                }
            } else if (users.isEmpty()) {
                Text(
                    "Chưa có người dùng nào.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users) { user ->
                        AdminUserCard(
                            user = user,
                            onStatusChange = { newStatus ->
                                coroutineScope.launch {
                                    try {
                                        NetworkClient.apiService.updateUserStatusAdmin(user.id, newStatus)
                                        loadUsers()
                                        Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRoleChange = { newRole ->
                                coroutineScope.launch {
                                    try {
                                        NetworkClient.apiService.updateUserRoleAdmin(user.id, newRole)
                                        loadUsers()
                                        Toast.makeText(context, "Cập nhật quyền thành công!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    onStatusChange: (String) -> Unit,
    onRoleChange: (String) -> Unit
) {
    var showRoleDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = user.avatarUrl ?: "https://ui-avatars.com/api/?name=${user.fullName}",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.fullName ?: "Không tên", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = user.email ?: "Không có email", fontSize = 14.sp, color = Color.Gray)
                }
                
                // Trạng thái (BANNED, ACTIVE)
                val isBanned = user.status == "BANNED"
                val statusColor = if (isBanned) Color.Red else Color(0xFF4CAF50)
                val statusText = if (isBanned) "BANNED" else "ACTIVE"
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quyền: ", fontSize = 14.sp, color = Color.Gray)
                    val role = user.role.toString()
                    val roleIcon = when (role) {
                        "ADMIN" -> Icons.Default.ManageAccounts
                        "RESCUE_ORG" -> Icons.Default.Business
                        else -> Icons.Default.Person
                    }
                    val roleColor = when (role) {
                        "ADMIN" -> Color(0xFF9C27B0)
                        "RESCUE_ORG" -> Color(0xFF2196F3)
                        else -> Color.DarkGray
                    }
                    
                    Icon(roleIcon, contentDescription = null, tint = roleColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = role,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = roleColor
                    )
                }
                
                // Action Buttons
                Row {
                    // Đổi Role
                    IconButton(onClick = { showRoleDialog = true }) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Đổi Quyền", tint = Color(0xFF2196F3))
                    }
                    
                    // Ban / Unban
                    if (user.status == "BANNED") {
                        IconButton(onClick = { onStatusChange("ACTIVE") }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Unban", tint = Color(0xFF4CAF50))
                        }
                    } else {
                        IconButton(onClick = { onStatusChange("BANNED") }) {
                            Icon(Icons.Default.Block, contentDescription = "Ban", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Đổi quyền người dùng") },
            text = {
                Column {
                    Text("Chọn quyền mới cho ${user.fullName}:")
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("MEMBER", "RESCUE_ORG", "ADMIN").forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (user.role.toString() == role),
                                onClick = {
                                    showRoleDialog = false
                                    if (user.role.toString() != role) {
                                        onRoleChange(role)
                                    }
                                }
                            )
                            Text(text = role)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
