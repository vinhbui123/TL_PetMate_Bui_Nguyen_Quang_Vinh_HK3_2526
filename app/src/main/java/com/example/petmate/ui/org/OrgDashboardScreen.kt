package com.example.petmate.ui.org

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.model.AdoptionResponse
import com.example.petmate.model.Pet
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.BackgroundBeige
import com.example.petmate.ui.theme.PrimaryPeach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToAdoptions: () -> Unit,
    onNavigateToOrgProfile: () -> Unit
) {
    var myPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var receivedApplications by remember { mutableStateOf<List<AdoptionResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                val orgResponse = NetworkClient.orgApi.getMyOrg()
                if (orgResponse.isSuccessful && orgResponse.body() != null) {
                    val orgId = orgResponse.body()!!.id ?: 0L
                    val pets = NetworkClient.apiService.getOrgPets(orgId)
                    val apps = NetworkClient.apiService.getOrgAdoptions(orgId)
                    myPets = pets
                    receivedApplications = apps
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val totalPets = myPets.size
    val pendingCount = receivedApplications.count { it.status == "PENDING" }
    val approvedCount = receivedApplications.count { it.status == "APPROVED" }

    Scaffold(
        containerColor = BackgroundBeige,
        topBar = {
            TopAppBar(
                title = { Text("Thống kê hoạt động tổ chức", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPeach)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tổng quan chỉ số",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Thú cưng",
                        value = totalPets.toString(),
                        subtitle = "đang quản lý",
                        icon = Icons.Default.Pets,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Đơn chờ duyệt",
                        value = pendingCount.toString(),
                        subtitle = "cần xử lý",
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Đã nhận nuôi",
                        value = approvedCount.toString(),
                        subtitle = "thành công",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lối tắt quản lý",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = onNavigateToAdoptions,
                        modifier = Modifier.weight(1f).height(90.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = PrimaryPeach, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Duyệt đơn nhận nuôi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Xem $pendingCount đơn mới", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Card(
                        onClick = onNavigateToOrgProfile,
                        modifier = Modifier.weight(1f).height(90.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HomeWork, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Hồ sơ Trạm", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Chỉnh sửa thông tin", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lịch sử nhận nuôi thành công mới nhất",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val approvedApps = receivedApplications.filter { it.status == "APPROVED" }
                if (approvedApps.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có bé nào được nhận nuôi thành công", color = Color.Gray)
                        }
                    }
                } else {
                    approvedApps.take(5).forEach { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Bé #${app.petId} - Người nhận: ${app.applicantName ?: "Thành viên"}", fontWeight = FontWeight.Bold)
                                    Text(text = "Kinh nghiệm: ${app.experience ?: "Chưa cập nhật"}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Thành công", color = Color(0xFF4CAF50), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Text(subtitle, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}
