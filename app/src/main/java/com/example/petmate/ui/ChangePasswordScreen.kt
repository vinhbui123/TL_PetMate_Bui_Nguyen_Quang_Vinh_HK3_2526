package com.example.petmate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
    onPasswordChanged: () -> Unit = {}
) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Kiểm tra xem user có đăng nhập bằng email/password không
    val hasPasswordProvider = remember {
        firebaseUser?.providerData?.any { it.providerId == "password" } == true
    }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChanging by remember { mutableStateOf(false) }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (hasPasswordProvider) "Thay đổi mật khẩu" else "Thiết lập mật khẩu",
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = BackgroundBeige,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Social Account Warning Card
            if (!hasPasswordProvider) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF5C6BC0), // Soft indigo
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val providerName = firebaseUser?.providerData
                            ?.firstOrNull { it.providerId != "firebase" }
                            ?.providerId?.replace(".com", "")
                            ?.replaceFirstChar { it.uppercase() } ?: "mạng xã hội"
                        
                        Text(
                            text = "Bạn đang dùng $providerName. Nhập mật khẩu mới và hệ thống sẽ gửi link xác nhận về email của bạn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (hasPasswordProvider) {
                    PasswordInputField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Mật khẩu hiện tại",
                        isVisible = currentPasswordVisible,
                        onToggleVisibility = { currentPasswordVisible = !currentPasswordVisible }
                    )
                }

                PasswordInputField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Mật khẩu mới",
                    isVisible = newPasswordVisible,
                    onToggleVisibility = { newPasswordVisible = !newPasswordVisible }
                )

                PasswordInputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Xác nhận mật khẩu mới",
                    isVisible = confirmPasswordVisible,
                    onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    when {
                        hasPasswordProvider && currentPassword.isEmpty() -> {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Vui lòng nhập mật khẩu hiện tại") }
                        }
                        newPassword.isEmpty() -> {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Vui lòng nhập mật khẩu mới") }
                        }
                        newPassword.length < 6 -> {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Mật khẩu ít nhất 6 ký tự") }
                        }
                        newPassword != confirmPassword -> {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Mật khẩu xác nhận không khớp") }
                        }
                        hasPasswordProvider && newPassword == currentPassword -> {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Mật khẩu mới phải khác mật khẩu hiện tại") }
                        }
                        else -> {
                            isChanging = true
                            coroutineScope.launch {
                                try {
                                    val user = FirebaseAuth.getInstance().currentUser
                                        ?: throw Exception("Chưa đăng nhập")

                                    if (hasPasswordProvider) {
                                        // User Email/Password: xác thực lại rồi đổi
                                        val email = user.email
                                            ?: throw Exception("Không tìm thấy email")
                                        val credential = EmailAuthProvider.getCredential(email, currentPassword)
                                        user.reauthenticate(credential).await()
                                    }

                                    // Cập nhật mật khẩu trực tiếp trên Firebase
                                    user.updatePassword(newPassword).await()
                                    onPasswordChanged()
                                } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                    snackbarHostState.showSnackbar("Mật khẩu hiện tại không đúng")
                                } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                                    snackbarHostState.showSnackbar("Phiên đăng nhập đã hết hạn. Vui lòng đăng xuất và đăng nhập lại.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Lỗi: ${e.localizedMessage ?: "Không xác định"}")
                                } finally {
                                    isChanging = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeach),
                enabled = !isChanging
            ) {
                if (isChanging) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (hasPasswordProvider) "Cập nhật mật khẩu" else "Thiết lập mật khẩu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPeach,
            unfocusedBorderColor = InputBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
            focusedLabelColor = PrimaryPeach,
            cursorColor = PrimaryPeach
        ),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = IconGray
                )
            }
        }
    )
}
