# Arquitectura de FixNow (HU01)

## La idea en una frase

Cada capa hace **una sola cosa** y solo conoce a la de abajo. Así, cambiar Firebase por otra base de datos no obliga a tocar las pantallas, y las pantallas no tienen lógica escondida.

```
┌──────────────────────────────────────────────────────────┐
│ presentation   Fragment  ──►  ViewModel                  │  lo que ve el usuario
│                (pinta)        (estado + decisiones de UI)│
├──────────────────────────────────────────────────────────┤
│ domain         UseCase  ──►  Repository (INTERFAZ)       │  reglas de negocio
│                (una acción)   (contrato)                 │  (Kotlin puro, sin Firebase)
├──────────────────────────────────────────────────────────┤
│ data           RepositoryImpl  ──►  Firebase             │  de dónde salen los datos
└──────────────────────────────────────────────────────────┘
```

La flecha significa "usa a". `domain` no conoce a `data`: define la interfaz `AuthRepository` y `data` la implementa con `AuthRepositoryImpl`. Hilt conecta ambas en `di/RepositoryModule.kt`.

## Qué es cada pieza

| Pieza | Responsabilidad | Ejemplo |
|-------|-----------------|---------|
| **Fragment** | Mostrar el estado y avisar de los clics. Nada más. | `LoginFragment` |
| **ViewModel** | Guardar el estado de la pantalla (`StateFlow`), validar entradas simples y llamar a casos de uso. Sobrevive a girar el teléfono. | `LoginViewModel` |
| **UseCase** | Una acción del negocio, con nombre de verbo. | `LoginUserUseCase`, `RegisterUserUseCase` |
| **Repository (interfaz)** | Contrato: qué operaciones existen. | `domain/repository/AuthRepository` |
| **RepositoryImpl** | Cómo se hacen realmente (Firebase). | `data/repository/AuthRepositoryImpl` |
| **Model** | Datos de la app. | `User`, `AuthUser` |

## Estado vs. eventos

En los ViewModels hay dos canales distintos, y conviene no mezclarlos:

- **`uiState` (StateFlow):** *cómo está la pantalla ahora* (¿cargando?, ¿qué usuario?). Si la pantalla se recrea, se vuelve a pintar igual.
- **`events` (Channel):** cosas que pasan **una sola vez** (navegar, mostrar un mensaje). Si fueran parte del estado, al girar el teléfono el mensaje saldría otra vez.

## Un ejemplo de punta a punta: iniciar sesión

1. El usuario toca **Iniciar sesión** → `LoginFragment` llama `viewModel.login(email, password)`.
2. `LoginViewModel` valida el correo y la contraseña. Pone `isLoading = true`.
3. Llama a `LoginUserUseCase`, que llama a `AuthRepository.login(...)` (interfaz).
4. Hilt entrega `AuthRepositoryImpl`, que llama a Firebase Auth. Si falla, `ErrorMapper` convierte el error técnico en un mensaje en español.
5. El resultado vuelve como `AuthResult`. El ViewModel manda un evento `NavigateToHome` o `ShowMessage`.
6. El Fragment lo recibe y navega o muestra el mensaje.

## Cómo agregar una funcionalidad nueva (receta)

Ejemplo: HU07, "el cliente crea una solicitud de servicio".

1. **Modelo** en `domain/model/`: `ServiceRequest` (con valores por defecto para Firestore).
2. **Contrato** en `domain/repository/`: `interface ServiceRequestRepository { suspend fun create(...): Result<String> }`.
3. **Casos de uso** en `domain/usecase/request/`: `CreateServiceRequestUseCase`.
4. **Implementación** en `data/repository/`: `ServiceRequestRepositoryImpl` con Firestore, usando `safeCall(Throwable::toFriendlyMessage) { ... }`.
5. **Hilt:** agrega un `@Binds` en `di/RepositoryModule.kt`.
6. **Pantalla** en `presentation/request/`: `NewRequestViewModel` (con `uiState` y `events`) y `NewRequestFragment`, más su layout `fragment_new_request.xml`.
7. **Navegación:** agrega el fragment y la acción en `res/navigation/nav_graph.xml`.
8. **Reglas de Firestore:** abre solo lo necesario en `firebase/firestore.rules` (por defecto todo está cerrado).

## Reglas para mantener el código limpio

- Un Fragment nunca importa nada de `firebase` ni de `data`.
- Un ViewModel nunca importa `data`; solo casos de uso.
- `domain` no importa `android.*` (por eso la Uri de la foto viaja como `String`).
- Un caso de uso hace **una** cosa. Si necesita dos repositorios, está bien (como `RegisterUserUseCase`).
- Los colores y tamaños se leen del tema (`?attr/colorPrimary`), nunca se escriben a mano en un layout: así el modo oscuro funciona solo.
