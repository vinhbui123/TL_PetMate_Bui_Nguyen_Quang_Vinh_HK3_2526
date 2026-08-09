package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.model.RedListRequest
import com.example.petmate.model.RedListSpecies
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.PrimaryPeach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRedListManagementScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Danh sách đỏ", "Tin chờ duyệt")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Danh sách đỏ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryPeach
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> RedListSpeciesTab()
                1 -> RedListReviewTab()
            }
        }
    }
}

@Composable
fun RedListSpeciesTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var speciesList by remember { mutableStateOf<List<RedListSpecies>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadSpecies(context) { speciesList = it; isLoading = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPeach)
            speciesList.isEmpty() -> Text(
                "Chưa có loài nào trong Danh sách đỏ.",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(speciesList) { species ->
                    RedListSpeciesCard(
                        species = species,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    NetworkClient.apiService.removeRedListSpecies(species.id)
                                    loadSpecies(context) { speciesList = it }
                                    Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = PrimaryPeach
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }

    if (showAddDialog) {
        AddRedListSpeciesDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { request ->
                coroutineScope.launch {
                    try {
                        NetworkClient.apiService.addRedListSpecies(request)
                        loadSpecies(context) { speciesList = it }
                        showAddDialog = false
                        Toast.makeText(context, "Đã thêm loài", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun RedListSpeciesCard(species: RedListSpecies, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(species.breedKeyword, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Danh mục: ${species.category ?: "Tất cả"}", fontSize = 14.sp, color = Color.Gray)
                    Text("Mức độ: ${formatProtectionLevel(species.protectionLevel)}",
                        fontSize = 14.sp,
                        color = if (species.protectionLevel == "PROHIBITED") Color.Red else Color(0xFFFFA000),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
                }
            }
            if (!species.synonyms.isNullOrEmpty()) {
                Text("Đồng nghĩa: ${species.synonyms}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
            if (!species.description.isNullOrEmpty()) {
                Text(species.description, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun AddRedListSpeciesDialog(onDismiss: () -> Unit, onAdd: (RedListRequest) -> Unit) {
    var breedKeyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var synonyms by remember { mutableStateOf("") }
    var protectionLevel by remember { mutableStateOf("RESTRICTED") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("" to "Tất cả", "DOGS" to "Chó", "CATS" to "Mèo", "BIRDS" to "Chim cảnh",
        "FISH" to "Cá cảnh", "HAMSTERS" to "Hamster", "RABBITS" to "Thỏ", "POULTRY" to "Gia cầm", "OTHER" to "Khác")
    val levels = listOf("RESTRICTED" to "Hạn chế (Admin duyệt)", "PROHIBITED" to "Cấm giao dịch")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm loài vào Danh sách đỏ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = breedKeyword, onValueChange = { breedKeyword = it }, label = { Text("Tên loài / từ khóa *") }, modifier = Modifier.fillMaxWidth())

                Text("Danh mục", fontSize = 12.sp, color = Color.Gray)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { (code, label) ->
                        FilterChip(selected = category == code, onClick = { category = code }, label = { Text(label) })
                    }
                }

                OutlinedTextField(
                    value = synonyms,
                    onValueChange = { synonyms = it },
                    label = { Text("Từ đồng nghĩa (phân cách bằng dấu phẩy)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mức độ bảo vệ", fontSize = 12.sp, color = Color.Gray)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.forEach { (code, label) ->
                        FilterChip(selected = protectionLevel == code, onClick = { protectionLevel = code }, label = { Text(label) })
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(RedListRequest(
                        category = category.takeIf { it.isNotEmpty() },
                        breedKeyword = breedKeyword.trim(),
                        synonyms = synonyms.takeIf { it.isNotBlank() },
                        protectionLevel = protectionLevel,
                        description = description.takeIf { it.isNotBlank() }
                    ))
                },
                enabled = breedKeyword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach)
            ) { Text("Thêm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        }
    )
}

@Composable
fun RedListReviewTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pendingPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        isLoading = true
        coroutineScope.launch {
            try {
                pendingPets = NetworkClient.apiService.getRedListPendingPets()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPeach)
            pendingPets.isEmpty() -> Text("Không có tin đăng nào cần xem xét Danh sách đỏ.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pendingPets) { pet ->
                    RedListPendingPetCard(
                        pet = pet,
                        onApprove = {
                            coroutineScope.launch {
                                try {
                                    NetworkClient.apiService.approveRedListPet(pet.id)
                                    Toast.makeText(context, "Đã duyệt", Toast.LENGTH_SHORT).show()
                                    load()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onReject = {
                            coroutineScope.launch {
                                try {
                                    NetworkClient.apiService.rejectRedListPet(pet.id)
                                    Toast.makeText(context, "Đã từ chối", Toast.LENGTH_SHORT).show()
                                    load()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RedListPendingPetCard(pet: Pet, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(pet.name ?: "Chưa có tên", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Giống: ${pet.breed}", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        "🔴 Cần xem xét Danh sách đỏ",
                        fontSize = 13.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!pet.redListNote.isNullOrEmpty()) {
                Text(
                    pet.redListNote,
                    fontSize = 12.sp,
                    color = Color(0xFFBF360C),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Duyệt") }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) { Text("Từ chối") }
            }
        }
    }
}

private fun loadSpecies(context: android.content.Context, onResult: (List<RedListSpecies>) -> Unit) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val result = NetworkClient.apiService.getRedList()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(result)
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
            }
        }
    }
}

private fun formatProtectionLevel(level: String): String {
    return when (level) {
        "PROHIBITED" -> "Cấm giao dịch"
        "RESTRICTED" -> "Hạn chế (Admin duyệt)"
        else -> level
    }
}
