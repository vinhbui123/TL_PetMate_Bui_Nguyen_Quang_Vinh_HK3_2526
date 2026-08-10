package com.example.petmate.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.model.RedListRequest
import com.example.petmate.model.RedListSpecies
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRedListManagementScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Danh sách đỏ", "Tin chờ duyệt")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Quản lý Danh sách đỏ", 
                        fontWeight = FontWeight.Bold,
                        color = DeepBrown
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = DeepBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBeige)
            )
        },
        containerColor = BackgroundBeige
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundBeige,
                contentColor = AccentOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentOrange
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (selectedTab == index) DeepBrown else TextGray
                            ) 
                        }
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
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = AccentOrange,
            contentColor = Color.White
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
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        fontSize = 18.sp,
                        color = DeepBrown
                    )
                    Text("Danh mục: ${species.category ?: "Tất cả"}", fontSize = 14.sp, color = TextGray)
                    
                    val levelColor = if (species.protectionLevel == "PROHIBITED") ErrorRed else AccentOrange
                    Surface(
                        color = levelColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            formatProtectionLevel(species.protectionLevel),
                            fontSize = 12.sp,
                            color = levelColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = ErrorRed)
                }
            }
            if (!species.synonyms.isNullOrEmpty()) {
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Đồng nghĩa: ") }
                        append(species.synonyms)
                    },
                    fontSize = 13.sp, 
                    color = TextGray, 
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (!species.description.isNullOrEmpty()) {
                Text(
                    species.description, 
                    fontSize = 13.sp, 
                    color = TextGray, 
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 18.sp
                )
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
        title = { Text("Thêm loài vào Danh sách đỏ", color = DeepBrown, fontWeight = FontWeight.Bold) },
        containerColor = CardWhite,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = breedKeyword, 
                    onValueChange = { breedKeyword = it }, 
                    label = { Text("Tên loài / từ khóa *") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedLabelColor = AccentOrange
                    )
                )

                Column {
                    Text("Danh mục", fontSize = 14.sp, color = DeepBrown, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { (code, label) ->
                            FilterChip(
                                selected = category == code, 
                                onClick = { category = code }, 
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentOrange
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
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedLabelColor = AccentOrange
                    )
                )

                Column {
                    Text("Mức độ bảo vệ", fontSize = 14.sp, color = DeepBrown, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        levels.forEach { (code, label) ->
                            FilterChip(
                                selected = protectionLevel == code, 
                                onClick = { protectionLevel = code }, 
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentOrange
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
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedLabelColor = AccentOrange
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Thêm", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = TextGray) }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPeach),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!pet.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = pet.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(IconGray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Text("Không có ảnh", color = TextGray, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        pet.name ?: "Chưa có tên", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        color = DeepBrown
                    )
                    Text("Giống: ${pet.breed}", fontSize = 14.sp, color = TextGray)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Cần xem xét Danh sách đỏ",
                            fontSize = 13.sp,
                            color = ErrorRed,
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
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                ) {
                    Text(
                        formattedNote,
                        fontSize = 13.sp,
                        color = Color(0xFF8D4B38), // A darker brown/red
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duyệt", fontWeight = FontWeight.Bold) 
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Từ chối", fontWeight = FontWeight.Bold) 
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
