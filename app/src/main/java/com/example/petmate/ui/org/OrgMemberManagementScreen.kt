package com.example.petmate.ui.org

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.InviteMemberDto
import com.example.petmate.model.OrgMemberDto
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgMemberManagementScreen(
    orgId: Long, 
    onBack: () -> Unit,
    onDissolved: () -> Unit,
    viewModel: OrganizationViewModel,
    onRefreshUser: () -> Unit = {}
) {
    var members by remember { mutableStateOf<List<OrgMemberDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showDissolveDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadMembers() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = NetworkClient.orgApi.getMembers(orgId)
                if (response.isSuccessful) {
                    members = response.body() ?: emptyList()
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, "Lỗi tải danh sách thành viên", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(orgId) {
        loadMembers()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Thành viên Tổ chức", 
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBrown
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Trở về",
                            tint = DeepBrown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInviteDialog = true },
                containerColor = PrimaryPeach,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Thêm thành viên")
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = viewModel.leaveOrganization(orgId)
                            if (success) {
                                android.widget.Toast.makeText(context, "Đã rời tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                                onRefreshUser()
                                onDissolved()
                            } else {
                                val error = viewModel.error.value
                                if (error?.contains("chuyển quyền") == true) {
                                    showTransferDialog = true
                                } else if (error?.contains("giải thể") == true) {
                                    showDissolveDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, error ?: "Không thể rời tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rời khỏi tổ chức", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPeach)
                }
            } else if (members.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Group, 
                        contentDescription = null, 
                        modifier = Modifier.size(80.dp),
                        tint = IconGray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Chưa có thành viên nào",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray
                    )
                    Text(
                        "Mời cộng tác viên để cùng quản lý trạm cứu hộ của bạn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IconGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(members) { member ->
                        MemberItem(
                            member = member,
                            onRemove = {
                                coroutineScope.launch {
                                    try {
                                        val response = NetworkClient.orgApi.removeMember(orgId, member.id!!)
                                        if (response.isSuccessful) {
                                            members = members.filter { it.id != member.id }
                                            android.widget.Toast.makeText(context, "Đã xóa thành viên", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Lỗi xóa thành viên", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { email, role ->
                coroutineScope.launch {
                    try {
                        val response = NetworkClient.orgApi.inviteMember(orgId, InviteMemberDto(email, role))
                        if (response.isSuccessful) {
                            loadMembers()
                            showInviteDialog = false
                            android.widget.Toast.makeText(context, "Đã gửi lời mời", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            val msg = if (errorBody?.contains("User not found") == true) "Không tìm thấy người dùng này" else "Mời thất bại"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Lỗi kết nối", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showTransferDialog) {
        TransferOwnershipDialog(
            members = members.filter { it.memberRole != "OWNER" },
            onDismiss = { showTransferDialog = false },
            onTransfer = { newOwnerId ->
                coroutineScope.launch {
                    val success = viewModel.transferOwnership(orgId, newOwnerId)
                    if (success) {
                        android.widget.Toast.makeText(context, "Đã chuyển quyền và rời tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                        onRefreshUser()
                        showTransferDialog = false
                        onDissolved()
                    } else {
                        android.widget.Toast.makeText(context, viewModel.error.value ?: "Lỗi chuyển quyền", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showDissolveDialog) {
        AlertDialog(
            onDismissRequest = { showDissolveDialog = false },
            title = { Text("Giải thể tổ chức?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn là thành viên duy nhất. Rời đi đồng nghĩa với việc tổ chức sẽ bị giải thể hoàn toàn và không thể khôi phục. Bạn có chắc chắn muốn tiếp tục?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = viewModel.dissolveOrganization(orgId)
                            if (success) {
                                android.widget.Toast.makeText(context, "Đã giải thể tổ chức", android.widget.Toast.LENGTH_SHORT).show()
                                onRefreshUser()
                                showDissolveDialog = false
                                onDissolved()
                            } else {
                                android.widget.Toast.makeText(context, viewModel.error.value ?: "Lỗi giải thể", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Giải thể", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDissolveDialog = false }) {
                    Text("Hủy", color = TextGray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun MemberItem(member: OrgMemberDto, onRemove: () -> Unit) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SoftPeach)
            ) {
                if (!member.userAvatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = member.userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        tint = DarkPeach
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.userName ?: "Thành viên mới",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DeepBrown
                )
                Text(
                    text = member.userEmail ?: "",
                    fontSize = 13.sp,
                    color = IconGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                RoleBadge(member.memberRole)
            }

            if (member.memberRole != "OWNER") {
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(
                        Icons.Default.RemoveCircleOutline, 
                        contentDescription = "Xóa", 
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Xóa thành viên?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa ${member.userName} khỏi tổ chức không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Hủy", color = TextGray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun RoleBadge(role: String?) {
    val (label, color, icon) = when (role) {
        "OWNER" -> Triple("Chủ sở hữu", DeepBrown, Icons.Default.VerifiedUser)
        "MANAGER" -> Triple("Quản lý", SuccessGreen, Icons.Default.AdminPanelSettings)
        "COLLABORATOR" -> Triple("Tình nguyện viên", AccentOrange, Icons.Default.Group)
        else -> Triple(role ?: "Thành viên", IconGray, Icons.Default.Person)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMemberDialog(onDismiss: () -> Unit, onInvite: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("COLLABORATOR") }
    val roles = listOf("MANAGER" to "Quản lý", "COLLABORATOR" to "Tình nguyện viên")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Mời Thành Viên", 
                fontWeight = FontWeight.ExtraBold, 
                color = DeepBrown,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            ) 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Nhập email người dùng bạn muốn mời vào tổ chức.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("user@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPeach,
                        focusedLabelColor = PrimaryPeach
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "Chọn vai trò:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                roles.forEach { (roleCode, roleName) ->
                    Surface(
                        onClick = { role = roleCode },
                        shape = RoundedCornerShape(12.dp),
                        color = if (role == roleCode) SoftPeach else Color.Transparent,
                        border = if (role == roleCode) androidx.compose.foundation.BorderStroke(1.dp, PrimaryPeach) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = role == roleCode,
                                onClick = { role = roleCode },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPeach)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = roleName,
                                fontWeight = if (role == roleCode) FontWeight.Bold else FontWeight.Normal,
                                color = if (role == roleCode) DeepBrown else TextGray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onInvite(email, role) },
                enabled = email.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) {
                Text("Gửi lời mời", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hủy bỏ", color = IconGray)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun TransferOwnershipDialog(
    members: List<OrgMemberDto>,
    onDismiss: () -> Unit,
    onTransfer: (Long) -> Unit
) {
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Chuyển Quyền Chủ Trạm", 
                fontWeight = FontWeight.ExtraBold, 
                color = DeepBrown
            ) 
        },
        text = {
            Column {
                Text(
                    "Bạn phải chuyển quyền chủ sở hữu cho một thành viên khác trước khi rời đi. Hãy chọn một người bên dưới:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (members.isEmpty()) {
                    Text("Không có thành viên nào khác để chuyển quyền.", color = Color.Red)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(members) { member ->
                            Surface(
                                onClick = { selectedMemberId = member.id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMemberId == member.id) SoftPeach else Color.Transparent,
                                border = if (selectedMemberId == member.id) androidx.compose.foundation.BorderStroke(1.dp, PrimaryPeach) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedMemberId == member.id,
                                        onClick = { selectedMemberId = member.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryPeach)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = member.userName ?: "Thành viên vô danh",
                                        fontWeight = if (selectedMemberId == member.id) FontWeight.Bold else FontWeight.Normal,
                                        color = DeepBrown
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedMemberId?.let { onTransfer(it) } },
                enabled = selectedMemberId != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) {
                Text("Xác nhận & Rời đi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = IconGray)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
