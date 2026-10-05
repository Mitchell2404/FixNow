package com.fixnow.app.domain.usecase.solicitud

import com.fixnow.app.core.util.safeCall
import com.fixnow.app.domain.model.CategoriaServicio
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.model.UrgenciaServicio
import com.fixnow.app.domain.repository.SolicitudRepository
import javax.inject.Inject

class CrearSolicitudUseCase @Inject constructor(
    private val repository: SolicitudRepository,
    private val calcularPrecio: CalcularPrecioSugeridoUseCase
) {

    suspend operator fun invoke(
        solicitud: SolicitudServicio
    ): Result<String> = safeCall {
        val categoria = CategoriaServicio.entries.firstOrNull {
            it.name == solicitud.categoriaId
        } ?: error("Selecciona una categoría.")

        val urgencia = UrgenciaServicio.entries.firstOrNull {
            it.name == solicitud.urgencia
        } ?: error("Selecciona una urgencia válida.")

        val descripcion = solicitud.descripcion.trim()
        val direccion = solicitud.direccion.trim()

        require(descripcion.isNotEmpty()) {
            "Describe el problema del equipo."
        }

        require(descripcion.length <= 2000) {
            "La descripción no debe superar los 2000 caracteres."
        }

        require(direccion.isNotEmpty()) {
            "Ingresa la dirección del servicio."
        }

        require(
            solicitud.latitud.isFinite() &&
                    solicitud.longitud.isFinite() &&
                    solicitud.latitud in -90.0..90.0 &&
                    solicitud.longitud in -180.0..180.0
        ) {
            "La ubicación no es válida."
        }

        require(solicitud.fotoBase64.length <= 180_000) {
            "La fotografía es demasiado pesada."
        }

        val solicitudValidada = solicitud.copy(
            categoriaNombre = categoria.titulo,
            descripcion = descripcion,
            direccion = direccion,
            precioSugerido = calcularPrecio(categoria, urgencia)
        )

        repository.crearSolicitud(solicitudValidada).getOrThrow()
    }
}