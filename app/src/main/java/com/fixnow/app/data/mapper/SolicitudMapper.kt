package com.fixnow.app.data.mapper

import com.fixnow.app.domain.model.SolicitudServicio
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toSolicitudServicio(): SolicitudServicio {
    return SolicitudServicio(
        id = id,
        clienteId = getString("clienteId").orEmpty(),
        categoriaId = getString("categoriaId").orEmpty(),
        categoriaNombre = getString("categoriaNombre").orEmpty(),
        descripcion = getString("descripcion").orEmpty(),
        latitud = getDouble("latitud") ?: 0.0,
        longitud = getDouble("longitud") ?: 0.0,
        direccion = getString("direccion").orEmpty(),
        fotoBase64 = getString("fotoBase64").orEmpty(),
        urgencia = getString("urgencia").orEmpty(),
        precioSugerido = getDouble("precioSugerido") ?: 0.0,
        estado = getString("estado").orEmpty(),
        tecnicoId = getString("tecnicoId"),
        fechaCreacion = getTimestamp("fechaCreacion")
            ?.toDate()
            ?.time
            ?: 0L
    )
}