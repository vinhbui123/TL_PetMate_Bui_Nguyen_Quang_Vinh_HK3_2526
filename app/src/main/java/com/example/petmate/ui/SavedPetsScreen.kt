package com.example.petmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petmate.model.Pet
import com.example.petmate.model.User
import com.example.petmate.network.NetworkClient.apiService
import com.example.petmate.ui.theme.BackgroundBeige
import com.example.petmate.ui.theme.PrimaryPeach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPetsScreen(
    onBackClick: () -> Unit,
    onPetClick: (Pet) -> Unit,
    currentUser: User?,
    userLatitude: Double?,
    userLongitude: Double?
) {
    var savedPets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val pets = apiService.getSavedPets()
            savedPets = pets
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin đã lưu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = BackgroundBeige
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryPeach
                )
            } else if (savedPets.isEmpty()) {
                Text(
                    text = "Bạn chưa lưu tin nào.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                MarketGrid(
                    items = savedPets,
                    onItemClick = onPetClick,
                    currentUser = currentUser,
                    userLatitude = userLatitude,
                    userLongitude = userLongitude
                )
            }
        }
    }
}
