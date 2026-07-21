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
fun EditPetScreen(
    pet: com.example.petmate.model.Pet,
    onBackClick: () -> Unit,
    onEditSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(pet.name) }
    var breed by remember { mutableStateOf(pet.breed ?: "") }
    var age by remember { mutableStateOf(pet.age ?: "") }
    var weight by remember { mutableStateOf(pet.weight ?: "") }
    var gender by remember { mutableStateOf(pet.sex ?: "Đực") }
    var description by remember { mutableStateOf(pet.about ?: "") }
    var price by remember { mutableStateOf(if (pet.price == "Miễn phí") "" else pet.price ?: "") }
    var category by remember { mutableStateOf(pet.category ?: "DOGS") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa thú cưng", fontWeight = FontWeight.Bold) },
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
                if (imageUri != null || !pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUri ?: pet.imageUrl,
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
                    if (name.isBlank() || breed.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên và giống!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val dto = PetRequest(
                                name = name, breed = breed, age = age, weight = weight,
                                gender = gender, description = description,
                                price = if (price.isBlank()) "Miễn phí" else price,
                                category = category,
                                status = pet.status ?: "AVAILABLE"
                            )
                            val updatedPet = NetworkClient.apiService.updatePet(pet.id, dto)
                            
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
                                NetworkClient.apiService.uploadPetImage(updatedPet.id, body)
                            }
                            
                            Toast.makeText(context, "Lưu thay đổi thành công!", Toast.LENGTH_LONG).show()
                            onEditSuccess()
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
                    Text("LƯU THAY ĐỔI", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
