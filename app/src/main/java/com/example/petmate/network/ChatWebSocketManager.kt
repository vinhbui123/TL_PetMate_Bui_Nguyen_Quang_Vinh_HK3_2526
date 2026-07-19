package com.example.petmate.network

import android.util.Log
import com.example.petmate.model.ChatMessagePayload
import com.example.petmate.model.Message
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

object ChatWebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 10)
    val incomingMessages = _incomingMessages.asSharedFlow()

    fun connect(userId: Long) {
        if (webSocket != null) return

        val request = Request.Builder()
            .url("ws://${NetworkClient.SERVER_IP}:${NetworkClient.SERVER_PORT}/ws/chat?userId=$userId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatWebSocket", "Connected to WebSocket")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ChatWebSocket", "Received message: $text")
                try {
                    val message = gson.fromJson(text, Message::class.java)
                    _incomingMessages.tryEmit(message)
                } catch (e: Exception) {
                    Log.e("ChatWebSocket", "Error parsing message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatWebSocket", "Closed: $reason")
                ChatWebSocketManager.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatWebSocket", "Failure", t)
                ChatWebSocketManager.webSocket = null
            }
        })
    }

    fun sendMessage(payload: ChatMessagePayload) {
        val json = gson.toJson(payload)
        Log.d("ChatWebSocket", "Sending message: $json")
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
