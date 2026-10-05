package com.fixnow.app.domain.model

/**
 * Categorías y tarifas.
 */
enum class CategoriaServicio(
    val titulo: String,
    val tarifaBase: Double
) {
    DIAGNOSTICO(
        titulo = "Diagnóstico de computadora",
        tarifaBase = 30.0
    ),
    MANTENIMIENTO(
        titulo = "Limpieza y mantenimiento",
        tarifaBase = 60.0
    ),
    HARDWARE(
        titulo = "Reparación o instalación de componentes",
        tarifaBase = 50.0
    ),
    SOFTWARE(
        titulo = "Instalación y configuración de programas",
        tarifaBase = 40.0
    ),
    SISTEMA_OPERATIVO(
        titulo = "Instalación o reparación del sistema operativo",
        tarifaBase = 70.0
    ),
    SEGURIDAD(
        titulo = "Revisión de virus y problemas de seguridad",
        tarifaBase = 50.0
    )
}