package com.example.petmate.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeHelper {

    /**
     * Format a given date string (e.g. from backend) to relative time (Chợ Tốt style).
     * Supported format: "yyyy-MM-dd'T'HH:mm:ss" or "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
     */
    fun getRelativeTime(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Vừa xong"

        try {
            // Spring Boot usually returns ISO 8601 format like "2024-07-13T18:23:33.123456"
            // We use a regex or multiple formats to handle variations
            val format = when {
                !dateString.contains(".") -> "yyyy-MM-dd'T'HH:mm:ss"
                dateString.length - dateString.indexOf('.') - 1 == 3 -> "yyyy-MM-dd'T'HH:mm:ss.SSS"
                else -> "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
            }
            
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC") // Spring Data typically saves in UTC, adjust if your server uses local time

            val pastDate = sdf.parse(dateString) ?: return "Vừa xong"
            val now = Date()

            val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - pastDate.time)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - pastDate.time)
            val hours = TimeUnit.MILLISECONDS.toHours(now.time - pastDate.time)
            val days = TimeUnit.MILLISECONDS.toDays(now.time - pastDate.time)

            return when {
                seconds < 60 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                days == 1L -> "Hôm qua"
                days < 7 -> "$days ngày trước"
                days < 30 -> "${days / 7} tuần trước"
                days < 365 -> "${days / 30} tháng trước"
                else -> "${days / 365} năm trước"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback in case of parsing error
            return "Gần đây"
        }
    }

    /**
     * Format a given timestamp (in milliseconds) to relative time (Chợ Tốt style).
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) return "Vừa xong"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days == 1L -> "Hôm qua"
            days < 7 -> "$days ngày trước"
            days < 30 -> "${days / 7} tuần trước"
            days < 365 -> "${days / 30} tháng trước"
            else -> "${days / 365} năm trước"
        }
    }
}
