package com.fixnow.app.data.repository

import com.fixnow.app.core.util.safeCall
import com.fixnow.app.data.local.ImageEncoder
import com.fixnow.app.data.mapper.toFriendlyMessage
import com.fixnow.app.domain.model.User
import com.fixnow.app.domain.repository.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [ProfileRepository] con Firestore (HU05 y HU06).
 * Las escrituras usan SetOptions.merge(): si el documento no existe se crea,
 * y si existe solo se cambian los campos indicados.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val imageEncoder: ImageEncoder
) : ProfileRepository {

    private val users = firestore.collection("users")

    override suspend fun getProfile(uid: String): Result<User> =
        safeCall(Throwable::toFriendlyMessage) {
            users.document(uid).get().await().toObject(User::class.java) ?: User(uid = uid)
        }

    override suspend fun createInitialProfile(uid: String, email: String): Result<Unit> =
        safeCall(Throwable::toFriendlyMessage) {
            users.document(uid).set(User(uid = uid, email = email)).await()
            Unit
        }

    override suspend fun updateProfile(
        uid: String,
        nombre: String,
        notificacionesActivas: Boolean
    ): Result<Unit> = safeCall(Throwable::toFriendlyMessage) {
        users.document(uid).set(
            mapOf(
                "nombre" to nombre,
                "notificacionesActivas" to notificacionesActivas
            ),
            SetOptions.merge()
        ).await()
        Unit
    }

    override suspend fun saveProfilePhoto(uid: String, imageUri: String): Result<String> =
        safeCall(Throwable::toFriendlyMessage) {
            // Procesar imágenes es trabajo pesado: se hace fuera del hilo principal.
            val base64 = withContext(Dispatchers.IO) { imageEncoder.toSquareBase64(imageUri) }
            users.document(uid).set(mapOf("fotoBase64" to base64), SetOptions.merge()).await()
            base64
        }

    override suspend fun setBiometricEnabled(uid: String, enabled: Boolean): Result<Unit> =
        safeCall(Throwable::toFriendlyMessage) {
            users.document(uid).set(mapOf("biometriaActivada" to enabled), SetOptions.merge()).await()
            Unit
        }
}
