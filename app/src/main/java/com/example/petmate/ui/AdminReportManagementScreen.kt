package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.petmate.model.ReportResponse
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportManagementScreen(onBack: () -> Unit) {
    var reports by remember { mutableStateOf<List<ReportResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterStatus by remember { mutableStateOf("PENDING") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val fetchReports = {
        coroutineScope.launch {
            isLoading = true
            try {
                val allReports = NetworkClient.apiService.getAllReports()
                reports = allReports.sortedByDescending { it.id }
            } catch (_: Exception) {
                Toast.makeText(context, "Lỗi khi tải danh sách báo cáo", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchReports()
    }

    val displayedReports = reports.filter { it.status == filterStatus }

    Scaffold(
        containerColor = BackgroundBeige,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Quản lý Báo Cáo", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrown
                    ) 
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftPeach)
                    ) {
                        Text(
                            "Trở về",
                            color = DeepBrown,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Tabs
            val filters = listOf(
                "PENDING" to "Chờ xử lý",
                "RESOLVED" to "Đã xử lý (Vi phạm)",
                "REJECTED" to "Đã từ chối"
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filters) { (status, label) ->
                    val isSelected = filterStatus == status
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { filterStatus = status },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) AccentOrange else CardWhite,
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextGray
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            } else if (displayedReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Không có báo cáo nào", color = IconGray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayedReports, key = { it.id }) { report ->
                        ReportItemCard(
                            report = report,
                            onUpdateStatus = { status ->
                                coroutineScope.launch {
                                    try {
                                        NetworkClient.apiService.updateReportStatus(report.id, status)
                                        Toast.makeText(context, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                                        fetchReports()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
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
fun ReportItemCard(
    report: ReportResponse,
    onUpdateStatus: (String) -> Unit
) {
    val dateStr = remember(report.createdAt) {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(report.createdAt)
            date?.let { formatter.format(it) } ?: report.createdAt
        } catch (e: Exception) {
            report.createdAt
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.reason,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = TextGray
                    )
                }
                Surface(
                    color = SoftPeach,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#${report.id}",
                        color = AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!report.description.isNullOrEmpty()) {
                Surface(
                    color = InputBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.description, 
                        fontSize = 14.sp, 
                        color = TextGray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            ReportInfoRow("Người gửi", report.reporter.fullName)
            
            if (report.reportedPet != null) {
                ReportInfoRow("Bài đăng bị báo cáo", "${report.reportedPet.name} (ID: ${report.reportedPet.id})")
            }
            
            if (report.reportedUser != null) {
                ReportInfoRow("Tài khoản bị báo cáo", "${report.reportedUser.fullName} (ID: ${report.reportedUser.id})")
            }
            
            if (report.reportedMessage != null) {
                ReportInfoRow("Tin nhắn bị báo cáo", "\"${report.reportedMessage.content}\" (ID: ${report.reportedMessage.id})")
            }

            ReportInfoRow("Ngày gửi", dateStr)
            
            if (report.status == "PENDING") {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onUpdateStatus("RESOLVED") },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Vi phạm", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onUpdateStatus("REJECTED") },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Hợp lệ", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                val (statusText, statusColor) = when(report.status) {
                    "RESOLVED" -> "Đã xử lý: Vi phạm" to ErrorRed
                    "REJECTED" -> "Đã xử lý: Hợp lệ" to SuccessGreen
                    else -> "" to Color.Gray
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ReportInfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            color = IconGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value ?: "Không có",
            fontSize = 13.sp,
            color = TextGray,
            fontWeight = FontWeight.Bold
        )
    }
}
