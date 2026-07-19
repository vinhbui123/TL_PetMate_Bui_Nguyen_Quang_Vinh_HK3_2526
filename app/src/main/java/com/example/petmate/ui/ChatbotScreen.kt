package com.example.petmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.petmate.model.ChatbotRequest
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.petmate.util.ChatbotStorage

data class ChatMessage(val content: String, val isMine: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<ChatMessage>>(ChatbotStorage.getMessages(context)) }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PetMate AI Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatbotMessageBubble(msg)
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                shadowElevation = 1.dp
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(12.dp).size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
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
                    placeholder = { Text("Nhắn tin cho AI...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    enabled = !isLoading
                )
                
                IconButton(
                    onClick = {
                        val content = textState.text.trim()
                        if (content.isNotEmpty() && !isLoading) {
                            messages = messages + ChatMessage(content, true)
                            ChatbotStorage.saveMessages(context, messages)
                            textState = TextFieldValue("")
                            isLoading = true
                            
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size)
                                try {
                                    val response = NetworkClient.apiService.askChatbot(ChatbotRequest(content))
                                    messages = messages + ChatMessage(response.reply, false)
                                    ChatbotStorage.saveMessages(context, messages)
                                } catch (e: Exception) {
                                    messages = messages + ChatMessage("Xin lỗi, tôi không thể kết nối tới máy chủ lúc này.", false)
                                    ChatbotStorage.saveMessages(context, messages)
                                } finally {
                                    isLoading = false
                                    listState.animateScrollToItem(messages.size)
                                }
                            }
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(24.dp)),
                    enabled = !isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatbotMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isMine) MaterialTheme.colorScheme.primary else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMine) 16.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isMine) Color.White else Color.Black
            )
        }
    }
}
