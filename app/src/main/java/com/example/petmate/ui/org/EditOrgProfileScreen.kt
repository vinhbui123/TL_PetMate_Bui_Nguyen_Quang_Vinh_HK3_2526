package com.example.petmate.ui.org

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.petmate.model.OrganizationProfileDto
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrgProfileScreen(
    org: OrganizationProfileDto,
    viewModel: OrganizationViewModel,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()

    var name by remember { mutableStateOf(org.name) }
    var logoUrl by remember { mutableStateOf(org.logoUrl ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }
    var address by remember { mutableStateOf(org.address) }
    var phone by remember { mutableStateOf(org.phone ?: org.contact) }
    var email by remember { mutableStateOf(org.email ?: "") }
    var website by remember { mutableStateOf(org.website ?: "") }
    var fanpage by remember { mutableStateOf(org.fanpage ?: "") }
    var description by remember { mutableStateOf(org.description) }

    var repName by remember { mutableStateOf(org.representativeName ?: "") }
    var repPhone by remember { mutableStateOf(org.representativePhone ?: "") }
    var repEmail by remember { mutableStateOf(org.representativeEmail ?: "") }
    var repIdType by remember { mutableStateOf(org.representativeIdType ?: "CMND") }
    var repIdNumber by remember { mutableStateOf(org.representativeIdNumber ?: "") }
    var repSocial by remember { mutableStateOf(org.representativeSocialUrl ?: "") }
    var repRole by remember { mutableStateOf(org.representativeRole ?: "OWNER") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa Hồ sơ Tổ chức") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar / Logo Preview
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Logo Trạm",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "Logo Trạm",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Chọn Logo", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên tổ chức *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Địa chỉ *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email liên hệ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fanpage,
                onValueChange = { fanpage = it },
                label = { Text("Fanpage (Facebook/Instagram)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Mô tả hoạt động") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Text("Thông tin người đại diện", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            
            OutlinedTextField(value = repName, onValueChange = { repName = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = repPhone, onValueChange = { repPhone = it }, label = { Text("Số điện thoại trực tiếp") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = repEmail, onValueChange = { repEmail = it }, label = { Text("Email cá nhân") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || address.isBlank() || phone.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập các trường bắt buộc (*)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val updatedDto = org.copy(
                        name = name,
                        logoUrl = logoUrl.takeIf { it.isNotBlank() },
                        address = address,
                        phone = phone,
                        contact = phone, // Sync with contact for backward compatibility
                        email = email.takeIf { it.isNotBlank() },
                        website = website.takeIf { it.isNotBlank() },
                        fanpage = fanpage.takeIf { it.isNotBlank() },
                        description = description,
                        representativeName = repName.takeIf { it.isNotBlank() },
                        representativePhone = repPhone.takeIf { it.isNotBlank() },
                        representativeEmail = repEmail.takeIf { it.isNotBlank() },
                        representativeIdType = repIdType,
                        representativeIdNumber = repIdNumber.takeIf { it.isNotBlank() },
                        representativeSocialUrl = repSocial.takeIf { it.isNotBlank() },
                        representativeRole = repRole
                    )
                    coroutineScope.launch {
                        val success = viewModel.updateOrgProfileWithLogo(org.id!!, updatedDto, imageUri, context)
                        if (success) {
                            Toast.makeText(context, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
                            onSaveSuccess()
                        } else {
                            Toast.makeText(context, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Lưu thay đổi")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
