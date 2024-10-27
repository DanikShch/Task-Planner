package com.example.taskplanner

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationHelper(private val context: Context) {

    fun createNotificationChannel() {
        // Создание канала уведомлений (только для Android 8.0 и выше)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for task notifications"
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(task: Task) {
        val notificationId = task.hashCode() // Уникальный ID для уведомления
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Установка времени уведомления
        val notificationTime = LocalDateTime.of(task.date, task.time)
        val alarmTime = notificationTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("task_title", task.title)
            putExtra("task_id", notificationId)
        }

        // Добавляем флаг FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Запланировать уведомление
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
    }


    companion object {
        const val CHANNEL_ID = "task_notifications"
    }
}
