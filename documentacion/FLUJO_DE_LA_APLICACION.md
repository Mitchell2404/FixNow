# FixNow: estructura de la aplicación y flujo completo

Esta guía explica cómo está organizada la app, cómo fluye la información entre pantallas, repositorios, Firebase y notificaciones, y qué hace cada bloque importante del código.

## 1. Visión general

FixNow es una app Android hecha con Kotlin + Jetpack Compose + Hilt + Firebase. La lógica está separada por capas para que la UI no dependa directamente de Firebase ni de la base de datos.

La estructura general es esta:

```text
app/
├── src/main/java/com/fixnow/app/
│   ├── presentation/       # Pantallas, ViewModels, navegación
│   ├── domain/              # Modelos, repositorios, casos de uso
│   ├── data/                # Implementación con Firebase y mappers
│   ├── di/                  # Hilt
│   ├── service/             # FCM foreground/background notifications
│   ├── work/                # WorkManager para reintentos y cola offline
│   ├── core/                # Utilidades compartidas
│   ├── FixNowApp.kt         # Inicio de la app
│   └── ...
├── firebase/
│   ├── firestore.rules
│   ├── firestore.indexes.json
│   └── functions/          # backend opcional / Cloud Functions
├── firebase.json
└── app/google-services.json
```

## 2. Arquitectura en capas

La idea es que cada capa solo conozca la de abajo.

```text
presentation
     ↓
domain
     ↓
data
     ↓
Firebase / Firestore / Auth / FCM
```

### Capa `presentation`
Es la parte visual:
- pantallas
- ViewModels
- navegación
- estado de UI

Aquí se decide qué muestra el usuario y qué acciones dispara.

### Capa `domain`
Es la lógica de negocio:
- modelos
- repositorios (interfaces)
- casos de uso

Aquí no hay Firebase, no hay Android, no hay navegadores. Es puro dominio de la app.

### Capa `data`
Es la implementación real:
- FirebaseAuth
- FirebaseFirestore
- FCM
- mappers
- repositorios concretos

### Capa `service` y `work`
Estas capas responden a eventos del sistema:
- notificaciones push
- reintentos de red
- sincronización offline

### Guion breve para explicar la arquitectura
Puedes presentar el recorrido de una acción así:

```text
Compose Screen recibe el clic
    ↓ callback
ViewModel actualiza StateFlow y ejecuta la acción
    ↓
UseCase / contrato Repository del dominio
    ↓ implementación
RepositoryImpl usa Firebase, ubicación o almacenamiento local
    ↓ resultado
ViewModel publica estado/evento y Compose vuelve a dibujar
```

La pantalla describe la interfaz; el `ViewModel` coordina el estado; `domain` expresa modelos/contratos; `data` implementa esos contratos. En algunas funciones recientes el ViewModel llama directamente al repositorio, por ejemplo en el flujo del técnico, por lo que la separación es una guía de responsabilidades y no todos los caminos pasan por un UseCase.

---

## 3. Archivo principal de arranque: `FixNowApp.kt`

Este es el punto de entrada de la app. Su código más importante es:

```kotlin
@HiltAndroidApp
class FixNowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        SolicitudSyncScheduler.enqueue(this)
        logFcmTokenInDebug()
    }
}
```

### Qué hace aquí

- `@HiltAndroidApp`: activa Hilt para inyectar dependencias.
- `MapLibre.getInstance(this)`: inicializa el mapa.
- `SolicitudSyncScheduler.enqueue(this)`: programa el trabajo de reintento de solicitudes pendientes.
- `logFcmTokenInDebug()`: en modo debug saca el token FCM para pruebas.

Esto asegura que la app tenga acceso al mapa y que el sistema de sincronización quede activo desde el inicio.

---

## 4. Flujo de autenticación

### Usuarios y roles
La app contempla varios roles:
- `cliente`
- `tecnico`
- `admin`

El perfil del usuario vive en Firestore en la colección `users/{uid}`.
El rol se compara exactamente como texto en minúsculas. El registro crea perfiles `cliente`; `tecnico` y `admin` se asignan desde un entorno administrativo de confianza, no desde el formulario del usuario. La pantalla Home muestra opciones según el rol: cliente crea y consulta sus solicitudes; técnico ve solicitudes cercanas y las que aceptó; admin dispone de ambos flujos y del sistema de diseño.

### Archivo clave: `data/repository/AuthRepositoryImpl.kt`

Este repository implementa el acceso al login, registro, logout y estados de sesión.

