package com.example.petmate.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
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
import coil.compose.AsyncImage
import com.example.petmate.model.PetRequest
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPetScreen(
    onBackClick: () -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Đực") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("DOGS") }
    var isVaccinated by remember { mutableStateOf(false) }
    var isNeutered by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    var myOrg by remember { mutableStateOf<com.example.petmate.model.OrganizationProfileDto?>(null) }
    var postAsOrg by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = NetworkClient.orgApi.getMyOrg()
            if (response.isSuccessful) {
                myOrg = response.body()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng tin thú cưng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (myOrg != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Đăng dưới danh nghĩa Tổ chức", fontWeight = FontWeight.Bold)
                            Text(myOrg?.name ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = postAsOrg, onCheckedChange = { postAsOrg = it })
                    }
                }
            }
            
            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tải ảnh lên", color = Color.Gray)
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên thú cưng") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Giống") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Giá (Để trống nếu Miễn phí)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Tuổi") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Cân nặng") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Mô tả chi tiết") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            
            // Health statuses
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isVaccinated, onCheckedChange = { isVaccinated = it })
                Text("Đã tiêm phòng")
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(checked = isNeutered, onCheckedChange = { isNeutered = it })
                Text("Đã triệt sản")
            }
            
            // Category Selection
            Text("Danh mục", fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "DOGS" to "🐶 Chó",
                    "CATS" to "🐱 Mèo",
                    "BIRDS" to "🦜 Chim cảnh",
                    "FISH" to "🐟 Cá cảnh",
                    "HAMSTERS" to "🐹 Hamster",
                    "RABBITS" to "🐰 Thỏ",
                    "POULTRY" to "🐔 Gia cầm",
                    "OTHER" to "🦎 Khác"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = category == code,
                        onClick = { category = code },
                        label = { Text(label) }
                    )
                }
            }

            // Gender Selection
            Text("Giới tính", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Đực", "Cái").forEach { g ->
                    FilterChip(
                        selected = gender == g,
                        onClick = { gender = g },
                        label = { Text(g) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val dto = PetRequest(
                                name = name, breed = breed, age = age, weight = weight,
                                gender = when (gender) {
                                    "Cái" -> "FEMALE"
                                    "Không rõ" -> "UNKNOWN"
                                    else -> "MALE"
                                }, description = description,
                                price = if (price.isBlank()) "Miễn phí" else price,
                                category = category, status = "AVAILABLE",
                                isVaccinated = isVaccinated,
                                isNeutered = isNeutered,
                                organizationId = if (postAsOrg) myOrg?.id else null
                            )
                            val createdPet = NetworkClient.apiService.createPet(dto)
                            
                            // Upload image if provided
                            if (imageUri != null) {
                                val inputStream = context.contentResolver.openInputStream(imageUri!!)
                                val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
                                val outputStream = FileOutputStream(tempFile)
                                inputStream?.copyTo(outputStream)
                                inputStream?.close()
                                outputStream.close()
                                
                                val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                                NetworkClient.apiService.uploadPetImage(createdPet.id, body)
                            }
                            
                            Toast.makeText(context, "Đăng tin thành công! Tin của bạn đang chờ Admin duyệt.", Toast.LENGTH_LONG).show()
                            onPostSuccess()
                        } catch (e: retrofit2.HttpException) {
                            e.printStackTrace()
                            val errorBody = e.response()?.errorBody()?.string()
                            val errorMessage = try {
                                org.json.JSONObject(errorBody ?: "").getString("message")
                            } catch (ex: Exception) {
                                e.message()
                            }
                            Toast.makeText(context, "Lỗi: $errorMessage", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = com.example.petmate.ui.theme.PrimaryPeach)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("ĐĂNG TIN", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
