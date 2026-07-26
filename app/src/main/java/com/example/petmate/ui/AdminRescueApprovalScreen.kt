package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.petmate.model.OrganizationProfileDto
import com.example.petmate.model.OrgReviewRequest
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRescueApprovalScreen(onBack: () -> Unit) {
    var pendingOrgs by remember { mutableStateOf<List<OrganizationProfileDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = NetworkClient.orgApi.listOrgs("PENDING")
            if (response.isSuccessful) {
                pendingOrgs = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Lỗi tải danh sách", android.widget.Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duyệt Trạm Cứu Hộ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                pendingOrgs.isEmpty() -> {
                    Text(
                        text = "Không có yêu cầu đăng ký nào.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pendingOrgs) { org ->
                            PendingOrgCard(
                                org = org,
                                onReview = { status, note ->
                                    coroutineScope.launch {
                                        try {
                                            val req = OrgReviewRequest(status, adminNote = if(status == "NEEDS_SUPPLEMENT") note else null, rejectionReason = if(status == "REJECTED") note else null)
                                            val response = NetworkClient.orgApi.reviewOrg(org.id!!, req)
                                            if (response.isSuccessful) {
                                                pendingOrgs = pendingOrgs.filter { it.id != org.id }
                                                android.widget.Toast.makeText(context, "Đã xử lý: $status", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Lỗi khi xử lý", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Lỗi hệ thống", android.widget.Toast.LENGTH_SHORT).show()
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

@Composable
fun PendingOrgCard(org: OrganizationProfileDto, onReview: (String, String?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var reviewNote by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!org.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = org.logoUrl,
                        contentDescription = "Logo",
                        modifier = Modifier.size(50.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(org.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(org.orgType ?: "Unknown", fontSize = 14.sp, color = Color.Gray)
                    Text("Đại diện: ${org.ownerName ?: org.representativeName ?: "Unknown"}", fontSize = 12.sp, color = Color.DarkGray)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Email: ${org.email}", fontSize = 12.sp)
            Text("SĐT: ${org.phone}", fontSize = 12.sp)
            Text("Địa chỉ: ${org.address}", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onReview("APPROVED", null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Duyệt", color = Color.White)
                }
                OutlinedButton(
                    onClick = { 
                        actionType = "NEEDS_SUPPLEMENT"
                        showDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Bổ sung")
                }
                OutlinedButton(
                    onClick = { 
                        actionType = "REJECTED"
                        showDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Từ chối")
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (actionType == "REJECTED") "Lý do từ chối" else "Yêu cầu bổ sung") },
            text = {
                OutlinedTextField(
                    value = reviewNote,
                    onValueChange = { reviewNote = it },
                    label = { Text("Nhập nội dung...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { 
                    showDialog = false
                    onReview(actionType, reviewNote) 
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Hủy") }
            }
        )
    }
}
