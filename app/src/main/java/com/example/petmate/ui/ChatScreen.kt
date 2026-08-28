package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.petmate.model.ChatMessagePayload
import com.example.petmate.model.Message
import com.example.petmate.network.ChatWebSocketManager
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: Long,
    currentUserId: Long,
    otherUserName: String,
    otherUserId: Long,
    otherUserAvatarUrl: String?,
    petId: Long?,
    petName: String?,
    petImageUrl: String?,
    petPrice: String?,
    onBack: () -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    onNavigateToPet: (Long) -> Unit
) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showReportDialogForMessage by remember { mutableStateOf<Long?>(null) }

    if (showReportDialogForMessage != null) {
        com.example.petmate.ui.components.ReportDialog(
            reportedMessageId = showReportDialogForMessage,
            onDismissRequest = { showReportDialogForMessage = null },
            onSuccess = {
                android.widget.Toast.makeText(context, "Cảm ơn bạn đã báo cáo tin nhắn.", android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }

    // 1. Fetch initial messages
    LaunchedEffect(roomId) {
        try {
            messages = NetworkClient.apiService.getChatMessages(roomId)
            if (messages.isNotEmpty()) {
                listState.scrollToItem(messages.size - 1)
            }
            // Mark as read
            NetworkClient.apiService.markRoomAsRead(roomId, currentUserId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. Listen to WebSocket for new messages
    LaunchedEffect(Unit) {
        ChatWebSocketManager.incomingMessages.collect { newMessage ->
            if (newMessage.roomId == roomId) {
                messages = messages.filterNot { it.status == "SENDING" && it.content == newMessage.content } + newMessage
                coroutineScope.launch {
                    kotlinx.coroutines.delay(50.milliseconds)
                    listState.scrollToItem(messages.size - 1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToProfile(otherUserId) }.padding(end = 16.dp)
                    ) {
                        if (!otherUserAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = otherUserAvatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(36.dp).background(Color.Gray.copy(alpha=0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(otherUserName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Pet Banner
            if (petId != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPet(petId) },
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = petImageUrl ?: "https://via.placeholder.com/150",
                            contentDescription = petName,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = petName ?: "Thú cưng",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = petPrice?.let { "$it đ" } ?: "Thỏa thuận",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF7906D)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF5F5F5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(
                                text = "Xem",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    MessageBubble(
                        message = msg, 
                        isMine = isMine,
                        onReportClick = { if (!isMine) showReportDialogForMessage = msg.id }
                    )
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    placeholder = { Text("Nhắn tin...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.example.petmate.ui.theme.PrimaryPeach,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                IconButton(
                    onClick = {
                        val content = textState.text.trim()
                        if (content.isNotEmpty()) {
                            val payload = ChatMessagePayload(
                                type = "CHAT",
                                roomId = roomId,
                                senderId = currentUserId,
                                recipientId = otherUserId,
                                content = content
                            )

                            // Optimistic UI: Hiển thị ngay tin nhắn lên màn hình
                            val tempMsg = Message(
                                id = System.currentTimeMillis(),
                                roomId = roomId,
                                senderId = currentUserId,
                                content = content,
                                createdAt = "",
                                status = "SENDING"
                            )
                            messages = messages + tempMsg
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(50.milliseconds)
                                listState.scrollToItem(messages.size - 1)
                            }

                            // Gửi đi
                            ChatWebSocketManager.sendMessage(payload)
                            textState = TextFieldValue("")
                        }
                    },
                    modifier = Modifier.background(com.example.petmate.ui.theme.PrimaryPeach, shape = RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean, onReportClick: () -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                color = if (isMine) com.example.petmate.ui.theme.PrimaryPeach else Color.White,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 16.dp
                ),
                shadowElevation = 1.dp,
                modifier = Modifier.clickable { if (!isMine) expanded = true }
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isMine) Color.White else Color.Black
                )
            }
            if (!isMine) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Báo cáo tin nhắn", color = Color.Red) },
                        onClick = { 
                            expanded = false
                            onReportClick()
                        }
                    )
                }
            }
        }
    }
}