```kotlin
override suspend fun register(email: String, password: String): Result<AuthUser> = safeCall {
    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
    val user = authResult.user ?: error("No se pudo crear el usuario.")
    user.sendEmailVerification().await()
    user.toAuthUser()
}
```

### Qué hace esta parte
- crea la cuenta en Firebase Auth
- manda verificación por email
- convierte el usuario de Firebase a un modelo propio de la app (`AuthUser`)
- no rompe la UI con Firebase directo

El dominio que usa la pantalla no sabe que Firebase existe.

### Flujo completo del login

```text
Usuario escribe email + password
      ↓
LoginViewModel
      ↓
LoginUserUseCase
      ↓
AuthRepository.login(...)
      ↓
FirebaseAuth.signInWithEmailAndPassword
      ↓
Resultado -> StateFlow/UI
```

Si sirve, en la app se usa `StateFlow` para manejar si está cargando, si hubo error, si hay navegación, etc.

### Avatar circular del perfil en Home
`HomeViewModel.refresh()` obtiene el perfil de Firestore y llena `HomeUiState.fotoBase64`. `FixNowNavHost` pasa ese valor a `HomeScreen`; la pantalla decodifica el Base64 como bitmap, lo recorta en un círculo y conserva la inicial del nombre como alternativa si no existe una imagen válida. El círculo es también el botón que navega a Perfil.

```text
users/{uid}.fotoBase64 → HomeViewModel → HomeUiState
                    → FixNowNavHost → HomeScreen → avatar circular
```

---

## 5. Flujo de creación de una solicitud de servicio

Este es el flujo importante de cliente.

### Modelo principal
Archivo: `domain/model/SolicitudServicio.kt`

```kotlin
data class SolicitudServicio(
    val id: String = "",
    val clienteId: String = "",
    val categoriaId: String = "",
    val categoriaNombre: String = "",
    val descripcion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val fotoBase64: String = "",
    val urgencia: String = UrgenciaServicio.NORMAL.name,
    val precioSugerido: Double = 0.0,
    val estado: String = EstadoSolicitud.PUBLICADA.name,
    val tecnicoId: String? = null,
    val comentarioEstado: String = "",
    val fechaCreacion: Long = 0L,
    val calificacionCliente: Int? = null,
    val comentarioCliente: String = "",
    val calificacionTecnico: Int? = null,
    val comentarioTecnico: String = "",
    val calificadoPorCliente: Boolean = false,
    val calificadoPorTecnico: Boolean = false
)
```

### ¿Qué contiene?
- datos del cliente
- ubicación
- dirección
- foto en Base64
- urgencia y precio sugerido
- estado actual
- id del técnico asignado
- fecha de creación

Esto permite que todo viaje en un solo documento de Firestore y que el técnico pueda ver la info como una "solicitud completa".

### Repositorio que crea la solicitud
Archivo: `data/repository/SolicitudRepositoryImpl.kt`

```kotlin
val documento = firestore.collection("solicitudes").document()

val datos = hashMapOf<String, Any?>(
    "id" to documento.id,
    "clienteId" to usuario.uid,
    "categoriaId" to solicitud.categoriaId,
    "categoriaNombre" to solicitud.categoriaNombre,
    "descripcion" to solicitud.descripcion.trim(),
    "latitud" to solicitud.latitud,
    "longitud" to solicitud.longitud,
    "direccion" to solicitud.direccion.trim(),
    "fotoBase64" to solicitud.fotoBase64,
    "urgencia" to solicitud.urgencia,
    "precioSugerido" to solicitud.precioSugerido,
    "estado" to EstadoSolicitud.PUBLICADA.name,
    "tecnicoId" to null,
    "fechaCreacion" to FieldValue.serverTimestamp()
)

documento.set(datos).await()
```

### Qué hace esta parte
- crea un documento en `solicitudes/{id}`
- guarda el dato del cliente
- guarda la categoría, ubicación y descripción
- guarda la foto como Base64
- publica con `estado = PUBLICADA`
- pone `tecnicoId = null` porque aun no tiene asignación

### Importante: no se usa Storage
La app usa Base64 directamente, como ya lo definiste en el proyecto. Esto evita Firebase Storage y reduce complejidad.

### Ubicación actual o elegida en el mapa
El formulario ofrece dos caminos para establecer el punto del servicio:

