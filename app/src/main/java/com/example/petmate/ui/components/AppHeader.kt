package com.example.petmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.R
import com.example.petmate.model.User
import com.example.petmate.ui.theme.*
import com.example.petmate.util.NotificationStorage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AppHeader(
    currentUser: User? = null,
    onLogoutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminDashboardClick: () -> Unit = {},
    onBlockedUsersClick: () -> Unit = {},
    onPostHistoryClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var unreadCount by remember { mutableStateOf(0) }

    // Polling cập nhật số thông báo chưa đọc mỗi 3 giây
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val storage = NotificationStorage(context)
            while (true) {
                unreadCount = storage.getUnreadCount()
                delay(3000.milliseconds)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (currentUser == null) {
                Modifier.clickable { onLogoutClick() }.weight(1f)
            } else {
                Modifier.weight(1f)
            }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            ) {
                if (!currentUser?.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = currentUser?.avatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser?.fullName ?: "Khách vãng lai",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    maxLines = 1
                )
                if (currentUser == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = PrimaryPeach,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đăng nhập ngay",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryPeach,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryPeach,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentUser.address ?: "Chưa cập nhật địa chỉ",
                            style = MaterialTheme.typography.bodySmall,
                            color = IconGray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        if (currentUser != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clickable { onNotificationsClick() }.padding(end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextGray,
                        modifier = Modifier.size(28.dp)
                    )
                    if (unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Red,
                            modifier = Modifier.size(16.dp).offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Box {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextGray,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { expanded = true }
                    )
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.White)) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White).width(180.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = null, 
                                        tint = TextGray,
                                        modifier = Modifier.size(20.dp)
                                    ) 
                                },
                                text = { Text("Hồ sơ cá nhân", color = TextGray, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    expanded = false
                                    onProfileClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = null, 
                                        tint = TextGray,
                                        modifier = Modifier.size(20.dp)
                                    ) 
                                },
                                text = { Text("Quản lí tin đăng", color = TextGray, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    expanded = false
                                    onPostHistoryClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null, 
                                        tint = TextGray,
                                        modifier = Modifier.size(20.dp)
                                    ) 
                                },
                                text = { Text("Tài khoản bị chặn", color = TextGray, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    expanded = false
                                    onBlockedUsersClick()
                                }
                            )
                            if (currentUser.role == "ADMIN") {
                                DropdownMenuItem(
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Dashboard, 
                                            contentDescription = null, 
                                            tint = TextGray,
                                            modifier = Modifier.size(20.dp)
                                        ) 
                                    },
                                    text = { Text("Quản trị hệ thống", color = TextGray, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        expanded = false
                                        onAdminDashboardClick()
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text("Đăng xuất", color = Color.Red) },
                                onClick = {
                                    expanded = false
                                    onLogoutClick()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
        }
    }
}
