package com.fixnow.app.domain.usecase.solicitud

import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.repository.SolicitudRepository
import javax.inject.Inject

class ObtenerMisSolicitudesUseCase @Inject constructor(
    private val repository: SolicitudRepository
) {
    suspend operator fun invoke(): Result<List<SolicitudServicio>> {
        return repository.obtenerMisSolicitudes()
    }
}