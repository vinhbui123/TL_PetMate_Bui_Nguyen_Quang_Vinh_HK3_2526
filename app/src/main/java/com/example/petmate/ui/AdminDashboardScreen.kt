package com.example.petmate.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.ui.theme.PrimaryPeach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToRescueApproval: () -> Unit,
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToPostApproval: () -> Unit = {},
    onNavigateToBroadcast: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onFeatureNotReady: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Trung tâm Điều khiển",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DashboardCard(
                        title = "Duyệt Trạm Cứu Hộ",
                        icon = Icons.Default.LocalHospital,
                        color = Color(0xFF4CAF50), // Green
                        onClick = onNavigateToRescueApproval
                    )
                }
                item {
                    DashboardCard(
                        title = "Quản lý Người dùng",
                        icon = Icons.Default.People,
                        color = Color(0xFF2196F3), // Blue
                        onClick = onNavigateToUserManagement
                    )
                }
                item {
                    DashboardCard(
                        title = "Quản lý bài đăng",
                        icon = Icons.Default.FactCheck,
                        color = Color(0xFFFF9800), // Orange
                        onClick = onNavigateToPostApproval
                    )
                }
                item {
                    DashboardCard(
                        title = "Xử lý Vi phạm",
                        icon = Icons.Default.Gavel,
                        color = Color(0xFFF44336), // Red
                        onClick = onNavigateToReports
                    )
                }
                item {
                    DashboardCard(
                        title = "Nhật ký Hệ thống",
                        icon = Icons.Default.FormatListBulleted,
                        color = Color(0xFF9C27B0), // Purple
                        onClick = onNavigateToLogs
                    )
                }
                item {
                    DashboardCard(
                        title = "Gửi Thông báo",
                        icon = Icons.Default.Campaign,
                        color = Color(0xFF00BCD4), // Cyan
                        onClick = onNavigateToBroadcast
                    )
                }
                item {
                    DashboardCard(
                        title = "Thống kê & Báo cáo",
                        icon = Icons.Default.PieChart,
                        color = Color(0xFF607D8B), // Blue Grey
                        onClick = onNavigateToStats
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square card
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                color = Color.DarkGray
            )
        }
    }
}
