package com.fixnow.app.domain.usecase.solicitud

import com.fixnow.app.domain.repository.SolicitudRepository
import javax.inject.Inject

class CancelarSolicitudUseCase @Inject constructor(
    private val repository: SolicitudRepository
) {

    suspend operator fun invoke(
        solicitudId: String
    ): Result<Unit> {
        return repository.cancelarSolicitud(solicitudId)
    }
}