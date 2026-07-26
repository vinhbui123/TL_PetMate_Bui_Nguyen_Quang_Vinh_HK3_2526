package com.example.petmate.ui.org

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.OrganizationProfileDto
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgProfileScreen(
    initialOrg: OrganizationProfileDto?,
    viewModel: OrganizationViewModel? = null,
    onBack: () -> Unit,
    onManageMembers: () -> Unit,
    onEditClick: (OrganizationProfileDto) -> Unit = {},
    onLeaveSuccess: () -> Unit = {},
    currentUserRole: String? = null
) {
    var org by remember(initialOrg?.id) { mutableStateOf(initialOrg) }
    val isOwner = currentUserRole == "RESCUE_ORG"
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDissolveConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(initialOrg?.id) {
        if (initialOrg?.id != null) {
            try {
                val response = com.example.petmate.network.NetworkClient.orgApi.getOrg(initialOrg.id)
                if (response.isSuccessful && response.body() != null) {
                    org = response.body()
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }
    
    val currentOrg = org
    if (currentOrg == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryPeach)
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hồ sơ Tổ chức", fontWeight = FontWeight.ExtraBold, color = DeepBrown) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về", tint = DeepBrown)
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { onEditClick(currentOrg) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = DeepBrown)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SoftPeach)
                            .border(2.dp, PrimaryPeach.copy(alpha = 0.5f), CircleShape)
                    ) {
                        if (!currentOrg.logoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentOrg.logoUrl,
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.HomeWork, 
                                contentDescription = null, 
                                modifier = Modifier.align(Alignment.Center).size(50.dp),
                                tint = DarkPeach
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = currentOrg.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrown
                    )
                    
                    if (currentOrg.isVerified) {
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                val displayBadge = when {
                                    currentOrg.badgeLabel?.contains("A'Ã£") == true -> "Cá nhân đã xác minh"
                                    currentOrg.orgType == "INDEPENDENT_FOSTER" -> "Cá nhân đã xác minh"
                                    else -> currentOrg.badgeLabel ?: "Đã xác minh"
                                }
                                
                                Text(
                                    text = displayBadge,
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Status Badge
                    val statusColor = when (currentOrg.status) {
                        "APPROVED" -> SuccessGreen
                        "PENDING" -> AccentOrange
                        else -> ErrorRed
                    }
                    val statusLabel = when (currentOrg.status) {
                        "APPROVED" -> "Hoạt động"
                        "PENDING" -> "Chờ duyệt"
                        else -> "Từ chối"
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(statusLabel, color = statusColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            if (currentOrg.status == "APPROVED") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onManageMembers,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (isOwner) "Quản lý thành viên" else "Danh sách thành viên",
                            fontWeight = FontWeight.Bold, 
                            fontSize = 16.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showLeaveConfirm = true }, 
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rời khỏi tổ chức", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showLeaveConfirm) {
                AlertDialog(
                    onDismissRequest = { showLeaveConfirm = false },
                    title = { Text("Rời khỏi tổ chức?", fontWeight = FontWeight.Bold) },
                    text = { Text("Bạn có chắc chắn muốn rời khỏi tổ chức '${currentOrg.name}' không?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLeaveConfirm = false
                                if (viewModel != null) {
                                    coroutineScope.launch {
                                        val success = viewModel.leaveOrganization(currentOrg.id!!)
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Đã rời khỏi tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                                            onLeaveSuccess()
                                        } else {
                                            val err = viewModel.error.value
                                            if (err?.contains("chuyển quyền") == true) {
                                                onManageMembers()
                                            } else if (err?.contains("giải thể") == true) {
                                                showDissolveConfirm = true
                                            } else {
                                                android.widget.Toast.makeText(context, err ?: "Lỗi rời tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    onManageMembers()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Xác nhận rời", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLeaveConfirm = false }) {
                            Text("Hủy", color = TextGray)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White
                )
            }

            if (showDissolveConfirm) {
                AlertDialog(
                    onDismissRequest = { showDissolveConfirm = false },
                    title = { Text("Giải thể tổ chức?", fontWeight = FontWeight.Bold) },
                    text = { Text("Bạn là thành viên duy nhất. Rời đi đồng nghĩa với việc tổ chức sẽ bị giải thể hoàn toàn và không thể khôi phục. Bạn có chắc chắn muốn giải thể?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDissolveConfirm = false
                                if (viewModel != null) {
                                    coroutineScope.launch {
                                        val success = viewModel.dissolveOrganization(currentOrg.id!!)
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Đã giải thể tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                                            onLeaveSuccess()
                                        } else {
                                            android.widget.Toast.makeText(context, viewModel.error.value ?: "Lỗi giải thể", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Giải thể", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDissolveConfirm = false }) {
                            Text("Hủy", color = TextGray)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Sections
            SectionCard("Thông tin liên hệ", Icons.AutoMirrored.Filled.ContactSupport) {
                InfoRow(Icons.Default.LocationOn, "Địa chỉ", currentOrg.address)
                InfoRow(Icons.Default.Phone, "Số điện thoại", currentOrg.phone ?: currentOrg.contact)
                InfoRow(Icons.Default.Email, "Email", currentOrg.email ?: "-")
                InfoRow(Icons.Default.Language, "Website", currentOrg.website ?: "-")
            }

            SectionCard("Thông tin Tổ chức", Icons.Default.Business) {
                InfoRow(Icons.Default.Category, "Loại hình", currentOrg.orgType ?: "-")
                InfoRow(Icons.Default.Description, "Mô tả", currentOrg.description)
            }

            SectionCard("Người đại diện", Icons.Default.Person) {
                val repName = currentOrg.representativeName?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"
                val repRoleStr = currentOrg.representativeRole?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                InfoRow(Icons.Default.Badge, "Đại diện", "$repName$repRoleStr")
                if (currentOrg.representativePhone?.isNotBlank() == true) {
                    InfoRow(Icons.Default.PhoneIphone, "SĐT đại diện", currentOrg.representativePhone)
                }
                if (currentOrg.representativeEmail?.isNotBlank() == true) {
                    InfoRow(Icons.Default.AlternateEmail, "Email đại diện", currentOrg.representativeEmail)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PrimaryPeach, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DeepBrown)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = IconGray, 
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = IconGray)
            Text(value, fontSize = 15.sp, color = TextGray, fontWeight = FontWeight.Medium)
        }
    }
}
