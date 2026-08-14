package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.petmate.model.AdoptionRequest
import com.example.petmate.model.Pet
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionFormScreen(
    pet: Pet,
    onBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3
    
    // Form States
    var message by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var houseType by remember { mutableStateOf("Căn hộ") }
    var hasFencedYard by remember { mutableStateOf<Boolean?>(null) }
    var hasOtherPets by remember { mutableStateOf<Boolean?>(null) }
    var otherPetsDetails by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đơn xin nhận nuôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundBeige
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress Indicator
            FormProgressBar(currentStep = currentStep, totalSteps = totalSteps)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Pet Summary Card
                PetSummaryMiniCard(pet)

                when (currentStep) {
                    1 -> StepContactAndJob(
                        phone = phone,
                        onPhoneChange = { phone = it },
                        job = job,
                        onJobChange = { job = it }
                    )
                    2 -> StepLivingCondition(
                        houseType = houseType,
                        onHouseTypeChange = { houseType = it },
                        hasFencedYard = hasFencedYard,
                        onFencedYardChange = { hasFencedYard = it },
                        hasOtherPets = hasOtherPets,
                        onOtherPetsChange = { hasOtherPets = it },
                        otherPetsDetails = otherPetsDetails,
                        onOtherPetsDetailsChange = { otherPetsDetails = it }
                    )
                    3 -> StepMotivation(
                        message = message,
                        onMessageChange = { message = it },
                        experience = experience,
                        onExperienceChange = { experience = it }
                    )
                }
            }

            // Bottom Navigation Buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border =BorderStroke(1.dp, PrimaryPeach)
                        ) {
                            Text("Quay lại", color = PrimaryPeach)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                // Validation for steps
                                val isValid = when(currentStep) {
                                    1 -> phone.isNotBlank() && job.isNotBlank()
                                    2 -> hasFencedYard != null && hasOtherPets != null
                                    else -> true
                                }
                                if (isValid) currentStep++ 
                                else Toast.makeText(context, "Vui lòng điền đủ thông tin", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                // Final Submit
                                if (message.isBlank() || experience.isBlank()) {
                                    Toast.makeText(context, "Vui lòng điền đủ thông tin", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            NetworkClient.apiService.applyForAdoption(
                                                AdoptionRequest(
                                                    petId = pet.id.toLong(),
                                                    message = message,
                                                    experience = experience,
                                                    phone = phone,
                                                    houseType = houseType,
                                                    hasFencedYard = hasFencedYard,
                                                    hasOtherPets = hasOtherPets,
                                                    otherPetsDetails = otherPetsDetails,
                                                    job = job
                                                )
                                            )
                                            Toast.makeText(context, "Đơn của bạn đã được gửi thành công!", android.widget.Toast.LENGTH_LONG).show()
                                            onSubmitSuccess()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Lỗi: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text(if (currentStep == totalSteps) "Gửi đơn đăng ký" else "Tiếp theo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isActive = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (isActive) PrimaryPeach else Color(0xFFEEEEEE))
            )
        }
    }
}

@Composable
fun PetSummaryMiniCard(pet: Pet) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Không có ảnh",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Đăng ký nhận nuôi", style = MaterialTheme.typography.labelMedium, color = IconGray)
                Text(pet.name ?: "Chưa có tên", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextGray)
            }
        }
    }
}

@Composable
fun StepContactAndJob(
    phone: String,
    onPhoneChange: (String) -> Unit,
    job: String,
    onJobChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Thông tin cơ bản", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Số điện thoại liên hệ") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryPeach) }
        )

        OutlinedTextField(
            value = job,
            onValueChange = onJobChange,
            label = { Text("Nghề nghiệp hiện tại") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = PrimaryPeach) }
        )
    }
}

@Composable
fun StepLivingCondition(
    houseType: String,
    onHouseTypeChange: (String) -> Unit,
    hasFencedYard: Boolean?,
    onFencedYardChange: (Boolean) -> Unit,
    hasOtherPets: Boolean?,
    onOtherPetsChange: (Boolean) -> Unit,
    otherPetsDetails: String,
    onOtherPetsDetailsChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Điều kiện sống", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Column {
            Text("Loại hình nhà ở", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val options = listOf("Căn hộ", "Nhà phố", "Biệt thự", "Khác")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = houseType == option
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onHouseTypeChange(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryPeach.copy(alpha = 0.1f) else Color.White,
                        border = BorderStroke(
                            1.dp, if (isSelected) PrimaryPeach else Color(0xFFDDDDDD)
                        )
                    ) {
                        Text(
                            option,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) PrimaryPeach else TextGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        YesNoQuestion(
            question = "Nhà bạn có sân vườn/hàng rào không?",
            selected = hasFencedYard,
            onSelected = onFencedYardChange
        )

        YesNoQuestion(
            question = "Bạn hiện có nuôi thú cưng khác không?",
            selected = hasOtherPets,
            onSelected = onOtherPetsChange
        )

        if (hasOtherPets == true) {
            OutlinedTextField(
                value = otherPetsDetails,
                onValueChange = onOtherPetsDetailsChange,
                label = { Text("Chi tiết về thú cưng hiện tại") },
                placeholder = { Text("Ví dụ: 1 chú chó Poodle 2 tuổi...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun StepMotivation(
    message: String,
    onMessageChange: (String) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tại sao bạn chọn bé?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Lời giới thiệu & Lý do nhận nuôi") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPeach)
        )

        OutlinedTextField(
            value = experience,
            onValueChange = onExperienceChange,
            label = { Text("Kinh nghiệm chăm sóc thú cưng") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPeach)
        )
    }
}

@Composable
fun YesNoQuestion(question: String, selected: Boolean?, onSelected: (Boolean) -> Unit) {
    Column {
        Text(question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(true to "Có", false to "Không").forEach { (value, label) ->
                val isSelected = selected == value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSelected(value) }
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelected(value) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryPeach)
                    )
                    Text(label, color = TextGray)
                }
            }
        }
    }
}
