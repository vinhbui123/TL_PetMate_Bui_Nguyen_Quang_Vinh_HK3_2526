package com.example.petmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.model.SystemStatsDto
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(onBack: () -> Unit) {
    var stats by remember { mutableStateOf<SystemStatsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                stats = NetworkClient.apiService.getSystemStats()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thống kê hệ thống", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (stats != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item { StatItem("Người dùng", stats!!.totalUsers.toString(), Icons.Default.People, Color(0xFF2196F3)) }
                item { StatItem("Tổ chức", stats!!.totalOrganizations.toString(), Icons.Default.Business, Color(0xFF4CAF50)) }
                item { StatItem("Thú cưng", stats!!.totalPets.toString(), Icons.Default.Pets, Color(0xFFFF9800)) }
                item { StatItem("Tổng Đơn", stats!!.totalAdoptions.toString(), Icons.Default.Assignment, Color(0xFF9C27B0)) }
                item { StatItem("Đơn chờ duyệt", stats!!.pendingAdoptions.toString(), Icons.Default.HourglassEmpty, Color(0xFFF44336)) }
                item { StatItem("Đơn thành công", stats!!.approvedAdoptions.toString(), Icons.Default.CheckCircle, Color(0xFF00BCD4)) }
                item { StatItem("Báo cáo vi phạm", stats!!.totalReports.toString(), Icons.Default.ReportProblem, Color(0xFFE91E63)) }
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 14.sp, color = Color.Gray)
        }
    }
}
