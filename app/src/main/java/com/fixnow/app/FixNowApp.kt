package com.fixnow.app

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp


/** @HiltAndroidApp activa Hilt en toda la aplicación (HU01). */
@HiltAndroidApp
class FixNowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        logFcmTokenInDebug()
    }

    /** HU03: en modo debug, imprime el token de FCM en Logcat (filtro: FixNowFCM) para probar notificaciones. */
    private fun logFcmTokenInDebug() {
        if (!BuildConfig.DEBUG) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FixNowFCM", "Token FCM: ${task.result}")
            } else {
                Log.w("FixNowFCM", "No se pudo obtener el token FCM", task.exception)
            }
        }
    }
}
