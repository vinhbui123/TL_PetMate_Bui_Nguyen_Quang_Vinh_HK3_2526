package com.example.petmate.ui.auth

import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.R
import com.example.petmate.ui.theme.*
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val webClientId = stringResource(R.string.default_web_client_id)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }

    // Logo animation removed
    val logoScale = 1f

    // --- Google Sign-In with Credential Manager ---
    suspend fun handleGoogleSignIn() {
        isLoading = true
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Show all accounts for manual trigger
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            
            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    
                    auth.signInWithCredential(firebaseCredential).await()
                    onLoginSuccess()
                } catch (e: GoogleIdTokenParsingException) {
                    snackbarHostState.showSnackbar("Dữ liệu Google không hợp lệ")
                }
            } else {
                snackbarHostState.showSnackbar("Loại xác thực không được hỗ trợ")
            }
        } catch (e: GetCredentialException) {
            if (e.type != "android.credentials.GetCredentialException.TYPE_USER_CANCELED") {
                snackbarHostState.showSnackbar("Đăng nhập Google thất bại: ${e.message}")
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Đã có lỗi xảy ra: ${e.localizedMessage}")
        } finally {
            isLoading = false
        }
    }

    // --- Facebook Sign-In ---
    val callbackManager = remember { CallbackManager.Factory.create() }

    DisposableEffect(Unit) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onCancel() {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Đăng nhập Facebook đã bị hủy")
                }
            }
            override fun onError(error: FacebookException) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Đăng nhập Facebook thất bại: ${error.localizedMessage}")
                }
            }
            override fun onSuccess(result: LoginResult) {
                val token = result.accessToken.token
                val credential = FacebookAuthProvider.getCredential(token)
                isLoading = true
                coroutineScope.launch {
                    try {
                        auth.signInWithCredential(credential).await()
                        onLoginSuccess()
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Đăng nhập Facebook thất bại: ${e.localizedMessage}")
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
        LoginManager.getInstance().registerCallback(callbackManager, callback)
        onDispose {
            LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }

    // Facebook ActivityResult handling is done automatically by passing ComponentActivity and CallbackManager to logIn()

    // --- Validation ---
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
            else -> null
        }
        return passwordError == null
    }

    fun handleLogin() {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        if (!isEmailValid || !isPasswordValid) return

        isLoading = true
        coroutineScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                if (result.user?.isEmailVerified == true) {
                    onLoginSuccess()
                } else {
                    auth.signOut()
                    snackbarHostState.showSnackbar("Vui lòng kiểm tra email và bấm link xác nhận trước khi đăng nhập!")
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.localizedMessage?.contains("no user record", ignoreCase = true) == true ->
                        "Tài khoản không tồn tại"
                    e.localizedMessage?.contains("password is invalid", ignoreCase = true) == true ->
                        "Mật khẩu không đúng"
                    e.localizedMessage?.contains("badly formatted", ignoreCase = true) == true ->
                        "Email không đúng định dạng"
                    else -> "Đăng nhập thất bại: ${e.localizedMessage}"
                }
                snackbarHostState.showSnackbar(errorMsg)
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
        containerColor = BackgroundBeige
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
                    .size(200.dp)
                    .offset(x = (-60).dp, y = (-60).dp)
                    .clip(CircleShape)
                    .background(PrimaryPeach.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = 100.dp)
                    .clip(CircleShape)
                    .background(DarkPeach.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
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
                Spacer(modifier = Modifier.height(60.dp))

                // Logo
                Surface(
                    modifier = Modifier
                        .size((80 * logoScale).dp)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    color = AccentOrange
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = "PetMate Logo",
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PetMate",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrown,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Chào mừng trở lại! 🐾",
                    style = MaterialTheme.typography.bodyLarge,
                    color = IconGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) validatePassword()
                    },
                    label = { Text("Mật khẩu") },
                    placeholder = { Text("Nhập mật khẩu") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
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
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        handleLogin()
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

                // Forgot Password Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Quên mật khẩu?",
                        color = AccentOrange,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToForgotPassword() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button
                Button(
                    onClick = { handleLogin() },
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
                                text = "Đăng nhập",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = InputBorder,
                        thickness = 1.dp
                    )
                    Text(
                        text = "  hoặc tiếp tục với  ",
                        color = IconGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = InputBorder,
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Social Login Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Google Button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                handleGoogleSignIn()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardWhite
                        )
                    ) {
                        Text(
                            text = "G",
                            color = GoogleRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google",
                            color = TextGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Facebook Button
                    OutlinedButton(
                        onClick = {
                            context.findActivity()?.let { activity ->
                                LoginManager.getInstance().logIn(
                                    activity,
                                    callbackManager,
                                    listOf("email", "public_profile")
                                )
                            } ?: coroutineScope.launch {
                                snackbarHostState.showSnackbar("Không thể khởi động đăng nhập Facebook")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardWhite
                        )
                    ) {
                        Text(
                            text = "f",
                            color = FacebookBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Facebook",
                            color = TextGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Register Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Chưa có tài khoản? ",
                        color = IconGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Đăng ký ngay",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToRegister() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Guest Login
                Text(
                    text = "Khám phá ngay (Không cần đăng nhập)",
                    color = TextGray,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onContinueAsGuest() }
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * Helper function to find the Activity from a Context.
 */
private fun Context.findActivity(): androidx.activity.ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is androidx.activity.ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
