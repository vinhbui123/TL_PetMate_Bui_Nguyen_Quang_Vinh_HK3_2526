package com.example.petmate.model

data class ChatbotRequest(
    val message: String
)

data class ChatbotResponse(
    val reply: String
)