1. **Usar mi ubicación:** `FixNowNavHost` solicita permiso y llama a `SolicitudServicioViewModel.obtenerUbicacion()`. Google Location Services entrega coordenadas; el ViewModel intenta resolver la dirección con Android `Geocoder`.
2. **Elegir ubicación en el mapa:** `SolicitudServicioScreen` abre `SelectorUbicacionDialog`. `MapLibreMap.addOnMapClickListener` actualiza el marcador y **Usar este punto** devuelve latitud/longitud sin requerir permiso GPS.

En ambos casos `seleccionarUbicacionManual()` o `obtenerUbicacion()` guarda coordenadas y dirección en `SavedStateHandle`, reflejándolas en `SolicitudServicioUiState`. `enviarSolicitud()` comprueba que las coordenadas existan antes de crear el documento. Si la geocodificación falla, el punto queda guardado y la dirección se puede escribir manualmente.

```text
SolicitudServicioScreen
    ├─ GPS → LocationServices → ViewModel
    └─ Elegir mapa → SelectorUbicacionDialog → MapLibre → ViewModel
                                                                                                ↓
                                                                            SavedStateHandle + Geocoder
                                                                                                ↓
                                                                crearSolicitud() → Firestore
```

---

## 6. Flujo de reintento offline con WorkManager

Esto es la parte que te pediste: cuando el usuario está sin internet, su solicitud debe quedar guardada y enviarse cuando vuelva la red.

### Estructura creada

- `data/local/PendingSolicitudQueue.kt`
- `work/SolicitudSyncScheduler.kt`
- `work/SolicitudSyncWorker.kt`

### Colita local
Archivo: `data/local/PendingSolicitudQueue.kt`

```kotlin
class PendingSolicitudQueue(context: Context) {
    private val prefs = context.getSharedPreferences("pending_solicitudes", Context.MODE_PRIVATE)

    fun enqueue(solicitud: SolicitudServicio) {
        val items = load().toMutableList()
        items.add(solicitud)
        prefs.edit().putString(KEY, gson.toJson(items)).apply()
    }
}
```

### Qué hace
- guarda la solicitud pendiente en `SharedPreferences`
- la lista queda en memoria local del teléfono
- cuando se restablece la red, el worker la intenta enviar

### Scheduler
Archivo: `work/SolicitudSyncScheduler.kt`

```kotlin
val request = OneTimeWorkRequestBuilder<SolicitudSyncWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        30,
        TimeUnit.SECONDS
    )
    .build()

WorkManager.getInstance(context)
    .enqueueUniqueWork("solicitud_sync", ExistingWorkPolicy.KEEP, request)
```

### Qué hace esto
- crea un trabajo único tipo `OneTimeWorkRequest`
- obliga a que haya red disponible
- reintenta con backoff exponencial
- no duplica trabajos si ya está en cola
- funciona aunque la app esté cerrada

### Worker
Archivo: `work/SolicitudSyncWorker.kt`

```kotlin
override suspend fun doWork(): Result {
    val pendingRequests = queue.load()
    if (pendingRequests.isEmpty()) return Result.success()

    for (solicitud in pendingRequests) {
        try {
            document.set(payload).await()
            sentCount += 1
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return Result.retry()
            }
            return Result.failure()
        }
    }
    queue.remove(pendingRequests.map { it.id })
    mostrarNotificacion(...)
    return Result.success()
}
```

### Qué hace aquí
- recorre una cola de solicitudes esperando a sincronizarse
- intenta guardar cada documento en Firestore
- si no hay red, devuelve `Result.retry()`
- cuando termina, elimina los pendientes y manda una notificación local de “Solicitudes enviadas”

Esto evita que la solicitud se pierda cuando el usuario estaba sin conexión.

---

## 7. Flujo de “Mis solicitudes” del cliente

La pantalla escucha `observarMisSolicitudes()` y muestra los cambios de estado en tiempo real. `obtenerMisSolicitudes()` también existe para recargas explícitas; ambas consultas ordenan por fecha de creación.

### Consulta problemática
Archivo: `data/repository/SolicitudRepositoryImpl.kt`

```kotlin
val resultado = firestore
    .collection("solicitudes")
    .whereEqualTo("clienteId", usuario.uid)
    .orderBy("fechaCreacion", Query.Direction.DESCENDING)
    .get(Source.SERVER)
    .await()
```

### Por qué fallaba
Firestore exige un índice compuesto sobre:
- `clienteId`
- `fechaCreacion`

Si no existe, lanza:

```text
FAILED_PRECONDITION: the query requires an index
```

