package com.example.petmate.ui.org

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.OrganizationProfileDto
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgRegisterScreen(viewModel: OrganizationViewModel, onBack: () -> Unit) {
    val currentStep by viewModel.currentStep.collectAsState()
    val formData by viewModel.formData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()
    val context = LocalContext.current

    val existingOrg by viewModel.existingOrg.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetSubmitSuccess()
        viewModel.loadExistingOrg()
    }

    if (submitSuccess) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Đăng ký thành công!", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBrown
            )
            Text(
                "Hồ sơ của bạn đã được gửi và đang chờ xét duyệt. Chúng tôi sẽ phản hồi sớm nhất có thể.", 
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = TextGray
            )
            Button(
                onClick = {
                    viewModel.resetSubmitSuccess()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) {
                Text("Trở về trang cá nhân", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    if (existingOrg != null && existingOrg?.status == "PENDING") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = PrimaryPeach,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Hồ sơ đang chờ xét duyệt", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBrown
            )
            Text(
                "Hồ sơ trạm cứu hộ '${existingOrg?.name}' của bạn đã được gửi và đang trong quá trình xét duyệt.", 
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = TextGray
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) {
                Text("Quay lại", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    if (existingOrg != null && existingOrg?.status == "REJECTED") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Hồ sơ bị từ chối", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBrown
            )
            Text(
                existingOrg?.rejectionReason ?: "Hồ sơ chưa đạt yêu cầu của hệ thống.", 
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = TextGray
            )
            Button(
                onClick = {
                    viewModel.resetState()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) {
                Text("Đăng ký hồ sơ mới", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Quay lại", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Đăng ký trạm cứu hộ", 
                        fontWeight = FontWeight.ExtraBold, 
                        color = DeepBrown,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về", tint = DeepBrown)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            OrgRegistrationStepperComponent(currentStep = currentStep)

            error?.let {
                Surface(
                    color = ErrorRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = it, 
                            color = ErrorRed, 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (currentStep) {
                    1 -> Step1OrgTypeScreen(formData) { type -> viewModel.updateFormData { it.copy(orgType = type) } }
                    2 -> Step2OrgInfoScreen(formData, viewModel.isIndependentFoster()) { f -> viewModel.updateFormData { f } }
                    3 -> Step3RepresentativeScreen(formData) { f -> viewModel.updateFormData { f } }
                    4 -> Step4DocumentsScreen(viewModel)
                    5 -> Step5PolicyTermsScreen(formData) { f -> viewModel.updateFormData { f } }
                    6 -> Step6ReviewSubmitScreen(formData)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { viewModel.prevStep() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, InputBorder)
                    ) {
                        Text("Quay lại", color = TextGray, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 6) viewModel.nextStep()
                        else viewModel.submit()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (currentStep == 6) "Gửi hồ sơ" else "Tiếp tục", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step1OrgTypeScreen(formData: OrganizationProfileDto, onTypeSelected: (String) -> Unit) {
    Column {
        Text(
            "Bước 1: Chọn loại hình hoạt động", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = DeepBrown,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val types = listOf(
            "PRIVATE_RESCUE" to "Tổ chức cứu hộ tư nhân",
            "PUBLIC_SHELTER" to "Trạm cứu hộ công lập",
            "VET_CLINIC" to "Phòng khám thú y",
            "INDEPENDENT_FOSTER" to "Cá nhân nuôi dưỡng"
        )
        
        types.forEach { (type, label) ->
            Surface(
                onClick = { onTypeSelected(type) },
                shape = RoundedCornerShape(12.dp),
                color = if (formData.orgType == type) SoftPeach else InputBackground,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                border = if (formData.orgType == type) BorderStroke(2.dp, PrimaryPeach) else null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    RadioButton(
                        selected = formData.orgType == type,
                        onClick = { onTypeSelected(type) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryPeach)
                    )
                    Text(
                        label, 
                        modifier = Modifier.padding(start = 12.dp),
                        fontWeight = if (formData.orgType == type) FontWeight.Bold else FontWeight.Medium,
                        color = if (formData.orgType == type) DeepBrown else TextGray
                    )
                }
            }
        }
        
        if (formData.orgType == "INDEPENDENT_FOSTER") {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                color = SoftPeach.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryPeach, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Lưu ý: Cá nhân nuôi dưỡng yêu cầu xác minh nhẹ hơn (CCCD, ảnh cơ sở, xác nhận thú y) và sẽ có nhãn 'Cá nhân đã xác minh'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun Step2OrgInfoScreen(formData: OrganizationProfileDto, isFoster: Boolean, update: (OrganizationProfileDto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Bước 2: Thông tin cơ bản", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = DeepBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        StyledTextField(value = formData.name, onValueChange = { update(formData.copy(name = it)) }, label = "Tên trạm / tổ chức")
        StyledTextField(value = formData.address, onValueChange = { update(formData.copy(address = it)) }, label = "Địa chỉ hoạt động")
        StyledTextField(value = formData.phone ?: "", onValueChange = { update(formData.copy(phone = it)) }, label = "Số điện thoại liên hệ")
        StyledTextField(value = formData.email ?: "", onValueChange = { update(formData.copy(email = it)) }, label = "Email liên hệ")
        
        if (!isFoster) {
            StyledTextField(
                value = formData.foundedYear?.toString() ?: "", 
                onValueChange = { update(formData.copy(foundedYear = it.toIntOrNull())) }, 
                label = "Năm thành lập"
            )
            StyledTextField(value = formData.businessAddress ?: "", onValueChange = { update(formData.copy(businessAddress = it)) }, label = "Địa chỉ ĐKKD")
            StyledTextField(value = formData.taxCode ?: "", onValueChange = { update(formData.copy(taxCode = it)) }, label = "Mã số thuế / Số ĐKKD")
            StyledTextField(value = formData.website ?: "", onValueChange = { update(formData.copy(website = it)) }, label = "Website (nếu có)")
            StyledTextField(value = formData.fanpage ?: "", onValueChange = { update(formData.copy(fanpage = it)) }, label = "Fanpage (Facebook/Tiktok)")
        }
    }
}

@Composable
fun Step3RepresentativeScreen(formData: OrganizationProfileDto, update: (OrganizationProfileDto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Bước 3: Người đại diện", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = DeepBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        StyledTextField(value = formData.representativeName ?: "", onValueChange = { update(formData.copy(representativeName = it)) }, label = "Họ và tên người đại diện")
        StyledTextField(value = formData.representativePhone ?: "", onValueChange = { update(formData.copy(representativePhone = it)) }, label = "Số điện thoại trực tiếp")
        StyledTextField(value = formData.representativeEmail ?: "", onValueChange = { update(formData.copy(representativeEmail = it)) }, label = "Email cá nhân")
        
        Text("Vai trò tại tổ chức:", fontWeight = FontWeight.Bold, color = DeepBrown, modifier = Modifier.padding(top = 8.dp))
        
        val roles = listOf(
            "OWNER" to "Người sáng lập / Chủ sở hữu", 
            "MANAGER" to "Quản lý điều hành", 
            "COORDINATOR" to "Điều phối viên", 
            "VOLUNTEER" to "Tình nguyện viên nòng cốt"
        )
        
        roles.forEach { (role, label) ->
            Surface(
                onClick = { update(formData.copy(representativeRole = role)) },
                shape = RoundedCornerShape(12.dp),
                color = if (formData.representativeRole == role) SoftPeach else InputBackground,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                border = if (formData.representativeRole == role) BorderStroke(1.dp, PrimaryPeach) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    RadioButton(
                        selected = formData.representativeRole == role, 
                        onClick = { update(formData.copy(representativeRole = role)) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryPeach)
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp), color = TextGray)
                }
            }
        }
    }
}

@Composable
fun Step4DocumentsScreen(viewModel: OrganizationViewModel) {
    val uploadedDocs by viewModel.uploadedDocs.collectAsState()
    val requiredDocs = viewModel.getRequiredDocTypes()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val docTypeLabels = mapOf(
        "BUSINESS_LICENSE" to ("Giấy phép kinh doanh" to "Ảnh chụp bản gốc giấy phép kinh doanh của tổ chức"),
        "VET_COOPERATION" to ("Xác nhận thú y" to "Hợp đồng hoặc giấy xác nhận hợp tác với phòng khám"),
        "FACILITY_PHOTO" to ("Ảnh cơ sở vật chất" to "Ảnh chụp không gian nuôi dưỡng và chăm sóc thú cưng"),
        "ID_CARD" to ("CCCD / Hộ chiếu" to "Ảnh chụp mặt trước CCCD người đại diện"),
        "LIVING_SPACE_PHOTO" to ("Ảnh không gian sống" to "Ảnh chụp nơi bạn sẽ nuôi dưỡng thú cưng")
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Bước 4: Tải lên giấy tờ", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = DeepBrown,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        requiredDocs.forEach { docType ->
            val doc = uploadedDocs.find { it.docType == docType }
            val (label, description) = docTypeLabels[docType] ?: (docType to "")
            
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bytes = inputStream?.readBytes()
                            if (bytes != null) {
                                viewModel.uploadDocument(docType, bytes, "doc_${docType}.jpg")
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi đọc file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        // Preview or Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (doc?.fileUrl != null) {
                                AsyncImage(
                                    model = doc.fileUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = if (docType == "FACILITY_PHOTO" || docType == "LIVING_SPACE_PHOTO") 
                                        Icons.Default.PhotoCamera else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = label, 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = description,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    if (doc != null) {
                        IconButton(
                            onClick = { viewModel.removeDocument(doc.id!!) },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Button(
                            onClick = { launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tải lên", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = Color(0xFFFDF7F2),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PrimaryPeach.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryPeach, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Lưu ý: Bạn vui lòng cung cấp ảnh rõ nét, không bị lóa hoặc mất góc để quá trình xét duyệt diễn ra nhanh chóng.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = DeepBrown.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun Step5PolicyTermsScreen(formData: OrganizationProfileDto, update: (OrganizationProfileDto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bước 5: Chính sách & Điều khoản", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBrown)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SoftPeach.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Chính sách minh bạch", fontWeight = FontWeight.Bold, color = DeepBrown)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = formData.sterilizationPolicy == true, 
                        onCheckedChange = { update(formData.copy(sterilizationPolicy = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPeach)
                    )
                    Text("Cam kết triệt sản trước khi nhận nuôi", color = TextGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = formData.vaccinationPolicy == true, 
                        onCheckedChange = { update(formData.copy(vaccinationPolicy = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPeach)
                    )
                    Text("Cam kết tiêm phòng trước khi nhận nuôi", color = TextGray)
                }
            }
        }
        
        StyledTextField(
            value = formData.policyDescription ?: "", 
            onValueChange = { update(formData.copy(policyDescription = it)) },
            label = "Mô tả thêm về chính sách (tùy chọn)",
            singleLine = false,
            modifier = Modifier.height(120.dp)
        )
        
        Surface(
            onClick = { update(formData.copy(agreedTerms = !(formData.agreedTerms ?: false))) },
            color = Color.Transparent
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Checkbox(
                    checked = formData.agreedTerms == true, 
                    onCheckedChange = { update(formData.copy(agreedTerms = it)) },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryPeach)
                )
                Text("Tôi đã đọc và đồng ý với các điều khoản hoạt động của cộng đồng PetMate", color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun Step6ReviewSubmitScreen(formData: OrganizationProfileDto) {
    val orgTypeLabels = mapOf(
        "PRIVATE_RESCUE" to "Tổ chức cứu hộ tư nhân",
        "PUBLIC_SHELTER" to "Trạm cứu hộ công lập",
        "VET_CLINIC" to "Phòng khám thú y",
        "INDEPENDENT_FOSTER" to "Cá nhân nuôi dưỡng"
    )

    val roleLabels = mapOf(
        "OWNER" to "Người sáng lập",
        "MANAGER" to "Quản lý",
        "COORDINATOR" to "Điều phối viên",
        "VOLUNTEER" to "Tình nguyện viên"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bước 6: Xem lại thông tin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBrown)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = InputBackground),
            border = BorderStroke(1.dp, InputBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReviewItem("Tên tổ chức", formData.name.ifBlank { "Chưa nhập" })
                ReviewItem("Loại hình", orgTypeLabels[formData.orgType] ?: (formData.orgType ?: "-"))
                ReviewItem("Địa chỉ", formData.address.ifBlank { "Chưa nhập" })
                
                val repName = formData.representativeName ?: "Chưa nhập"
                val repRole = roleLabels[formData.representativeRole] ?: (formData.representativeRole ?: "-")
                ReviewItem("Người đại diện", "$repName ($repRole)")

                ReviewItem("Triệt sản", if(formData.sterilizationPolicy == true) "Có cam kết" else "Chưa cam kết")
                ReviewItem("Tiêm phòng", if(formData.vaccinationPolicy == true) "Có cam kết" else "Chưa cam kết")
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = SoftPeach.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Vui lòng kiểm tra kỹ thông tin. Bạn có thể nhấn 'Gửi hồ sơ' để hoàn tất đăng ký. Quản trị viên sẽ phản hồi trong vòng 24-72 giờ làm việc qua email và thông báo ứng dụng.", 
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = DeepBrown
            )
        }
    }
}

@Composable
fun ReviewItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = IconGray, fontSize = 14.sp)
        Text(value, color = DeepBrown, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
}

@Composable
fun StyledTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPeach,
            unfocusedBorderColor = InputBorder,
            focusedLabelColor = PrimaryPeach,
            unfocusedLabelColor = IconGray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = InputBackground
        ),
        singleLine = singleLine
    )
}
