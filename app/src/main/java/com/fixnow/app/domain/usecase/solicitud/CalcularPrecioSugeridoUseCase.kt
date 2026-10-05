package com.fixnow.app.domain.usecase.solicitud

import com.fixnow.app.domain.model.CategoriaServicio
import com.fixnow.app.domain.model.UrgenciaServicio
import javax.inject.Inject

class CalcularPrecioSugeridoUseCase @Inject constructor() {

    operator fun invoke(
        categoria: CategoriaServicio,
        urgencia: UrgenciaServicio
    ): Double {
        return categoria.tarifaBase + urgencia.recargo
    }
}