### Índice correcto
Archivo: `firebase/firestore.indexes.json`

```json
{
  "indexes": [
    {
      "collectionGroup": "solicitudes",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "clienteId", "order": "ASCENDING" },
        { "fieldPath": "fechaCreacion", "order": "DESCENDING" }
      ]
    }
  ]
}
```

### Cómo corregirlo paso a paso

1. Conectarse al proyecto correcto:

```powershell
firebase use fixnow-919f5
```

2. Desplegar solo los índices:

```powershell
firebase deploy --only firestore:indexes
```

3. Si la consola lo pide, crear manualmente el índice:
   - colección: `solicitudes`
   - campo 1: `clienteId` (ASC)
   - campo 2: `fechaCreacion` (DESC)

4. Reabrir la pantalla de Mis solicitudes.

El archivo de índices actual también contempla las consultas del técnico (`tecnicoId` + `fechaCreacion`) y de solicitudes disponibles (`estado` + `tecnicoId`). Los índices desplegados evitan el error `FAILED_PRECONDITION` para esos filtros y ordenamientos.

### Estados del ciclo de vida

Los estados están definidos en `domain/model/EstadoSolicitud.kt`:

| Estado | Significado |
|--------|-------------|
| `PUBLICADA` | Solicitud creada, disponible para un técnico. |
| `ACEPTADA` | Un técnico la aceptó y quedó asignado. |
| `EN_PROCESO` | El trabajo comenzó. |
| `FINALIZADA` | El técnico terminó el trabajo. |
| `CANCELADA` | La solicitud fue cancelada por el cliente antes de asignarse o por el técnico asignado. |

El cliente puede cancelar mientras la solicitud siga `PUBLICADA` y no tenga técnico. La tarjeta muestra también `comentarioEstado`, que sirve para informar al cliente sobre el progreso.

---

## 8. Flujo de seguimiento de solicitudes para técnicos

La app también tiene una parte de técnicos que ven solicitudes cercanas.

### Archivo principal
- `presentation/tecnico/SolicitudesTecnicoViewModel.kt`
- `presentation/tecnico/SolicitudesTecnicoScreen.kt`

### Solicitudes cercanas
El ViewModel obtiene la ubicación del técnico y observa solicitudes publicadas sin asignar. La pantalla muestra lista y mapa con MapLibre, calcula distancias, y permite ordenar por distancia, urgencia o fecha reciente. `GeocodingApi` consulta la dirección cuando se selecciona un punto.

La consulta Firestore no es geoespacial: primero recibe todas las solicitudes `PUBLICADA` sin técnico y la pantalla filtra localmente las que están a más de 50 km del GPS. El control **Mostrar todas** permite quitar ese filtro. Por eso una lista sin resultados puede significar que no hay publicaciones, que ninguna queda dentro del radio o que la consulta falló; la pantalla diferencia esos estados y muestra el error cuando Firestore lo devuelve.

### Consulta usada para técnicos
Archivo: `SolicitudRepositoryImpl.kt`

```kotlin
val listener = firestore
    .collection("solicitudes")
    .whereEqualTo("estado", EstadoSolicitud.PUBLICADA.name)
    .whereEqualTo("tecnicoId", null)
    .addSnapshotListener { snapshot, error ->
        if (snapshot != null) {
            trySend(snapshot.documents.map { it.toSolicitudServicio() })
        }
    }
```

Esto permite observar en tiempo real todas las solicitudes disponibles del área.

### Aceptación y solicitudes asignadas
La pantalla tiene una pestaña **Mis solicitudes**, alimentada por `observarSolicitudesTecnico()` (`tecnicoId == uid`). Al aceptar una solicitud, `SolicitudRepositoryImpl.aceptarSolicitud()` ejecuta una transacción que verifica que siga publicada y sin asignar; en la misma operación establece `estado = ACEPTADA` y el `tecnicoId`. Esto evita que dos técnicos acepten el mismo trabajo.

Desde esa bandeja el técnico puede iniciar el trabajo (`EN_PROCESO`), publicar una nota de progreso de hasta 500 caracteres, finalizarlo o cancelarlo. Las transiciones se validan en el repositorio y también en `firebase/firestore.rules`.

---

## 9. Sistema de notificaciones FCM

### Servicio FCM
Archivo: `service/FixNowMessagingService.kt`

```kotlin
class FixNowMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { profile ->
                if (profile.getString("rol") == "tecnico") guardarToken(uid, token)
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: "FixNow"
        val body = message.data["body"] ?: "" 
        mostrarNotificacion(title, body, message.data["requestId"])
    }
}
```

