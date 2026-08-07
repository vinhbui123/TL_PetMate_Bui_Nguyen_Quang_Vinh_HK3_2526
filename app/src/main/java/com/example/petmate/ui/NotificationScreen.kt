package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.ui.theme.PrimaryPeach
import com.example.petmate.util.AppNotification
import com.example.petmate.util.NotificationStorage
import com.example.petmate.util.TimeHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = remember { NotificationStorage(context) }
    var notifications by remember { mutableStateOf(storage.getNotifications()) }

    LaunchedEffect(Unit) {
        // Mark all as read when opening screen
        storage.markAllAsRead()
        notifications = storage.getNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = {
                            storage.clearAll()
                            notifications = emptyList()
                        }) {
                            Text("Xóa tất cả", color = PrimaryPeach)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có thông báo nào", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: AppNotification) {
    val bgColor = if (notification.isRead) Color.White else Color(0xFFFFF0ED)
    var isHandled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                shape = CircleShape,
                color = PrimaryPeach.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryPeach)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = notification.title,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TimeHelper.getRelativeTime(notification.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (notification.type == "org_invite" && !isHandled && notification.data != null) {
                    val orgId = notification.data["orgId"]?.toLongOrNull()
                    val memberId = notification.data["memberId"]?.toLongOrNull()
                    if (orgId != null && memberId != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val response = com.example.petmate.network.NetworkClient.orgApi.acceptInvitation(orgId, memberId)
                                            if (response.isSuccessful) {
                                                isHandled = true
                                                android.widget.Toast.makeText(context, "Đã chấp nhận", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Lỗi kết nối", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("Chấp nhận", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val response = com.example.petmate.network.NetworkClient.orgApi.rejectInvitation(orgId, memberId)
                                            if (response.isSuccessful) {
                                                isHandled = true
                                                android.widget.Toast.makeText(context, "Đã từ chối", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Lỗi kết nối", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {}
                                    }
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("Từ chối", fontSize = 12.sp, color = Color.Gray) }
                        }
                    }
                }
            }
        }
    }
}
