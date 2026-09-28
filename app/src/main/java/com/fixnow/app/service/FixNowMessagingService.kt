package com.fixnow.app.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * HU03: deja listo Firebase Cloud Messaging.
 * En HU08 (técnico recibe solicitudes cercanas) aquí se guardará el token en Firestore
 * y se mostrarán las notificaciones con la app abierta.
 */
class FixNowMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Nuevo token FCM: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Mensaje recibido: ${message.notification?.title}")
    }

    private companion object {
        const val TAG = "FixNowFCM"
    }
}
