package com.example.petmate.util

import android.content.Context
import android.content.SharedPreferences
import com.example.petmate.ui.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ChatbotStorage {
    private const val PREFS_NAME = "chatbot_prefs"
    private const val KEY_MESSAGES = "chat_messages"

    fun saveMessages(context: Context, messages: List<ChatMessage>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(messages)
        editor.putString(KEY_MESSAGES, json)
        editor.apply()
    }

    fun getMessages(context: Context): List<ChatMessage> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_MESSAGES, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<ChatMessage>>() {}.type
                return gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Trả về câu chào mặc định nếu chưa có tin nhắn nào được lưu
        return listOf(
            ChatMessage("Xin chào! Tôi là trợ lý AI chuyên về thú y và chăm sóc vật nuôi. Tôi có thể giúp gì cho bạn hôm nay?", false)
        )
    }
}