### Qué hace
- registra el token FCM del técnico cuando cambia
- guarda ese token en `users/{uid}/fcmTokens/{token}`
- recibe un push con la información de la solicitud
- crea una notificación local para abrir la app y ver el detalle

### Importante
La notificación no se dispara solo por el app; requiere que un backend mande el mensaje a los tokens guardados. En Firebase esto normalmente se hace con:
- Cloud Functions, o
- un backend externo con `firebase-admin`

En este proyecto, la función para notificar técnicos cercanos se preparó en `firebase/functions/index.js`.
El cliente FCM está preparado para registrar tokens y recibir mensajes, pero el envío automático requiere desplegar un backend emisor. Cloud Functions requiere el plan Blaze, por lo que no se debe presentar el aviso automático como operativo en el plan Spark.

---

## 10. Reglas de Firestore

Archivo: `firebase/firestore.rules`

Las reglas verifican identidad y rol, además de validar las escrituras. En concreto, controlan quién puede crear/leer solicitudes, aceptar una publicación sin asignar, actualizar una solicitud que tiene asignada y calificarla una sola vez cuando está finalizada o cancelada. La ubicación se limita a coordenadas válidas del técnico/admin autenticado.

### Colecciones y privacidad

| Ruta | Contenido y acceso |
|------|--------------------|
| `users/{uid}` | Perfil y rol; cada usuario lee su perfil. |
| `solicitudes/{id}` | Datos del servicio, estado, técnico asignado, nota de progreso y banderas/estrellas de rating. La nota `comentarioEstado` se muestra al cliente. |
| `reputaciones/{uid}` | Puntos acumulados y total de valoraciones; el promedio se calcula como puntos / total. Los usuarios autenticados pueden leer el resumen. |
| `users/{uid}/valoraciones/{solicitudId}` | Comentario privado recibido por ese perfil. Solo el propietario del perfil puede leer sus reseñas. La regla comprueba que la reseña corresponda al rating válido de esa solicitud. |

Los comentarios de rating no se copian al documento público de la solicitud. Se guardan separados de las notas de progreso, que sí son parte del seguimiento del trabajo.

---

## 11. Flujo end-to-end real de FixNow

### Caso: cliente crea solicitud

```text
Cliente abre app
     ↓
Rellena categoria, descripción, ubicación, urgencia, foto
     ↓
ViewModel valida
     ↓
SolicitudRepositoryImpl.crearSolicitud(...)
     ↓
Firestore: collection("solicitudes") -> nuevo documento
     ↓
estado = PUBLICADA
     ↓
si hay red -> se publica inmediatamente
si no hay red -> se guarda en cola local y se reintenta con WorkManager
```

### Caso: técnico recibe notificación

```text
Solicitud publicada en Firestore
     ↓
Backend / Cloud Function / servicio externo mira usuarios técnicos
     ↓
Obtiene sus tokens FCM
     ↓
Envia push multicast
     ↓
FixNowMessagingService.onMessageReceived(...)
     ↓
Muestra notificación local
     ↓
Técnico abre la app y ve la solicitud cercana
```

### Caso: cliente consulta sus solicitudes

```text
Cliente entra a pantalla "Mis solicitudes"
     ↓
MisSolicitudesViewModel observa observarMisSolicitudes()
     ↓
Firestore escucha documentos donde clienteId = uid, ordenados por fecha
    ↓
Los cambios de estado/notas llegan a la pantalla en tiempo real
```

### Caso: técnico acepta y procesa una solicitud

```text
Técnico abre “Solicitudes cercanas” y acepta una publicación
    ↓
SolicitudRepositoryImpl.aceptarSolicitud() valida y asigna en una transacción
    ↓
PUBLICADA → ACEPTADA → EN_PROCESO
    ↓
Técnico informa progreso y finaliza o cancela
    ↓
El cliente ve el estado y la nota desde su listener
```

### Caso: rating mutuo

```text
Solicitud FINALIZADA o CANCELADA y con técnico asignado
    ↓
Cada parte puede enviar una calificación de 1 a 5 estrellas una sola vez
    ↓
Una transacción actualiza la solicitud y reputaciones/{uid} del receptor
    ↓
El comentario se guarda en users/{receptorUid}/valoraciones/{solicitudId}
    ↓
El perfil del receptor escucha su resumen y reseñas privadas
```

