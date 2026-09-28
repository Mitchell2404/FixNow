package com.fixnow.app.data.repository

import com.fixnow.app.core.util.safeCall
import com.fixnow.app.data.mapper.toFriendlyMessage
import com.fixnow.app.domain.model.AuthResult
import com.fixnow.app.domain.model.AuthUser
import com.fixnow.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Implementación de [AuthRepository] con Firebase Authentication (HU04). */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun register(email: String, password: String): AuthResult =
        safeCall(Throwable::toFriendlyMessage) {
            val firebaseUser = firebaseAuth.createUserWithEmailAndPassword(email, password)
                .await().user ?: error("No se pudo crear el usuario")
            // El correo de verificación es "lo mejor posible": si falla, la cuenta igual queda creada.
            safeCall { firebaseUser.sendEmailVerification().await() }
            firebaseUser.toAuthUser()
        }.toAuthResult()

    override suspend fun login(email: String, password: String): AuthResult =
        safeCall(Throwable::toFriendlyMessage) {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .await().user?.toAuthUser() ?: error("No se pudo iniciar sesión")
        }.toAuthResult()

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        safeCall(Throwable::toFriendlyMessage) {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Unit
        }

    override suspend fun resendVerificationEmail(): Result<Unit> =
        safeCall(Throwable::toFriendlyMessage) {
            val user = firebaseAuth.currentUser ?: error("No hay una sesión activa")
            user.sendEmailVerification().await()
            Unit
        }

    override suspend fun refreshCurrentUser(): AuthUser? {
        val user = firebaseAuth.currentUser ?: return null
        // Si falla (por ejemplo, sin internet) se usan los datos que ya tenía el teléfono.
        safeCall { user.reload().await() }
        return firebaseAuth.currentUser?.toAuthUser()
    }

    override fun logout() = firebaseAuth.signOut()

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified
    )

    private fun Result<AuthUser>.toAuthResult(): AuthResult = fold(
        onSuccess = { AuthResult.Success(it) },
        onFailure = { AuthResult.Error(it.message ?: "Ocurrió un error inesperado") }
    )
}
