# Guía del código de FixNow

Esta guía explica **qué hace cada archivo importante, qué métodos tiene y cómo se conectan entre sí**. Está pensada para que puedas leer el proyecto sin perderte y defenderlo ante tu equipo o tu profesor.

Rutas: todos los archivos `.kt` están bajo `app/src/main/java/com/fixnow/app/`. Para no repetirlo, aquí se abrevia como `…/` (ejemplo: `…/presentation/auth/LoginFragment.kt`).

---

## Índice

1. [Qué es la app hoy](#1-qué-es-la-app-hoy)
2. [Conceptos que aparecen en el código](#2-conceptos-que-aparecen-en-el-código)
3. [Qué pasa cuando abres la app](#3-qué-pasa-cuando-abres-la-app)
4. [Capa `core`: utilidades](#4-capa-core-utilidades)
5. [Capa `domain`: reglas de negocio](#5-capa-domain-reglas-de-negocio)
6. [Capa `data`: donde vive Firebase](#6-capa-data-donde-vive-firebase)
7. [Carpeta `di`: cómo Hilt conecta todo](#7-carpeta-di-cómo-hilt-conecta-todo)
8. [Capa `presentation`: las pantallas](#8-capa-presentation-las-pantallas)
9. [Flujos de punta a punta](#9-flujos-de-punta-a-punta)
10. [Mapa de dependencias](#10-mapa-de-dependencias)
11. [Recursos: layouts, tema, navegación y manifiesto](#11-recursos-layouts-tema-navegación-y-manifiesto)
12. [Firestore: datos y reglas](#12-firestore-datos-y-reglas)
13. [Decisiones de diseño y trampas conocidas](#13-decisiones-de-diseño-y-trampas-conocidas)
14. [Quiero cambiar X: ¿dónde toco?](#14-quiero-cambiar-x-dónde-toco)
15. [Limitaciones actuales](#15-limitaciones-actuales)
16. [Orden sugerido para leer el código](#16-orden-sugerido-para-leer-el-código)

---

## 1. Qué es la app hoy

Cubre las historias HU01 a HU06: hay registro y login, una pantalla de inicio provisional, un perfil con foto, ingreso con huella y un catálogo del sistema de diseño. La lógica del negocio de FixNow (solicitudes, técnicos, precios) llega desde HU07.

### Pantallas y navegación

```
                        ┌───────────────┐
        (sin sesión) ──►│  LoginFragment │◄── (sesión + huella activada)
                        └──┬─────────┬──┘
              "Regístrate" │         │ login correcto / huella correcta
                           ▼         ▼
                  ┌────────────────┐ │
                  │ RegisterFragment│─┤ registro correcto
                  └────────────────┘ │
                                     ▼
   (sesión, sin huella) ────────►┌──────────────┐
                                 │ HomeFragment  │──"Cerrar sesión"──► LoginFragment
                                 └──┬────────┬──┘
                       "Mi perfil"  │        │ "Ver sistema de diseño"
                                    ▼        ▼
                        ┌────────────────┐  ┌──────────────────────┐
                        │ ProfileFragment │  │ DesignSystemFragment │
                        └────────────────┘  └──────────────────────┘
```

La app tiene **una sola Activity** (`MainActivity`). Todas las pantallas son Fragments que se intercambian dentro de ella con Navigation Component. El mapa de destinos vive en `res/navigation/nav_graph.xml`.

---

## 2. Conceptos que aparecen en el código

| Concepto | Qué es, en simple | Dónde lo verás |
|----------|-------------------|----------------|
| **Fragment** | Una pantalla. Solo dibuja y reenvía clics. | `presentation/*/…Fragment.kt` |
| **ViewModel** | Guarda el estado de una pantalla y decide qué hacer ante un clic. Sobrevive a girar el teléfono. | `presentation/*/…ViewModel.kt` |
| **UseCase (caso de uso)** | Una acción del negocio con nombre de verbo, en una clase con un solo método `invoke`. | `domain/usecase/` |
| **Repository** | El "almacén" de datos. La **interfaz** (contrato) vive en `domain`; la **implementación** con Firebase vive en `data`. | `domain/repository/`, `data/repository/` |
| **Hilt / `@Inject`** | Un "cableado automático": tú declaras qué necesita una clase en su constructor y Hilt se lo entrega. | Constructores con `@Inject`, `di/` |
| **`suspend fun`** | Función que puede esperar (red, base de datos) sin congelar la pantalla. Solo se llama desde otra `suspend` o desde una corrutina (`viewModelScope.launch`). | Repositorios y casos de uso |
| **`StateFlow`** | Un valor observable: "cómo está la pantalla ahora". Al cambiar, quien lo escucha se entera. | `uiState` en cada ViewModel |
| **`Channel` + `receiveAsFlow()`** | Una cola de mensajes de **una sola vez** (navegar, mostrar un aviso). | `events` en cada ViewModel |
| **`Result<T>`** | Un valor que es éxito (`onSuccess`) o fallo (`onFailure`), sin lanzar excepciones. | Repositorios y casos de uso |
| **ViewBinding** | Acceso seguro a las vistas del layout: `binding.buttonLogin` en vez de `findViewById`. | Todos los fragments |
| **`?attr/colorPrimary`** | Leer un color del tema en vez de escribirlo a mano. Es lo que hace funcionar el modo oscuro. | Layouts |

---

## 3. Qué pasa cuando abres la app

```
Android crea FixNowApp ──► Hilt se inicializa
        │
        ▼
MainActivity.onCreate
        │  1. infla activity_main.xml (solo contiene el NavHostFragment)
        │  2. pide a MainViewModel: ¿startAtHome?
        │  3. infla nav_graph.xml y le cambia el destino inicial
        ▼
Se muestra LoginFragment o HomeFragment
```

### `…/FixNowApp.kt`
- `@HiltAndroidApp` es lo que activa Hilt en toda la app. **Sin esta anotación, nada de `@Inject` funciona.** Está registrada en el manifiesto como `android:name=".FixNowApp"`.
- `onCreate()` llama a `logFcmTokenInDebug()`, que solo en modo debug imprime el token de notificaciones en Logcat (filtro `FixNowFCM`).

### `…/presentation/main/MainActivity.kt`
- `@AndroidEntryPoint`: permite inyectar en la Activity (aquí, el ViewModel).
- `onCreate()`:
  1. `ActivityMainBinding.inflate(...)` y `setContentView(...)`.
  2. Busca el `NavHostFragment` (`R.id.navHostFragment`) y toma su `navController`.
  3. Infla `R.navigation.nav_graph`, llama `graph.setStartDestination(...)` con `homeFragment` o `loginFragment` según `viewModel.startAtHome`, y asigna `navController.graph = graph`.
- No tiene lógica de negocio: solo decide entre dos destinos con lo que le dice el ViewModel.

### `…/presentation/main/MainViewModel.kt`
- Una sola propiedad: `val startAtHome: Boolean = getCurrentUser() != null && !isBiometricEnabled()`.
- La regla, en tres casos:

| Situación | Destino inicial |
|-----------|-----------------|
| No hay sesión guardada | Login |
| Hay sesión y la huella **no** está activada | Home directo |
| Hay sesión y la huella **sí** está activada | Login (que pide la huella al abrirse) |

- Solo importa en el primer arranque; si la app se recrea, el `NavController` restaura la pila de pantallas que ya tenía.

---

## 4. Capa `core`: utilidades

Código de apoyo que no pertenece a ninguna pantalla en particular.

### `…/core/util/SafeCall.kt`
```kotlin
suspend inline fun <T> safeCall(errorMapper: (Throwable) -> String = {...}, block: suspend () -> T): Result<T>
```
- Ejecuta `block` dentro de un `try/catch` y devuelve `Result.success(valor)` o `Result.failure(Exception(mensajeAmigable))`.
- El `errorMapper` traduce la excepción técnica a texto para el usuario. Los repositorios le pasan `Throwable::toFriendlyMessage`.
- **Detalle importante:** si la excepción es `CancellationException` (la corrutina fue cancelada, por ejemplo al salir de la pantalla) se **relanza**; tragársela rompe el mecanismo de cancelación.
- Es la razón por la que ningún repositorio tiene `try/catch` repetidos.

### `…/core/util/Validators.kt`
- `String.isValidEmail()`: valida el formato con una expresión regular. Es Kotlin puro, por eso tiene test (`app/src/test/.../ValidatorsTest.kt`).

### `…/core/util/Extensions.kt`
| Función | Qué hace |
|---------|----------|
| `View.setVisible(Boolean)` | Pone `VISIBLE` o `GONE`. |
| `Fragment.showSnackbar(mensaje)` | Muestra un aviso en la parte baja usando la vista del fragment. |
| `Fragment.collectWhenStarted(flow) { ... }` | Escucha un `Flow` **solo mientras la pantalla está visible** (`repeatOnLifecycle(STARTED)`). Es la forma recomendada de consumir `uiState` y `events` desde un Fragment. Todos los fragments la usan. |

---

## 5. Capa `domain`: reglas de negocio

Regla de oro: **`domain` no importa nada de Android ni de Firebase.** Por eso podría probarse con tests simples y por eso cambiar Firebase no lo afecta.

### 5.1 Modelos (`…/domain/model/`)

| Archivo | Contenido |
|---------|-----------|
| `User.kt` | Perfil guardado en Firestore: `uid`, `nombre`, `email`, `fotoBase64`, `rol`, `notificacionesActivas`, `biometriaActivada`, `creadoEn`. **Todos tienen valor por defecto**, porque Firestore lo necesita para convertir el documento en objeto. Constantes `ROL_CLIENTE`, `ROL_TECNICO`, `ROL_ADMIN`. |
| `AuthUser.kt` | Usuario autenticado, versión simple: `uid`, `email`, `isEmailVerified`. Existe para que el resto de la app **no dependa de `FirebaseUser`**. |
| `AuthResult.kt` | `sealed class` con dos casos: `Success(user)` y `Error(message)`. Lo devuelven el registro y el login. |

### 5.2 Contratos (`…/domain/repository/`)

| Interfaz | Métodos | Implementada por |
|----------|---------|------------------|
| `AuthRepository` | `currentUser`, `observeAuthState()`, `register`, `login`, `sendPasswordReset`, `resendVerificationEmail`, `refreshCurrentUser`, `logout` | `AuthRepositoryImpl` |
| `ProfileRepository` | `getProfile`, `createInitialProfile`, `updateProfile`, `saveProfilePhoto`, `setBiometricEnabled` | `ProfileRepositoryImpl` |
| `BiometricSettingsRepository` | `isEnabled()`, `setEnabled(Boolean)` | `BiometricSettingsRepositoryImpl` |

### 5.3 Casos de uso (`…/domain/usecase/`)

Cada uno tiene un solo método: `operator fun invoke(...)`. Eso permite llamarlos como si fueran funciones: `loginUser(email, password)`.

| Caso de uso | Qué hace | Lo usa |
|-------------|----------|--------|
| `auth/RegisterUserUseCase` | Crea la cuenta (`AuthRepository.register`) y, si salió bien, crea el documento inicial del perfil (`ProfileRepository.createInitialProfile`). **Es el único que usa dos repositorios.** | `RegisterViewModel` |
| `auth/LoginUserUseCase` | Inicia sesión. | `LoginViewModel` |
| `auth/SendPasswordResetUseCase` | Envía el correo de recuperación. | `LoginViewModel` |
| `auth/ResendVerificationEmailUseCase` | Reenvía el correo de verificación. | `HomeViewModel` |
| `auth/GetCurrentUserUseCase` | Devuelve el `AuthUser` con sesión guardada, o `null`. | `MainViewModel`, `LoginViewModel`, `HomeViewModel`, `ProfileViewModel` |
| `auth/RefreshCurrentUserUseCase` | Vuelve a pedir el usuario a Firebase (para saber si ya verificó el correo). | `HomeViewModel` |
| `auth/LogoutUseCase` | Cierra sesión. | `HomeViewModel` |
| `auth/ObserveAuthStateUseCase` | Flujo que emite cada cambio de sesión. **Hoy nadie lo usa**; queda listo para casos como detectar una sesión vencida. | — |
| `profile/GetUserProfileUseCase` | Lee el documento del usuario. | `HomeViewModel`, `ProfileViewModel` |
| `profile/UpdateProfileUseCase` | Guarda nombre y preferencia de notificaciones. | `ProfileViewModel` |
| `profile/SaveProfilePhotoUseCase` | Procesa y guarda la foto. | `ProfileViewModel` |
| `biometric/IsBiometricEnabledUseCase` | Lee si la huella está activada **en este dispositivo**. | `MainViewModel`, `LoginViewModel`, `ProfileViewModel` |
| `biometric/SetBiometricEnabledUseCase` | Guarda el valor **primero en el dispositivo** y luego en Firestore. Devuelve el resultado de Firestore. | `ProfileViewModel` |

---

## 6. Capa `data`: donde vive Firebase

Aquí es el único lugar donde se importan clases de Firebase.

### 6.1 `…/data/repository/AuthRepositoryImpl.kt` (HU04)

Recibe `FirebaseAuth` por constructor (Hilt lo provee) e implementa `AuthRepository`.

| Método | Qué hace |
|--------|----------|
| `currentUser` | `firebaseAuth.currentUser` convertido a `AuthUser`. Firebase guarda la sesión en el teléfono; por eso sigue ahí al reabrir la app (sesión persistente). |
| `observeAuthState()` | Envuelve `AuthStateListener` de Firebase en un `Flow` con `callbackFlow`. Al dejar de escuchar, `awaitClose` quita el listener (evita fugas de memoria). |
| `register(email, password)` | `createUserWithEmailAndPassword(...).await()`; luego intenta `sendEmailVerification()` como "mejor esfuerzo" (si falla, la cuenta igual queda creada); devuelve `AuthResult`. |
| `login(email, password)` | `signInWithEmailAndPassword(...).await()` → `AuthResult`. |
| `sendPasswordReset(email)` | `sendPasswordResetEmail(...)`. |
| `resendVerificationEmail()` | `sendEmailVerification()` del usuario actual; falla con mensaje claro si no hay sesión. |
| `refreshCurrentUser()` | `user.reload()` para refrescar `isEmailVerified`. Si falla (sin internet) usa los datos que ya tenía. |
| `logout()` | `firebaseAuth.signOut()`. |

Dos funciones privadas al final: `FirebaseUser.toAuthUser()` (convierte al modelo del dominio) y `Result<AuthUser>.toAuthResult()` (convierte `Result` en `AuthResult`).

`.await()` viene de `kotlinx-coroutines-play-services` y convierte los `Task` de Firebase (que usan callbacks) en funciones `suspend`.

### 6.2 `…/data/repository/ProfileRepositoryImpl.kt` (HU05 y HU06)

Recibe `FirebaseFirestore` y `ImageEncoder`. Trabaja sobre la colección `users`, con el `uid` como ID de documento.

| Método | Qué hace |
|--------|----------|
| `getProfile(uid)` | Lee el documento y lo convierte con `toObject(User::class.java)`. Si no existe, devuelve un `User(uid = uid)` vacío. |
| `createInitialProfile(uid, email)` | `set(User(...))`: crea el documento con valores por defecto (rol `cliente`). |
| `updateProfile(uid, nombre, notificacionesActivas)` | Escribe solo esos dos campos. |
| `saveProfilePhoto(uid, imageUri)` | Pasa la foto por `ImageEncoder` (en `Dispatchers.IO`, porque es trabajo pesado) y guarda el Base64 en `fotoBase64`. Devuelve ese Base64. |
| `setBiometricEnabled(uid, enabled)` | Guarda `biometriaActivada`. |

Las escrituras usan `SetOptions.merge()`: si el documento no existe se crea, y si existe **solo cambian los campos indicados** (no borra los demás). Sin `merge`, un `set` reemplazaría todo el documento.

### 6.3 `…/data/local/ImageEncoder.kt`

`toSquareBase64(uriString, targetSizePx = 256)` convierte la foto de la cámara en una miniatura de unos 20–40 KB, en cinco pasos:

1. Lee **solo las dimensiones** (`inJustDecodeBounds`), para no cargar una foto de 12 MP en memoria.
2. Calcula `inSampleSize` (potencias de 2) y decodifica ya reducida.
3. Lee la orientación EXIF y **endereza** la imagen (las cámaras guardan fotos "de lado" con una marca).
4. **Recorta al centro** en cuadrado y escala a 256 × 256.
5. Comprime a JPEG calidad 80 y lo codifica en Base64 (`NO_WRAP`).

Existe porque Firebase Storage exige un plan de pago (ver README).

### 6.4 `…/data/local/BiometricSettingsRepositoryImpl.kt` (HU06)

Guarda un solo dato, `biometric_enabled`, en `EncryptedSharedPreferences` (cifrado con una llave del Keystore del teléfono). Es **por dispositivo**: activar la huella en un teléfono no la activa en otro.
- `isEnabled()` y `setEnabled()` están envueltos en `runCatching`: si el Keystore del teléfono falla, se asume "desactivado" en lugar de cerrar la app.
- El `SharedPreferences` se crea con `by lazy`, es decir, solo la primera vez que se usa.

### 6.5 `…/data/mapper/ErrorMapper.kt`

`Throwable.toFriendlyMessage()` convierte errores técnicos en español legible. Ejemplos: contraseña débil, correo ya registrado, credenciales incorrectas, sin internet, permisos denegados en Firestore. **El orden del `when` importa:** `FirebaseAuthWeakPasswordException` hereda de `FirebaseAuthInvalidCredentialsException`, así que debe ir antes.

---

## 7. Carpeta `di`: cómo Hilt conecta todo

Hilt necesita saber **cómo fabricar** lo que no puede construir solo. Hay dos casos:

| Archivo | Problema que resuelve |
|---------|-----------------------|
| `di/FirebaseModule.kt` | `FirebaseAuth` y `FirebaseFirestore` no son clases nuestras: no se les puede poner `@Inject`. Con `@Provides` le decimos a Hilt cómo obtenerlas (`FirebaseAuth.getInstance()`). |
| `di/RepositoryModule.kt` | Los casos de uso piden `AuthRepository` (interfaz). Con `@Binds` le decimos a Hilt: "cuando pidan `AuthRepository`, entrega `AuthRepositoryImpl`". Igual para `ProfileRepository` y `BiometricSettingsRepository`. |

Ambos se instalan en `SingletonComponent`: hay **una sola instancia** para toda la app.

Los ViewModels usan `@HiltViewModel` + `@Inject constructor(...)` y los fragments `@AndroidEntryPoint`. Así, al escribir `private val viewModel: LoginViewModel by viewModels()`, Hilt arma toda la cadena:

```
LoginViewModel ◄─ LoginUserUseCase ◄─ AuthRepository ◄─ AuthRepositoryImpl ◄─ FirebaseAuth
                                       (interfaz)        (la elige RepositoryModule)  (la provee FirebaseModule)
```

---

## 8. Capa `presentation`: las pantallas

Todas siguen el mismo molde:

```
Fragment                              ViewModel
  ├─ onViewCreated: conecta clics ───►  función pública (login, register, ...)
  ├─ collectWhenStarted(uiState) ◄────  StateFlow<...UiState>   (cómo está la pantalla)
  └─ collectWhenStarted(events)  ◄────  Flow<...Event>          (cosas de una sola vez)
```

- **`...UiState`** (data class): estado que se repinta igual tras girar el teléfono. Ej.: `LoginUiState(isLoading)`.
- **`...Event`** (sealed class): navegar o mostrar un mensaje. Van por un `Channel` para que **no se repitan** al recrear la pantalla.

### 8.1 Login (HU04 + HU06)

**`LoginViewModel`** (`…/presentation/auth/`)

| Miembro | Qué hace |
|---------|----------|
| `uiState: StateFlow<LoginUiState>` | `isLoading`: muestra la barra de progreso y bloquea el botón. |
| `events` | `NavigateToHome` o `ShowMessage(texto)`. |
| `biometricShortcutEnabled` | `true` si **hay sesión guardada y la huella está activada**. Se calcula una vez al crear el ViewModel. |
| `login(email, password)` | Valida correo y contraseña; si están bien, activa `isLoading`, llama `loginUser(...)` y emite `NavigateToHome` o `ShowMessage(error)`; al terminar apaga `isLoading`. |
| `onForgotPassword(email)` | Valida el correo y llama `sendPasswordReset`; emite un mensaje de éxito o de error. |
| `onBiometricSuccess()` | Emite `NavigateToHome` (la sesión de Firebase ya existía; la huella solo confirma identidad). |

**`LoginFragment`**
- `onViewCreated`: conecta los botones al ViewModel, llama `setupBiometricShortcut(...)` y escucha `uiState` y `events`.
- `setupBiometricShortcut(autoPrompt)`: el botón de huella se muestra solo si `viewModel.biometricShortcutEnabled` **y** `BiometricAuthManager.isBiometricAvailable(...)` (el teléfono tiene sensor y una huella registrada). Si `autoPrompt` es `true` (primera vez que se crea la pantalla, no tras girar) pide la huella automáticamente.
- `promptBiometric()`: llama a `BiometricAuthManager.showBiometricPrompt(...)`; si tiene éxito → `viewModel.onBiometricSuccess()`.
- Escucha `events`: `NavigateToHome` → `findNavController().navigate(R.id.action_login_to_home)`; `ShowMessage` → Snackbar.

### 8.2 Registro (HU04)

**`RegisterViewModel`**: `register(email, password, confirmPassword)` valida (correo válido, contraseña de al menos 6 caracteres, ambas iguales); si hay un error, emite `ShowMessage`. Si todo está bien llama `registerUser(...)` y emite `NavigateToHome` o `ShowMessage`.

**`RegisterFragment`**: idéntico en estructura a Login. "¿Ya tienes cuenta?" hace `popBackStack()`.

### 8.3 Home

**`HomeViewModel`**
- `HomeUiState(displayName, emailVerified)`.
- `refresh()`: pide el usuario actualizado (`refreshCurrentUser()`), lee su perfil para obtener el nombre y actualiza el estado. El nombre mostrado es `nombre` o, si está vacío, el correo.
- `resendVerification()`: reenvía el correo y avisa el resultado por evento.
- `signOut()`: `logout()`.

**`HomeFragment`**
- `onStart()` llama `viewModel.refresh()`: **cada vez que Home vuelve a mostrarse**, se actualiza el saludo y se oculta el aviso "Verifica tu correo" si ya lo verificaste.
- "Cerrar sesión": `viewModel.signOut()` y luego navega con `action_home_to_login`.
- Es provisional: aquí irá el flujo de solicitudes (HU07+).

### 8.4 Perfil (HU05 + HU06)

**`ProfileViewModel`**
- `ProfileUiState(isLoading, user, email, biometricEnabled)`.
- `init { loadProfile() }`: carga el perfil **una vez**, al crearse (no se recarga al girar el teléfono).
- `saveProfile(nombre, notificacionesActivas)`: exige un nombre de al menos 2 caracteres y llama `updateProfile`.
- `savePhoto(imageUri)`: enciende `isLoading`, llama `saveProfilePhoto` y, si salió bien, actualiza `user.fotoBase64` en el estado (sin recargar todo el perfil).
- `onBiometricToggled(enabled)`: actualiza el estado y llama `setBiometricEnabled(uid, enabled)`.

**`ProfileFragment`** (el más completo)

| Pieza | Qué hace |
|-------|----------|
| `cameraPermissionLauncher` | Pide el permiso `CAMERA`. Si lo dan → `launchCamera()`. |
| `takePictureLauncher` | Abre la cámara (`TakePicture`). Al volver con éxito → `viewModel.savePhoto(uri.toString())`. |
| `launchCamera()` | Crea un archivo temporal en `cacheDir`, obtiene su `Uri` con `FileProvider.getUriForFile(...)` (necesario porque Android no deja pasar rutas de archivo directas a otra app) y se lo entrega a la cámara. |
| `notificationPermissionLauncher` | En Android 13+ pide `POST_NOTIFICATIONS` cuando el usuario **enciende** el interruptor de notificaciones. |
| `setupBiometricSwitch()` | Si el teléfono no tiene huella, deshabilita el interruptor y cambia su texto. Al **activarlo**, primero pide la huella; solo si la confirma llama `viewModel.onBiometricToggled(true)`. Si cancela o falla, devuelve el interruptor a apagado. Al desactivarlo, llama directo `onBiometricToggled(false)`. |
| `bindUser(user, biometricEnabled)` | Rellena nombre, notificaciones y huella **una sola vez** (`formBound`) para no pisar lo que el usuario esté escribiendo. La foto se decodifica del Base64 con Glide solo si cambió (`shownPhoto`). |

### 8.5 Huella: `BiometricAuthManager` (`…/presentation/biometric/`)

Un `object` (no necesita instancia) que envuelve `BiometricPrompt`.
- `isBiometricAvailable(context)`: `true` si hay un sensor y al menos una huella registrada (`BIOMETRIC_STRONG`).
- `showBiometricPrompt(fragment, title, subtitle, onSuccess, onCancel, onError)`: muestra el diálogo del sistema con el botón "Usar contraseña". Distingue tres resultados:
  - reconocida → `onSuccess`
  - el usuario tocó "Usar contraseña" o cerró el diálogo → `onCancel`
  - falla real (sensor bloqueado, demasiados intentos) → `onError(mensaje)`
- **Un intento fallido suelto no cierra el diálogo**: Android muestra "no reconocida" y deja reintentar, así que no se avisa nada aquí.

### 8.6 `DesignSystemFragment` (HU02)

Solo infla `fragment_design_system.xml`: un catálogo con la paleta, la tipografía y los componentes. No tiene lógica. Sirve para revisar el diseño en modo claro y oscuro.

### 8.7 `…/service/FixNowMessagingService.kt` (HU03)

Servicio de FCM registrado en el manifiesto. `onNewToken` y `onMessageReceived` por ahora solo escriben en Logcat. En HU08 guardará el token del técnico en Firestore y mostrará las notificaciones.

---

## 9. Flujos de punta a punta

Cada flujo indica **archivo → método** en orden.

### 9.1 Registrarse

1. `RegisterFragment` (botón "Registrarme") → `RegisterViewModel.register(email, password, confirmPassword)`
2. Valida los campos. Si algo falla, emite `ShowMessage` y termina.
3. `RegisterUserUseCase.invoke` → `AuthRepository.register` → **`AuthRepositoryImpl.register`**
4. Firebase crea la cuenta y envía el correo de verificación.
5. De vuelta en `RegisterUserUseCase`: como fue `Success`, llama `ProfileRepository.createInitialProfile` → **`ProfileRepositoryImpl.createInitialProfile`** crea `users/{uid}` con rol `cliente`.
6. `RegisterViewModel` emite `NavigateToHome`; `RegisterFragment` navega con `action_register_to_home`.
7. `HomeFragment.onStart` → `refresh()`: el correo aún no está verificado, así que se muestra el aviso.

### 9.2 Iniciar sesión con correo

1. `LoginFragment` → `LoginViewModel.login(...)` → `LoginUserUseCase` → `AuthRepositoryImpl.login`.
2. Si Firebase rechaza (contraseña mal), `ErrorMapper` produce "Correo o contraseña incorrectos" → `ShowMessage` → Snackbar.
3. Si acepta → `NavigateToHome`.

### 9.3 Sesión persistente (cerrar y abrir la app)

1. `MainActivity.onCreate` → `MainViewModel.startAtHome`.
2. `GetCurrentUserUseCase` → `AuthRepositoryImpl.currentUser` → Firebase recuerda la sesión en el teléfono.
3. Si hay sesión y no hay huella activada → arranca en Home.

### 9.4 Recuperar contraseña

1. Escribes el correo y tocas "¿Olvidaste tu contraseña?" → `LoginViewModel.onForgotPassword(email)`.
2. Valida el correo → `SendPasswordResetUseCase` → `AuthRepositoryImpl.sendPasswordReset`.
3. Firebase envía el correo; el ViewModel emite un `ShowMessage` de éxito.

### 9.5 Verificación de correo

1. Se envía al registrarse (`AuthRepositoryImpl.register`).
2. Mientras no verifiques, `HomeFragment` muestra la tarjeta con "Reenviar correo" → `HomeViewModel.resendVerification()`.
3. Al verificar y volver a la app, `HomeFragment.onStart` → `refresh()` → `RefreshCurrentUserUseCase` → `AuthRepositoryImpl.refreshCurrentUser()` (hace `reload()`) → `isEmailVerified = true` → la tarjeta se oculta.

### 9.6 Foto de perfil

1. `ProfileFragment`: "Tomar foto" → `cameraPermissionLauncher` → `launchCamera()`.
2. La cámara guarda la foto en el archivo temporal → `takePictureLauncher` → `ProfileViewModel.savePhoto(uri)`.
3. `SaveProfilePhotoUseCase` → **`ProfileRepositoryImpl.saveProfilePhoto`** → `ImageEncoder.toSquareBase64` (256 px, Base64) → Firestore `fotoBase64`.
4. `ProfileViewModel` actualiza `uiState.user.fotoBase64` → `ProfileFragment.bindUser` decodifica y la muestra circular con Glide.

### 9.7 Activar la huella

1. `ProfileFragment`: el usuario enciende el interruptor → `showBiometricPrompt(...)` con título "Activar huella dactilar".
2. Si la confirma → `ProfileViewModel.onBiometricToggled(true)` → `SetBiometricEnabledUseCase`.
3. El caso de uso guarda **primero** en `BiometricSettingsRepositoryImpl` (cifrado, en el teléfono) y **luego** en Firestore (`biometriaActivada`).

### 9.8 Entrar con la huella

1. Se abre la app → `MainViewModel.startAtHome = false` (hay sesión pero la huella está activada) → `LoginFragment`.
2. `LoginViewModel.biometricShortcutEnabled = true`; `LoginFragment.setupBiometricShortcut(autoPrompt = true)` pide la huella.
3. Huella correcta → `LoginViewModel.onBiometricSuccess()` → `NavigateToHome`.
4. Si toca "Usar contraseña" (`onCancel`), no pasa nada y puede escribir correo y contraseña.

---

## 10. Mapa de dependencias

Quién necesita a quién (de la pantalla hacia Firebase):

| ViewModel | Casos de uso que usa | Repositorios (interfaz) | Implementación → servicio |
|-----------|----------------------|--------------------------|----------------------------|
| `MainViewModel` | GetCurrentUser, IsBiometricEnabled | Auth, BiometricSettings | AuthRepositoryImpl → FirebaseAuth · BiometricSettingsRepositoryImpl → EncryptedSharedPreferences |
| `LoginViewModel` | LoginUser, SendPasswordReset, GetCurrentUser, IsBiometricEnabled | Auth, BiometricSettings | ídem |
| `RegisterViewModel` | RegisterUser | Auth + Profile | AuthRepositoryImpl → FirebaseAuth · ProfileRepositoryImpl → Firestore |
| `HomeViewModel` | GetCurrentUser, RefreshCurrentUser, GetUserProfile, ResendVerificationEmail, Logout | Auth, Profile | ídem |
| `ProfileViewModel` | GetCurrentUser, GetUserProfile, UpdateProfile, SaveProfilePhoto, SetBiometricEnabled, IsBiometricEnabled | Auth, Profile, BiometricSettings | ídem + `ImageEncoder` |

Resumen de capas y qué está **prohibido** importar:

| Capa | Puede importar | No puede importar |
|------|----------------|--------------------|
| `presentation` | `domain`, `core`, Android | `data`, Firebase |
| `domain` | Kotlin, corrutinas | Android, Firebase, `data` |
| `data` | `domain`, `core`, Firebase, Android | `presentation` |

Se puede comprobar con búsqueda de texto: en `domain` no debe aparecer `import android` ni `import com.google.firebase`.

---

## 11. Recursos: layouts, tema, navegación y manifiesto

### Layouts (`res/layout/`) y los IDs que usa cada Fragment

| Layout | Fragment | IDs que se usan desde el código |
|--------|----------|----------------------------------|
| `activity_main.xml` | `MainActivity` | `navHostFragment` (contenedor de pantallas) |
| `fragment_login.xml` | `LoginFragment` | `inputEmail`, `inputPassword`, `buttonLogin`, `buttonBiometricLogin`, `textForgotPassword`, `textGoToRegister`, `progressLogin` |
| `fragment_register.xml` | `RegisterFragment` | `inputEmail`, `inputPassword`, `inputConfirmPassword`, `buttonRegister`, `textGoToLogin`, `progressRegister` |
| `fragment_home.xml` | `HomeFragment` | `textGreeting`, `cardVerifyEmail`, `buttonResendVerification`, `buttonProfile`, `buttonDesignSystem`, `buttonLogout` |
| `fragment_profile.xml` | `ProfileFragment` | `imageProfilePhoto`, `buttonTakePhoto`, `textEmail`, `inputName`, `switchNotifications`, `switchBiometric`, `buttonSaveProfile`, `progressProfile` |
| `fragment_design_system.xml` | `DesignSystemFragment` | (ninguno) |

Regla de ViewBinding: `fragment_login.xml` → clase `FragmentLoginBinding`; el ID `button_login` se accede como `binding.buttonLogin`.

### Tema (HU02)
- `values/colors.xml` (claro) y `values-night/colors.xml` (oscuro) definen **los mismos nombres** (`fixnow_primary`, `fixnow_on_primary`…). Android elige el archivo según el modo del teléfono. Azul = primario, verde azulado = secundario, ámbar = terciario.
- `values/themes.xml`: `Theme.FixNow`, que hereda de `Theme.Material3.DayNight.NoActionBar`. Conecta cada color con un rol de Material 3 (`colorPrimary`, `colorSurface`…) y fija los estilos por defecto de botón, campo de texto y card.
- `values/type.xml`: ajustes de tipografía. `values/styles.xml`: variantes de botón (`Widget.FixNow.Button.Tonal`, `.Outlined`, `.Text`), campo de texto y card.
- En los layouts, los colores se leen con `?attr/colorPrimary`, `?attr/colorOnSurface`, etc. **Nunca** con un código hexadecimal: así el modo oscuro funciona solo.

### Navegación (`res/navigation/nav_graph.xml`)

| Acción | Va de → a | Nota |
|--------|-----------|------|
| `action_login_to_register` | Login → Register | |
| `action_login_to_home` | Login → Home | `popUpTo nav_graph` inclusive: borra el historial, para que "atrás" no vuelva al login |
| `action_register_to_home` | Register → Home | ídem |
| `action_home_to_profile` | Home → Profile | |
| `action_home_to_design_system` | Home → DesignSystem | |
| `action_home_to_login` | Home → Login | ídem (cierre de sesión) |

### `AndroidManifest.xml`
- Permisos: `INTERNET`, `CAMERA`, `USE_BIOMETRIC`, `POST_NOTIFICATIONS`.
- `android:name=".FixNowApp"` (activa Hilt), `android:allowBackup="false"` (evita que un respaldo restaure las preferencias cifradas con una llave que ya no existe).
- `MainActivity` como pantalla de entrada.
- `FixNowMessagingService` para FCM.
- `FileProvider` (autoridad `${applicationId}.fileprovider`) que lee `res/xml/file_paths.xml` (la carpeta `cache/`). Es la puerta para que la cámara escriba la foto.

### Gradle
- `app/build.gradle.kts` aplica los plugins: `kotlin-kapt` + `com.google.dagger.hilt.android` (Hilt genera código al compilar), `com.google.gms.google-services` (lee `google-services.json`).
- `viewBinding = true` genera las clases `...Binding`; `buildConfig = true` habilita `BuildConfig.DEBUG`.
- Las versiones de plugins están en el `build.gradle.kts` de la raíz.

---

## 12. Firestore: datos y reglas

Una sola colección por ahora:

```
users/{uid}                   ← uid = ID de Firebase Auth
   ├─ uid: String
   ├─ nombre: String
   ├─ email: String
   ├─ fotoBase64: String      ← miniatura 256×256 en Base64
   ├─ rol: "cliente" | "tecnico" | "admin"
   ├─ notificacionesActivas: Boolean
   ├─ biometriaActivada: Boolean
   └─ creadoEn: Long          ← milisegundos
```

Reglas (`firebase/firestore.rules`):
- Cada persona **solo lee y escribe su propio documento** (`request.auth.uid == uid`).
- Al crear, el rol solo puede ser `cliente`; al actualizar, **no se puede cambiar el rol**. Así nadie se auto-asigna admin o técnico desde la app. El cambio de rol (HU13) lo hará un admin desde la consola o una Cloud Function.
- `fotoBase64` no puede superar 200 000 caracteres.
- Cualquier otra colección está cerrada hasta que una historia la necesite.

---

## 13. Decisiones de diseño y trampas conocidas

| Detalle | Por qué está así |
|---------|------------------|
| **`uiState` y `events` separados** | Si un mensaje fuera parte del estado, al girar el teléfono volvería a aparecer. Con `Channel`, se consume una sola vez. |
| **`collectWhenStarted`** | Solo escucha con la pantalla visible; evita trabajo (y errores) cuando la app está en segundo plano. |
| **`by viewModels()` en cada Fragment** | Cada pantalla tiene su ViewModel; se destruye cuando sales de ella definitivamente. |
| **`isPressed` en los interruptores** | `setOnCheckedChangeListener` también se dispara cuando el **código** cambia `isChecked` (al cargar el perfil). `button.isPressed` es `true` solo si el usuario tocó el interruptor, así evitamos pedir permisos o escribir en Firestore sin querer. |
| **`formBound` en `ProfileFragment`** | El formulario se rellena una sola vez; si no, un cambio de estado (por ejemplo, terminar de subir la foto) borraría lo que el usuario está escribiendo. |
| **Huella confirmada antes de activarla** | Comprueba que de verdad hay una huella registrada y evita activar la opción "a ciegas". |
| **`SetBiometricEnabledUseCase` guarda local primero** | Lo que decide el login es el valor del teléfono; que Firestore falle (sin internet) no debe impedir usar la huella. |
| **Foto como `String` (Uri) hacia `domain`** | `android.net.Uri` es de Android y `domain` no puede depender de Android. |
| **Interfaces de repositorio** | Permiten cambiar la implementación (por ejemplo, volver a Storage si algún día hay plan Blaze) sin tocar pantallas ni casos de uso. |
| **`allowBackup="false"`** | Restaurar un respaldo con `EncryptedSharedPreferences` en otro teléfono lo deja ilegible y puede provocar errores. |
| **La huella es un "candado de pantalla", no criptografía** | Solo protege el paso por la pantalla de login; la sesión de Firebase ya está en el dispositivo. Es la solución estándar para esta historia, pero conviene saberlo si te preguntan por seguridad. |

---

## 14. Quiero cambiar X: ¿dónde toco?

| Quiero… | Archivo(s) |
|---------|------------|
| Cambiar el color principal | `res/values/colors.xml` y `res/values-night/colors.xml` (`fixnow_primary` y los que lo acompañan) |
| Cambiar la esquina redondeada de los botones | `res/values/styles.xml` (`cornerRadius`) |
| Pedir contraseña de más de 6 caracteres | `RegisterViewModel` (constante `MIN_PASSWORD_LENGTH`) |
| Cambiar un mensaje de error de Firebase | `data/mapper/ErrorMapper.kt` |
| Cambiar un mensaje de validación | El ViewModel correspondiente (`LoginViewModel`, `RegisterViewModel`, `ProfileViewModel`) |
| Exigir correo verificado para entrar | `LoginUserUseCase`: comprobar `result.user.isEmailVerified` |
| Cambiar el tamaño de la foto de perfil | `ImageEncoder` (`DEFAULT_SIZE_PX`, `JPEG_QUALITY`) |
| Añadir un campo al perfil (ej. teléfono) | `domain/model/User.kt` → contrato y `ProfileRepositoryImpl` → ViewModel → layout y `ProfileFragment.bindUser` |
| Añadir una pantalla | Fragment + ViewModel en `presentation/`, layout en `res/layout/`, destino y acciones en `nav_graph.xml` |
| Añadir una tabla/colección de Firestore | Modelo + interfaz en `domain`, implementación en `data`, `@Binds` en `RepositoryModule`, reglas en `firestore.rules` |
| Que la app inicie siempre en Login | `MainViewModel.startAtHome` |
| Mostrar notificaciones con la app abierta | `FixNowMessagingService.onMessageReceived` |
| Cambiar el nombre del paquete | *Refactor → Rename* del paquete, y `namespace`/`applicationId` en `app/build.gradle.kts` (además del paquete en Firebase) |

---

## 15. Limitaciones actuales

Para que no te sorprendan:

- `ObserveAuthStateUseCase` está creado pero **ningún ViewModel lo usa todavía**.
- El login **no bloquea** correos sin verificar; solo se muestra el aviso en Home.
- Los mensajes de texto están escritos directamente en español dentro del código y los layouts (no en `strings.xml`); si algún día necesitas varios idiomas, hay que migrarlos.
- Solo hay un test (`ValidatorsTest`). Los ViewModels son fáciles de probar porque dependen de casos de uso, pero aún no tienen pruebas.
- Todos los usuarios nacen como `cliente`; todavía no hay forma de registrarse como técnico (llega con HU13).
- La foto de perfil es una miniatura en Base64 dentro de Firestore: sirve para imágenes pequeñas, no para fotos grandes (ver README).
- No se pudo compilar ni ejecutar el proyecto en el entorno donde se generó; si Android Studio marca errores al sincronizar, hay que corregirlos.

---

## 16. Orden sugerido para leer el código

1. `res/navigation/nav_graph.xml` y `presentation/main/MainActivity.kt`: para ver el mapa de pantallas y el arranque.
2. `presentation/auth/LoginFragment.kt` → `LoginViewModel.kt`: el molde que repiten todas las pantallas.
3. `domain/usecase/auth/LoginUserUseCase.kt` → `domain/repository/AuthRepository.kt` → `data/repository/AuthRepositoryImpl.kt`: cómo baja una acción hasta Firebase.
4. `di/RepositoryModule.kt` y `di/FirebaseModule.kt`: por qué todo "se conecta solo".
5. `core/util/SafeCall.kt` y `data/mapper/ErrorMapper.kt`: cómo se manejan los errores.
6. `presentation/profile/ProfileFragment.kt` y `data/local/ImageEncoder.kt`: lo más elaborado (cámara, permisos, procesamiento de imagen).
7. `presentation/biometric/BiometricAuthManager.kt` y `data/local/BiometricSettingsRepositoryImpl.kt`: la huella.
8. `res/values/themes.xml` y `res/layout/fragment_design_system.xml`: el sistema de diseño.
