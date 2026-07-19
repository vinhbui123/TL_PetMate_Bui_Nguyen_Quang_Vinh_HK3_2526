package com.example.petmate.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Auth instance removed, handled after OTP
    // Password strength
    val passwordStrength = remember(password) {
        when {
            password.isEmpty() -> 0
            password.length < 8 -> 1
            password.length >= 8 && password.any { it.isUpperCase() } && password.any { !it.isLetterOrDigit() } -> 3
            password.length >= 8 && (password.any { it.isUpperCase() } || password.any { !it.isLetterOrDigit() }) -> 2
            else -> 1
        }
    }

    val passwordStrengthColor = when (passwordStrength) {
        1 -> ErrorRed
        2 -> Color(0xFFFFA726)
        3 -> SuccessGreen
        else -> Color.Transparent
    }

    val passwordStrengthText = when (passwordStrength) {
        1 -> "Yếu"
        2 -> "Trung bình"
        3 -> "Mạnh"
        else -> ""
    }

    // --- Validation ---
    fun validateName(): Boolean {
        nameError = when {
            fullName.isBlank() -> "Vui lòng nhập họ tên"
            fullName.length < 2 -> "Họ tên quá ngắn"
            else -> null
        }
        return nameError == null
    }

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> "Vui lòng nhập email"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không hợp lệ"
            else -> null
        }
        return emailError == null
    }

    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> "Vui lòng nhập mật khẩu"
            password.length < 8 -> "Mật khẩu tối thiểu 8 ký tự"
            !password.any { it.isUpperCase() } -> "Cần ít nhất 1 chữ hoa"
            !password.any { it.isDigit() } -> "Cần ít nhất 1 chữ số"
            !password.any { !it.isLetterOrDigit() } -> "Cần ít nhất 1 ký tự đặc biệt (!@#\$...)"
            else -> null
        }
        return passwordError == null
    }

    fun validateConfirmPassword(): Boolean {
        confirmPasswordError = when {
            confirmPassword.isBlank() -> "Vui lòng xác nhận mật khẩu"
            confirmPassword != password -> "Mật khẩu không khớp"
            else -> null
        }
        return confirmPasswordError == null
    }

    val auth = remember { FirebaseAuth.getInstance() }

    fun handleRegister() {
        val validations = listOf(validateName(), validateEmail(), validatePassword(), validateConfirmPassword())
        if (validations.any { !it }) return

        isLoading = true
        coroutineScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()
                )?.await()
                
                // Send email verification
                result.user?.sendEmailVerification()?.await()
                
                snackbarHostState.showSnackbar("Đăng ký thành công! Vui lòng kiểm tra email để xác thực.")
                kotlinx.coroutines.delay(1500)
                onRegisterSuccess()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Có lỗi xảy ra: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DeepBrown,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = BackgroundBeige,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = DeepBrown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(PrimaryPeach.copy(alpha = 0.25f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-20).dp, y = 20.dp)
                    .clip(CircleShape)
                    .background(AccentOrange.copy(alpha = 0.15f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tạo tài khoản",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrown
                )
                Text(
                    text = "Đăng ký để bắt đầu hành trình cùng PetMate 🐶",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IconGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (nameError != null) validateName()
                    },
                    label = { Text("Họ và tên") },
                    placeholder = { Text("Nguyễn Văn A") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentOrange)
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = ErrorRed) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        cursorColor = AccentOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) validateEmail()
                    },
                    label = { Text("Email") },
                    placeholder = { Text("example@email.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = AccentOrange)
                    },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = ErrorRed) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        cursorColor = AccentOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) validatePassword()
                        if (confirmPassword.isNotEmpty() && confirmPasswordError != null) validateConfirmPassword()
                    },
                    label = { Text("Mật khẩu") },
                    placeholder = { Text("Tối thiểu 8 ký tự, có ký tự đặc biệt") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = IconGray
                            )
                        }
                    },
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = ErrorRed) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        cursorColor = AccentOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Strength Indicator
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index < passwordStrength) passwordStrengthColor
                                        else InputBorder
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = passwordStrengthText,
                            style = MaterialTheme.typography.labelSmall,
                            color = passwordStrengthColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (confirmPasswordError != null) validateConfirmPassword()
                    },
                    label = { Text("Xác nhận mật khẩu") },
                    placeholder = { Text("Nhập lại mật khẩu") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = IconGray
                            )
                        }
                    },
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError?.let { { Text(it, color = ErrorRed) } },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        handleRegister()
                    }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        cursorColor = AccentOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Register Button
                Button(
                    onClick = { handleRegister() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AccentOrange, DarkPeach)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Đăng ký",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Đã có tài khoản? ",
                        color = IconGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Đăng nhập",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToLogin() }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
