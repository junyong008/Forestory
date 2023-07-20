package com.yjy.forestory.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.yjy.forestory.R
import com.yjy.forestory.feature.main.MainActivity
import com.yjy.forestory.feature.viewPost.PostActivity

object NotificationHelper {

    private const val CHANNEL_ID = "CHANNEL_NEW_COMMENT"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = context.getString(R.string.noti_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val descriptionText = context.getString(R.string.noti_channel_description)

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundInfo(context: Context, postId: Int): ForegroundInfo {
        val requestCode = System.currentTimeMillis().toInt() // 매번 알림이 쌓이도록

        val intent1 = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val intent2 = Intent(context, PostActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("postId", postId)
        }

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addNextIntent(intent1)
            addNextIntent(intent2)
        }

        val pendingIntent = stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.delivering_news_to_forest_friends))
            .setContentIntent(pendingIntent)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()

        return ForegroundInfo(requestCode, notification)
    }

    fun sendNewCommentNotification(context: Context, postId: Int, title: String, content: String) {
        val requestCode = System.currentTimeMillis().toInt() // 매번 알림이 쌓이도록

        val intent1 = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val intent2 = Intent(context, PostActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("postId", postId)
        }

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addNextIntent(intent1)
            addNextIntent(intent2)
        }

        val pendingIntent = stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestCode, builder.build())
    }
}