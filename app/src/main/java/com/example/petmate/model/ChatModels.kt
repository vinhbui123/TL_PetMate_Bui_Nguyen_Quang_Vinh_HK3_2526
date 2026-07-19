package com.example.petmate.model

data class ChatRoom(
    val id: Long,
    val otherUser: User,
    val pet: Pet,
    val lastMessage: Message?,
    val unreadCount: Int = 0,
    val updatedAt: String
)

data class Message(
    val id: Long,
    val roomId: Long,
    val senderId: Long,
    val content: String,
    val createdAt: String,
    val status: String
)

data class ChatMessagePayload(
    val type: String = "CHAT",
    val roomId: Long,
    val senderId: Long,
    val recipientId: Long?,
    val content: String,
    val senderName: String? = null,
    val timestamp: String? = null
)
