package com.example.petmate.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    val type: String = "general",
    val data: Map<String, String>? = null
)

class NotificationStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("petmate_notifications", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val NOTIFICATIONS_KEY = "notifications_list"

    fun saveNotification(title: String, body: String, type: String = "general", data: Map<String, String>? = null) {
        val notifications = getNotifications().toMutableList()
        notifications.add(0, AppNotification(title = title, body = body, type = type, data = data))
        // Keep only last 100 notifications to prevent storage bloat
        val trimmed = if (notifications.size > 100) notifications.take(100) else notifications
        prefs.edit().putString(NOTIFICATIONS_KEY, gson.toJson(trimmed)).apply()
    }

    fun getNotifications(): List<AppNotification> {
        val json = prefs.getString(NOTIFICATIONS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<AppNotification>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun markAsRead(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            prefs.edit().putString(NOTIFICATIONS_KEY, gson.toJson(notifications)).apply()
        }
    }

    fun markAllAsRead() {
        val notifications = getNotifications().map { it.copy(isRead = true) }
        prefs.edit().putString(NOTIFICATIONS_KEY, gson.toJson(notifications)).apply()
    }

    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }
    
    fun clearAll() {
        prefs.edit().remove(NOTIFICATIONS_KEY).apply()
    }
}
