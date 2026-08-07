package com.example.petmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.petmate.ui.theme.PetMateTheme
import com.example.petmate.ui.navigation.Screen
import com.example.petmate.ui.navigation.NavGraph
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.petmate.network.NetworkClient
import com.example.petmate.network.ChatWebSocketManager
import com.example.petmate.ui.auth.ForgotPasswordScreen
import com.example.petmate.ui.auth.LoginScreen
import com.example.petmate.ui.auth.RegisterScreen
import com.example.petmate.util.LocationHelper
import com.example.petmate.ui.components.GpsPromptDialog
import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build.VERSION_CODES.TIRAMISU
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.facebook.login.LoginManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

enum class AppScreen {
    Login, Register, ForgotPassword, Main
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetMateTheme {
                PetMateApp()
            }
        }
    }
}

@Composable
fun PetMateApp() {
    var currentScreen by remember {
        mutableStateOf(if (FirebaseAuth.getInstance().currentUser != null) AppScreen.Main else AppScreen.Login)
    }

    when (currentScreen) {
        AppScreen.Login -> LoginScreen(
            onLoginSuccess = { currentScreen = AppScreen.Main },
            onNavigateToRegister = { currentScreen = AppScreen.Register },
            onNavigateToForgotPassword = { currentScreen = AppScreen.ForgotPassword },
            onContinueAsGuest = { currentScreen = AppScreen.Main }
        )
        AppScreen.Register -> RegisterScreen(
            onRegisterSuccess = { currentScreen = AppScreen.Login },
            onNavigateToLogin = { currentScreen = AppScreen.Login }
        )
        AppScreen.ForgotPassword -> ForgotPasswordScreen(
            onResetEmailSent = { currentScreen = AppScreen.Login },
            onNavigateBack = { currentScreen = AppScreen.Login }
        )
        AppScreen.Main -> {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            MainContent(onLogout = { 
                scope.launch {
                    // 1. Sign out from Firebase
                    try {
                        val token = FirebaseMessaging.getInstance().token.await()
                        NetworkClient.apiService.removeFcmToken(token)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    FirebaseAuth.getInstance().signOut()
                    
                    // 2. Disconnect WebSocket
                    ChatWebSocketManager.disconnect()

                    // 3. Modern Google Sign-out using Credential Manager
                    try {
                        val credentialManager = CredentialManager.create(context)
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 4. Also sign out of Facebook if applicable
                    LoginManager.getInstance().logOut()

                    // 5. Navigate to Login screen
                    currentScreen = AppScreen.Login 
                }
            })
        }
    }
}

@Composable
fun MainContent(onLogout: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    var currentUser by remember {
        mutableStateOf(
            firebaseUser?.let {
                com.example.petmate.model.User(
                    id = 0L,
                    providerId = it.uid,
                    email = it.email ?: "",
                    fullName = it.displayName ?: "Người dùng",
                    role = "MEMBER",
                    avatarUrl = it.photoUrl?.toString()
                )
            }
        )
    }

    var userRole by remember { mutableStateOf<String?>(null) }
    var totalUnreadCount by remember { mutableStateOf(0) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var screenStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    var selectedTab by remember { mutableStateOf(0) }
    var userLatitude by remember { mutableStateOf<Double?>(null) }
    var userLongitude by remember { mutableStateOf<Double?>(null) }
    var showGpsPrompt by remember { mutableStateOf(false) }
    var blockedUserIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    val currentScreenState = screenStack.last()
    
    // Bắt Deep Link Firebase Hosting và chuyển vào màn hình Chi tiết
    val activity = context as? android.app.Activity
    val intentData = activity?.intent?.data
    LaunchedEffect(intentData) {
        if (intentData != null && intentData.host == "test-mobile-app-8c2ce.web.app" && intentData.path?.startsWith("/pet/") == true) {
            val petIdStr = intentData.lastPathSegment
            if (petIdStr != null) {
                try {
                    val petId = petIdStr.toInt()
                    val pet = NetworkClient.apiService.getPetById(petId)
                    screenStack = screenStack + Screen.PetDetails(pet)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Không thể tải bài viết từ link", Toast.LENGTH_SHORT).show()
                }
            }
            activity.intent.data = null // Clear để không bị lặp lại khi recompose
        }
    }
    
    val alreadyHasPermission = remember {
        ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    var locationPermissionGranted by remember { mutableStateOf(alreadyHasPermission) }
    var permissionChecked by remember { mutableStateOf(alreadyHasPermission) }

    // Gộp chung xin quyền Vị trí và Thông báo (Android không cho phép mở 2 bảng xin quyền cùng lúc)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.containsKey(ACCESS_FINE_LOCATION) ||
            permissions.containsKey(ACCESS_COARSE_LOCATION)) {
            locationPermissionGranted = permissions[ACCESS_FINE_LOCATION] == true ||
                                        permissions[ACCESS_COARSE_LOCATION] == true
        }
        permissionChecked = true
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        // Xin quyền vị trí nếu chưa có
        if (!alreadyHasPermission) {
            permissionsToRequest.add(ACCESS_FINE_LOCATION)
            permissionsToRequest.add(ACCESS_COARSE_LOCATION)
        }
        
        // Xin quyền thông báo (Android 13+) nếu chưa có
        if (android.os.Build.VERSION.SDK_INT >= TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(POST_NOTIFICATIONS)
            }
        }

        // Mở 1 bảng xin quyền duy nhất cho tất cả
        if (permissionsToRequest.isNotEmpty()) {
            permissionChecked = false // Đợi người dùng trả lời xong mới check vị trí
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            permissionChecked = true
        }
    }

    // Sync User (chạy ngay khi đăng nhập, và mỗi khi refreshTrigger thay đổi)
    LaunchedEffect(firebaseUser, refreshTrigger) {
        if (firebaseUser != null) {
            try {
                // Lấy email: ưu tiên firebaseUser.email, fallback vào providerData (Google)
                val userEmail = firebaseUser.email
                    ?: firebaseUser.providerData
                        .firstOrNull { it.providerId == "google.com" }?.email
                    ?: firebaseUser.providerData
                        .firstOrNull { it.email != null }?.email

                val syncBody = mapOf(
                    "providerId" to firebaseUser.uid,
                    "email" to userEmail,
                    "fullName" to firebaseUser.displayName,
                    "avatarUrl" to firebaseUser.photoUrl?.toString()
                )
                val user = NetworkClient.apiService.syncUser(syncBody)
                currentUser = user
                userRole = user.role

                // Chỉ kết nối WebSocket và đăng ký FCM lần đầu (refreshTrigger == 0)
                if (refreshTrigger == 0) {
                    ChatWebSocketManager.connect(user.id)

                    // Get Blocked Users
                    blockedUserIds = NetworkClient.apiService.getBlockedUsers()

                    // Register FCM Token - lấy token mới nhất và gửi lên Server
                    try {
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        android.util.Log.d("FCM_DEBUG", "New FCM Token: $token")
                        NetworkClient.apiService.registerFcmToken(mapOf("token" to token))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Refresh blocked users khi refresh
                    blockedUserIds = NetworkClient.apiService.getBlockedUsers()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Location (chỉ chạy SAU KHI kết quả xin quyền đã trả về)
    LaunchedEffect(permissionChecked, locationPermissionGranted) {
        if (!permissionChecked) return@LaunchedEffect
        if (firebaseUser == null) return@LaunchedEffect

        if (locationPermissionGranted) {
            try {
                val location = LocationHelper.getCurrentLocation(context)
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    coroutineScope.launch {
                        try {
                            NetworkClient.apiService.updateLocation(mapOf(
                                "latitude" to location.latitude,
                                "longitude" to location.longitude
                            ))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    if (!isGpsEnabled && !isNetworkEnabled) {
                        showGpsPrompt = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "Chưa được cấp quyền Vị trí. Vui lòng bật trong Cài đặt ứng dụng!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Lắng nghe sự kiện Push Notification (FCM) để cập nhật UI tức thời
    LaunchedEffect(Unit) {
        com.example.petmate.util.AppEventBus.refreshEvents.collect {
            refreshTrigger++
        }
    }

    // Lắng nghe tin nhắn WebSocket (Chat) để cập nhật UI tức thời
    LaunchedEffect(Unit) {
        com.example.petmate.network.ChatWebSocketManager.incomingMessages.collect {
            refreshTrigger++
        }
    }

    // Unread count polling
    LaunchedEffect(currentUser, refreshTrigger) {
        currentUser?.let { user ->
            while (true) {
                try {
                    totalUnreadCount = NetworkClient.apiService.getTotalUnreadCount(user.id)
                } catch (e: Exception) {}
                delay(10000.milliseconds) // Polling every 10s is enough for old devices
            }
        }
    }

    if (showGpsPrompt) {
        GpsPromptDialog(
            onDismissRequest = { showGpsPrompt = false },
            onEnableGpsClick = {
                showGpsPrompt = false
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        )
    }

    NavGraph(
        currentScreen = currentScreenState,
        currentUser = currentUser,
        userRole = userRole,
        userLatitude = userLatitude,
        userLongitude = userLongitude,
        blockedUserIds = blockedUserIds,
        totalUnreadCount = totalUnreadCount,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        coroutineScope = coroutineScope,
        onNavigate = { screen -> screenStack = screenStack + screen },
        onNavigateAndClear = { screen -> screenStack = listOf(screen) },
        onPop = { if (screenStack.size > 1) screenStack = screenStack.dropLast(1) },
        onLogout = onLogout,
        onRefresh = { refreshTrigger++ }
    )
}
