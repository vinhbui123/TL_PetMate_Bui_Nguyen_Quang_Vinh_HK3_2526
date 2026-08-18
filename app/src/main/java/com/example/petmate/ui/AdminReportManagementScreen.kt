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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Quản lý Báo Cáo", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Trở về",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Tabs (Modern Admin Pill Filter)
            val filters = listOf(
                "PENDING" to "Chờ xử lý",
                "RESOLVED" to "Đã xử lý (Vi phạm)",
                "REJECTED" to "Đã từ chối"
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filters) { (status, label) ->
                    val isSelected = filterStatus == status
                    Surface(
                        modifier = Modifier
                            .height(38.dp)
                            .clickable { filterStatus = status },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF6366F1) else Color.White,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                }
            } else if (displayedReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Không có báo cáo nào", color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#${report.id}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            if (!report.description.isNullOrEmpty()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.description, 
                        fontSize = 13.sp, 
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
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
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onUpdateStatus("RESOLVED") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vi phạm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onUpdateStatus("REJECTED") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hợp lệ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                val (statusText, statusColor) = when(report.status) {
                    "RESOLVED" -> "Đã xử lý: Vi phạm" to Color(0xFFEF4444)
                    "REJECTED" -> "Đã xử lý: Hợp lệ" to Color(0xFF10B981)
                    else -> "" to Color.Gray
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp),
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
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value ?: "Không có",
            fontSize = 13.sp,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.SemiBold
        )
    }
}