El perfil muestra promedio, cantidad de reviews y cinco estrellas; con cero calificaciones las estrellas aparecen grises.

---

## 12. Qué archivo leer primero si quieres entender el proyecto

Si alguien quiere entender rápidamente la app, el orden ideal es:

1. `FixNowApp.kt`
2. `di/RepositoryModule.kt`
3. `domain/model/*`
4. `domain/repository/*`
5. `data/repository/AuthRepositoryImpl.kt`
6. `data/repository/SolicitudRepositoryImpl.kt`
7. `presentation/.../ViewModel.kt`
8. `presentation/.../Screen.kt`
9. `service/FixNowMessagingService.kt`
10. `firebase/firestore.rules`
11. `firebase/firestore.indexes.json`

### Responsabilidad de los archivos principales

| Archivo | Responsabilidad |
|---------|-----------------|
| `presentation/navigation/FixNowNavHost.kt` | Declara las rutas Compose y conecta pantallas con ViewModels. |
| `presentation/home/HomeViewModel.kt` y `HomeScreen.kt` | Cargan el rol y `fotoBase64`; muestran las opciones por rol y el avatar circular con fallback a inicial. |
| `domain/model/SolicitudServicio.kt` y `EstadoSolicitud.kt` | Definen los datos de una solicitud y sus estados válidos para la UI. |
| `domain/repository/SolicitudRepository.kt` | Contrato de las operaciones de solicitud, aceptación, seguimiento y calificación. |
| `data/repository/SolicitudRepositoryImpl.kt` | Implementa las consultas en tiempo real, transacciones de aceptación/estado/rating y sincronización con Firestore. |
| `data/mapper/SolicitudMapper.kt` | Convierte documentos de Firestore en `SolicitudServicio`. |
| `presentation/solicitud/MisSolicitudesViewModel.kt` y `MisSolicitudesScreen.kt` | Observan las solicitudes del cliente, muestran estados y permiten cancelar o calificar. |
| `presentation/solicitud/SolicitudServicioScreen.kt` y `SelectorUbicacionDialog.kt` | Presentan el formulario y selector manual de ubicación en MapLibre. |
| `presentation/solicitud/SolicitudServicioViewModel.kt` | Conserva el formulario, resuelve coordenadas/dirección y crea la solicitud. |
| `presentation/tecnico/SolicitudesTecnicoViewModel.kt` y `SolicitudesTecnicoScreen.kt` | Gestionan solicitudes cercanas, mapa, solicitudes aceptadas, progreso y rating al cliente. |
| `presentation/common/CalificacionDialog.kt` | Entrada de 1–5 estrellas y comentario para una valoración. |
| `presentation/profile/ProfileViewModel.kt` y `ProfileScreen.kt` | Escuchan el resumen de reputación y reseñas privadas, y muestran promedio, total y estrellas. |
| `firebase/firestore.rules` y `firebase/firestore.indexes.json` | Aplican permisos/transiciones del negocio y soportan las consultas compuestas. |

Para seguir específicamente una solicitud, comienza por `presentation/solicitud/MisSolicitudesViewModel.kt` (cliente) o `presentation/tecnico/SolicitudesTecnicoViewModel.kt` (técnico), continúa en `domain/repository/SolicitudRepository.kt` y termina en `data/repository/SolicitudRepositoryImpl.kt`.

---

## 13. Resumen de la lógica principal

La aplicación está construida sobre una idea clara:

- la capa de presentación no sabe nada de Firebase
- la capa de dominio define las reglas y los contratos
- la capa de datos se encarga del backend real
- las solicitudes se guardan en Firestore
- FCM procesa notificaciones; para enviar alertas automáticas a técnicos hace falta un backend emisor. Cloud Functions no se despliegan en el plan Spark.
- las solicitudes pendientes se reintentan con WorkManager al recuperar la red
- el sistema está pensado para funcionar con Base64 y sin Storage
- los comentarios de las valoraciones son privados para el perfil receptor

Eso hace que la app sea más mantenible, más fácil de testear y más segura.

---

## 14. Conclusión

FixNow usa una arquitectura moderna y limpia:

- `presentation` para UI
- `domain` para negocio
- `data` para Firebase
- `service` para FCM
- `work` para sincronización offline
- `firebase` para reglas, índices y configuración del backend de notificaciones

La clave es que toda la lógica importante está en repositorios y ViewModels, mientras que el negocio real se guarda en Firestore con reglas y protección por usuario.
