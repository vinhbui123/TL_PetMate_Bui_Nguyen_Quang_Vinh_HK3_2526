package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.model.RedListRequest
import com.example.petmate.model.RedListSpecies
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRedListManagementScreen(onBack: () -> Unit, onPetClick: (Pet) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Danh sách đỏ", "Tin chờ duyệt")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Quản lý Danh sách đỏ", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Quay lại", 
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF6366F1),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF6366F1)
                    )
                },
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (selectedTab == index) Color(0xFF6366F1) else Color(0xFF64748B)
                            ) 
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> RedListSpeciesTab()
                1 -> RedListReviewTab(onPetClick = onPetClick)
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
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF6366F1))
            speciesList.isEmpty() -> Text(
                "Chưa có loài nào trong Danh sách đỏ.",
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
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
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Color(0xFF6366F1),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm")
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        species.breedKeyword, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Danh mục: ${species.category ?: "Tất cả"}", fontSize = 13.sp, color = Color(0xFF64748B))
                    
                    val levelColor = if (species.protectionLevel == "PROHIBITED") Color(0xFFEF4444) else Color(0xFFF59E0B)
                    Surface(
                        color = levelColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            formatProtectionLevel(species.protectionLevel),
                            fontSize = 11.sp,
                            color = levelColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFEF4444))
                }
            }
            if (!species.synonyms.isNullOrEmpty()) {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF334155))) { append("Đồng nghĩa: ") }
                        append(species.synonyms)
                    },
                    fontSize = 13.sp, 
                    color = Color(0xFF64748B), 
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (!species.description.isNullOrEmpty()) {
                Text(
                    species.description, 
                    fontSize = 13.sp, 
                    color = Color(0xFF64748B), 
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        title = { Text("Thêm loài vào Danh sách đỏ", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = breedKeyword, 
                    onValueChange = { breedKeyword = it }, 
                    label = { Text("Tên loài / từ khóa *") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )

                Column {
                    Text("Danh mục", fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { (code, label) ->
                            FilterChip(
                                selected = category == code, 
                                onClick = { category = code }, 
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEEF2FF),
                                    selectedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = synonyms,
                    onValueChange = { synonyms = it },
                    label = { Text("Từ đồng nghĩa (phân cách bằng dấu phẩy)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )

                Column {
                    Text("Mức độ bảo vệ", fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        levels.forEach { (code, label) ->
                            FilterChip(
                                selected = protectionLevel == code, 
                                onClick = { protectionLevel = code }, 
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEEF2FF),
                                    selectedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Thêm", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF64748B)) }
        }
    )
}

@Composable
fun RedListReviewTab(onPetClick: (Pet) -> Unit = {}) {
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
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF6366F1))
            pendingPets.isEmpty() -> Text(
                "Không có tin đăng nào cần xem xét Danh sách đỏ.", 
                modifier = Modifier.align(Alignment.Center), 
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingPets) { pet ->
                    RedListPendingPetCard(
                        pet = pet,
                        onClick = { onPetClick(pet) },
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
fun RedListPendingPetCard(pet: Pet, onApprove: () -> Unit, onReject: () -> Unit, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Pets, contentDescription = "Không có ảnh", tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        pet.name ?: "Chưa có tên", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text("Giống: ${pet.breed}", fontSize = 13.sp, color = Color(0xFF64748B))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Cần xem xét Danh sách đỏ",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!pet.redListNote.isNullOrEmpty()) {
                val formattedNote = pet.redListNote
                    .replace("EXACT", "CHÍNH XÁC")
                    .replace("PARTIAL", "MỘT PHẦN")
                
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                ) {
                    Text(
                        formattedNote,
                        fontSize = 12.sp,
                        color = Color(0xFF991B1B),
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Duyệt", fontWeight = FontWeight.Bold, fontSize = 13.sp) 
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Từ chối", fontWeight = FontWeight.Bold, fontSize = 13.sp) 
                }
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
