package com.fixnow.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fixnow.app.data.local.PendingSolicitudQueue
import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.model.SolicitudServicio
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class SolicitudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()
    private val queue = PendingSolicitudQueue(appContext)

    override suspend fun doWork(): Result {
        val pendingRequests = queue.load()
        if (pendingRequests.isEmpty()) return Result.success()

        var sentCount = 0

        for (solicitud in pendingRequests) {
            try {
                val document = firestore.collection("solicitudes").document(solicitud.id.ifBlank { solicitud.id })
                val payload = hashMapOf<String, Any?>(
                    "id" to document.id,
                    "clienteId" to solicitud.clienteId,
                    "categoriaId" to solicitud.categoriaId,
                    "categoriaNombre" to solicitud.categoriaNombre,
                    "descripcion" to solicitud.descripcion,
                    "latitud" to solicitud.latitud,
                    "longitud" to solicitud.longitud,
                    "direccion" to solicitud.direccion,
                    "fotoBase64" to solicitud.fotoBase64,
                    "urgencia" to solicitud.urgencia,
                    "precioSugerido" to solicitud.precioSugerido,
                    "estado" to EstadoSolicitud.PUBLICADA.name,
                    "tecnicoId" to null,
                    "fechaCreacion" to FieldValue.serverTimestamp()
                )

                document.set(payload).await()
                sentCount += 1
            } catch (error: FirebaseFirestoreException) {
                if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                    error.code == FirebaseFirestoreException.Code.CANCELLED
                ) {
                    return Result.retry()
                }
                return Result.failure()
            } catch (error: Exception) {
                return Result.retry()
            }
        }

        if (sentCount > 0) {
            queue.remove(pendingRequests.map { it.id })
            mostrarNotificacion(
                title = "Solicitudes enviadas",
                body = "Se sincronizaron $sentCount solicitud(es) pendientes."
            )
        }

        return Result.success()
    }

    private fun mostrarNotificacion(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronización de solicitudes",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "solicitudes_sync"
    }
}
