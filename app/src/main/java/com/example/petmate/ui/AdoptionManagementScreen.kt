package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.AsyncImage
import com.example.petmate.model.AdoptionResponse
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.PrimaryPeach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionManagementScreen(currentUserId: Long? = null, onBack: (() -> Unit)? = null) {
    var sentApplications by remember { mutableStateOf<List<AdoptionResponse>>(emptyList()) }
    var receivedApplications by remember { mutableStateOf<List<AdoptionResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Đơn đã gửi", "Đơn nhận được", "Lịch sử nhận nuôi")
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val loadData = {
        coroutineScope.launch {
            isLoading = true
            try {
                sentApplications = NetworkClient.apiService.getMyAdoptionApplications()
                
                val personalReceived = NetworkClient.apiService.getReceivedAdoptionApplications()
                var orgReceived: List<AdoptionResponse> = emptyList()
                
                try {
                    val orgResponse = NetworkClient.orgApi.getMyOrg()
                    if (orgResponse.isSuccessful && orgResponse.body() != null) {
                        val orgId = orgResponse.body()!!.id ?: 0L
                        orgReceived = NetworkClient.apiService.getOrgAdoptions(orgId)
                    }
                } catch (e: Exception) {
                    // Ignore org fetch error
                }
                
                receivedApplications = (personalReceived + orgReceived).distinctBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentUserId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Duyệt đơn") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = PrimaryPeach
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPeach)
                    }
                    selectedTabIndex == 0 && sentApplications.isEmpty() -> {
                        Text("Bạn chưa gửi đơn nhận nuôi nào.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    }
                    selectedTabIndex == 1 && receivedApplications.isEmpty() -> {
                        Text("Chưa có đơn đăng ký nhận nuôi nào.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    }
                    selectedTabIndex == 2 && receivedApplications.none { it.status == "APPROVED" } -> {
                        Text("Chưa có bé nào được cho nhận nuôi thành công.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    }
                    else -> {
                        val currentList = when (selectedTabIndex) {
                            0 -> sentApplications
                            1 -> receivedApplications
                            else -> receivedApplications.filter { it.status == "APPROVED" }
                        }
                        
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(currentList) { app ->
                                if (selectedTabIndex == 0) {
                                    SentApplicationCard(
                                        app = app,
                                        onCancel = {
                                            coroutineScope.launch {
                                                try {
                                                    NetworkClient.apiService.cancelAdoptionApplication(app.id)
                                                    Toast.makeText(context, "Hủy đơn thành công!", Toast.LENGTH_SHORT).show()
                                                    loadData()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Lỗi hủy đơn!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    ApplicationCard(
                                        app = app,
                                        onStatusChange = { newStatus ->
                                            coroutineScope.launch {
                                                try {
                                                    val updatedApp = NetworkClient.apiService.updateAdoptionStatus(app.id, newStatus)
                                                    receivedApplications = receivedApplications.map { if (it.id == updatedApp.id) updatedApp else it }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
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
        }
    }
}

@Composable
fun SentApplicationCard(app: AdoptionResponse, onCancel: () -> Unit) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Xác nhận hủy đơn") },
            text = { Text("Bạn có chắc chắn muốn hủy đơn xin nhận nuôi bé ${app.petName} không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hủy Đơn") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Đóng") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!app.petImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = app.petImageUrl,
                        contentDescription = "Pet Image",
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bé: ${app.petName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trạng thái: ${app.status}", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        color = when (app.status) {
                            "APPROVED" -> Color.Green
                            "REJECTED" -> Color.Red
                            else -> Color(0xFFFFA000)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Lời nhắn của bạn:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(app.message, fontSize = 14.sp, color = Color.DarkGray)

            if (app.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Hủy Yêu Cầu")
                }
            }
        }
    }
}

@Composable
fun ApplicationCard(app: AdoptionResponse, onStatusChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!app.petImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = app.petImageUrl,
                        contentDescription = "Pet Image",
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Bé: ${app.petName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Trạng thái: ${app.status}", 
                        fontSize = 12.sp, 
                        color = when (app.status) {
                            "APPROVED" -> Color.Green
                            "REJECTED" -> Color.Red
                            else -> Color(0xFFFFA000)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!app.applicantAvatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = app.applicantAvatarUrl,
                        contentDescription = "Applicant",
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(app.applicantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(app.applicantPhone ?: "Không có SĐT", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Lý do:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(app.message, fontSize = 14.sp, color = Color.DarkGray)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Kinh nghiệm:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(app.experience, fontSize = 14.sp, color = Color.DarkGray)

            if (app.status == "PENDING") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStatusChange("APPROVED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Duyệt")
                    }
                    OutlinedButton(
                        onClick = { onStatusChange("REJECTED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Từ chối")
                    }
                }
            }
        }
    }
}
