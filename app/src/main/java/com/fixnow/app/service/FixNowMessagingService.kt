package com.fixnow.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fixnow.app.presentation.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe avisos de nuevas solicitudes cercanas y registra el token del dispositivo.
 */
class FixNowMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Nuevo token FCM: $token")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { profile ->
                if (profile.getString("rol") == "tecnico") guardarToken(uid, token)
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { profile ->
                if (profile.getString("rol") != "tecnico") return@addOnSuccessListener
                val title = message.data["title"] ?: message.notification?.title ?: "FixNow"
                val body = message.data["body"] ?: message.notification?.body.orEmpty()
                Log.d(TAG, "Mensaje recibido: $title")
                mostrarNotificacion(title, body, message.data["requestId"])
            }
    }

    private fun guardarToken(uid: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("fcmTokens").document(token)
            .set(mapOf("token" to token, "actualizadoEn" to FieldValue.serverTimestamp()))
    }

    private fun mostrarNotificacion(title: String, body: String, requestId: String?) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Solicitudes cercanas", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("solicitudId", requestId)
            putExtra("openTechnicianInbox", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(this).notify(requestId?.hashCode() ?: 1, notification)
    }

    private companion object {
        const val TAG = "FixNowFCM"
        const val CHANNEL_ID = "solicitudes_cercanas"
    }
}
