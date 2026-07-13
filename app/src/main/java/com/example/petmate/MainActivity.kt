package com.example.petmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.petmate.model.Pet
import com.example.petmate.ui.PetDetailsScreen
import com.example.petmate.ui.PetDiscoveryScreen
import com.example.petmate.ui.theme.PetMateTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.example.petmate.model.PetMarketItem
import com.example.petmate.ui.PetMarketScreen
import com.example.petmate.ui.auth.ForgotPasswordScreen
import com.example.petmate.ui.auth.LoginScreen
import com.example.petmate.ui.auth.RegisterScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Navigation screen enum for managing auth and main app flow.
 */
enum class AppScreen {
    Login,
    Register,
    ForgotPassword,
    Main
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PetMateApp()
                }
            }
        }
    }
}

@Composable
fun PetMateApp() {
    val auth = remember { FirebaseAuth.getInstance() }

    // Check if user is already logged in
    var currentScreen by remember {
        mutableStateOf(
            if (auth.currentUser != null) AppScreen.Main else AppScreen.Login
        )
    }

    // No extra states needed for auth flows anymore

    // Handle back navigation for auth screens
    BackHandler(enabled = currentScreen != AppScreen.Main && currentScreen != AppScreen.Login) {
        currentScreen = when (currentScreen) {
            AppScreen.Register -> AppScreen.Login
            AppScreen.ForgotPassword -> AppScreen.Login
            else -> AppScreen.Login
        }
    }

    when (currentScreen) {
        AppScreen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = AppScreen.Main
                },
                onNavigateToRegister = {
                    currentScreen = AppScreen.Register
                },
                onNavigateToForgotPassword = {
                    currentScreen = AppScreen.ForgotPassword
                }
            )
        }

        AppScreen.Register -> {
            RegisterScreen(
                onRegisterSuccess = {
                    currentScreen = AppScreen.Login
                },
                onNavigateToLogin = {
                    currentScreen = AppScreen.Login
                }
            )
        }

        AppScreen.ForgotPassword -> {
            ForgotPasswordScreen(
                onResetEmailSent = {
                    currentScreen = AppScreen.Login
                },
                onNavigateBack = {
                    currentScreen = AppScreen.Login
                }
            )
        }

        AppScreen.Main -> {
            MainContent(
                onLogout = {
                    auth.signOut()
                    currentScreen = AppScreen.Login
                }
            )
        }
    }
}

@Composable
fun MainContent(onLogout: () -> Unit = {}) {
    var userRole by remember { mutableStateOf<String?>(null) }
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var currentUser by remember { 
        mutableStateOf<com.example.petmate.model.User?>(
            if (firebaseUser != null) {
                com.example.petmate.model.User(
                    id = 0L,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: "Người dùng",
                    role = "MEMBER",
                    avatarUrl = firebaseUser.photoUrl?.toString()
                )
            } else null
        ) 
    }
    var currentPet by remember { mutableStateOf<Pet?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        try {
            val fbUser = FirebaseAuth.getInstance().currentUser
            var email = fbUser?.email
            var displayName = fbUser?.displayName
            var avatarUrl = fbUser?.photoUrl?.toString()

            // Nếu email hoặc avatar null (trường hợp Facebook login),
            // lấy thông tin từ Facebook SDK
            val fbAccessToken = com.facebook.AccessToken.getCurrentAccessToken()
            if (fbAccessToken != null && !fbAccessToken.isExpired) {
                // Avatar: dùng URL trực tiếp từ Facebook (luôn hoạt động)
                if (avatarUrl.isNullOrEmpty()) {
                    val fbUserId = fbAccessToken.userId
                    avatarUrl = "https://graph.facebook.com/$fbUserId/picture?type=large&access_token=${fbAccessToken.token}"
                }

                // Email & Name: lấy từ Graph API
                if (email.isNullOrEmpty() || displayName.isNullOrEmpty()) {
                    try {
                        val result = kotlinx.coroutines.suspendCancellableCoroutine<Pair<String?, String?>> { cont ->
                            val request = com.facebook.GraphRequest.newMeRequest(fbAccessToken) { jsonObject, _ ->
                                val e = jsonObject?.optString("email", null)
                                val n = jsonObject?.optString("name", null)
                                cont.resume(Pair(e, n), null)
                            }
                            val params = android.os.Bundle()
                            params.putString("fields", "email,name")
                            request.parameters = params
                            request.executeAsync()
                        }
                        if (!result.first.isNullOrEmpty() && email.isNullOrEmpty()) email = result.first
                        if (!result.second.isNullOrEmpty() && displayName.isNullOrEmpty()) displayName = result.second
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val syncBody = mapOf(
                "email" to email,
                "fullName" to displayName,
                "avatarUrl" to avatarUrl
            )
            val user = com.example.petmate.network.RetrofitClient.apiService.syncUser(syncBody)
            if (user.status == "NOT ACTIVED" || user.status == "BANNED") {
                android.widget.Toast.makeText(context, "Tài khoản của bạn đã bị khoá!", android.widget.Toast.LENGTH_LONG).show()
                onLogout()
                return@LaunchedEffect
            }
            userRole = user.role
            currentUser = user
        } catch (e: Exception) {
            e.printStackTrace()
            userRole = "MEMBER" // Fallback in case of error
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showPostPetScreen by remember { mutableStateOf(false) }

    if (showPostPetScreen) {
        BackHandler {
            showPostPetScreen = false
            refreshTrigger++
        }
        com.example.petmate.ui.PostPetScreen(
            onBackClick = { showPostPetScreen = false },
            onPostSuccess = {
                showPostPetScreen = false
                refreshTrigger++
            }
        )
    } else if (showProfileScreen) {
        BackHandler {
            showProfileScreen = false
            refreshTrigger++
        }
        com.example.petmate.ui.ProfileScreen(
            onBackClick = {
                showProfileScreen = false
                refreshTrigger++
            }
        )
    } else if (currentPet != null) {
        BackHandler {
            currentPet = null
        }
        PetDetailsScreen(
            pet = currentPet!!,
            onBackClick = {
                currentPet = null
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    if (userRole == "RESCUE_ORG") {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Kho thú cưng") },
                            label = { Text("Kho thú cưng") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Duyệt đơn") },
                            label = { Text("Duyệt đơn") }
                        )
                    } else if (userRole == "ADMIN") {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "QL Người dùng") },
                            label = { Text("Người dùng") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Duyệt bài") },
                            label = { Text("Duyệt bài") }
                        )
                    } else {
                        // Default is MEMBER
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Nhận nuôi") },
                            label = { Text("Nhận nuôi") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Mua bán") },
                            label = { Text("Mua bán") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (selectedTab == 0) {
                    PetDiscoveryScreen(
                        currentUser = currentUser,
                        onPetClick = { pet ->
                            currentPet = pet
                        },
                        onLogoutClick = onLogout,
                        onProfileClick = { showProfileScreen = true },
                        onNavigateToPostAd = { showPostPetScreen = true }
                    )
                } else {
                    PetMarketScreen(
                        onItemClick = { item ->
                            currentPet = item
                        },
                        onNavigateToPostAd = { showPostPetScreen = true }
                    )
                }
            }
        }
    }
}
