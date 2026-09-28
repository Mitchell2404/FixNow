# FixNow – Proyecto base (HU01 a HU06)

Proyecto Android completo (Kotlin + MVVM + Firebase + Material 3). Reemplaza el zip anterior de HU04–06: ya incluye esas historias integradas sobre la base de HU01–03.

| HU | Qué contiene |
|----|--------------|
| **HU01** Arquitectura MVVM | Capas `data` / `domain` / `presentation`, ViewModels con `StateFlow`, patrón Repository con interfaces, Hilt. Explicado en `docs/ARQUITECTURA.md`. |
| **HU02** Sistema de diseño | Tema Material 3 con azul como primario, secundario y terciario, modo claro y oscuro, tipografía y estilos de botón, input y card. Hay una pantalla de muestra (Home → "Ver sistema de diseño"). |
| **HU03** Firebase | SDK de Auth, Firestore y Cloud Messaging integrado; reglas de seguridad en `firebase/firestore.rules`. **Sin Firebase Storage** (ver la nota más abajo): todo funciona en el plan gratuito, sin tarjeta. |
| **HU04** Registro y login | Correo y contraseña, verificación de correo, recuperar contraseña, sesión persistente. |
| **HU05** Perfil | Nombre, foto con la cámara (miniatura guardada en Firestore), preferencia de notificaciones. |
| **HU06** Huella | `BiometricPrompt`, activar/desactivar desde el perfil, contraseña como alternativa. |

> No pude compilar el proyecto en mi entorno (no tiene el SDK de Android). Está revisado a mano y con verificaciones automáticas de XML e IDs, pero si Android Studio marca algún error al sincronizar, pásamelo tal cual y lo corregimos.

## Paso a paso para correrlo

### 1. Requisitos
- Android Studio (versión estable reciente). Usa su JDK integrado (17).
- Un teléfono o emulador con Android 8.0 (API 26) o superior.

### 2. Crear el proyecto en Firebase
1. Entra a [console.firebase.google.com](https://console.firebase.google.com) → **Agregar proyecto** → nombre `FixNow`.
2. En el proyecto: **Agregar app → Android**. En "Nombre del paquete" escribe exactamente `com.fixnow.app`.
3. Descarga **`google-services.json`** y colócalo en la carpeta **`app/`** (al lado de `app/build.gradle.kts`). Sin este archivo el proyecto no compila.
4. En **Authentication → Sign-in method**, activa **Correo electrónico/contraseña**.
5. En **Firestore Database → Crear base de datos** (modo producción).
6. Pega las reglas de seguridad: Firestore → pestaña *Reglas* → contenido de `firebase/firestore.rules` → *Publicar*.

Deja el proyecto en el plan **Spark** (gratuito). No actives Storage ni pases a Blaze: no hace falta.

### 3. Abrir en Android Studio
1. **File → Open** y elige la carpeta `FixNow`.
2. Espera a que termine el *Gradle Sync*. Si sugiere actualizar Gradle o el plugin de Android, puedes aceptar; **no actualices Kotlin ni Hilt a ciegas** porque van casados entre sí.
3. Presiona **Run ▶**.

### 4. Cómo comprobar cada historia (criterios de aceptación)

**HU01 – Arquitectura**
- [ ] Los paquetes son `data`, `domain`, `presentation` (más `di`, `core`, `service`).
- [ ] Ningún Fragment tiene lógica de negocio: solo pinta el estado y reenvía clics al ViewModel.
- [ ] Los ViewModels exponen `StateFlow` y se inyectan con Hilt.
- [ ] Los casos de uso dependen de interfaces (`domain/repository`), no de Firebase.

**HU02 – Diseño**
- [ ] Inicia sesión → Home → **Ver sistema de diseño**: se ven paleta, tipografía y componentes.
- [ ] Activa el modo oscuro del teléfono: los colores cambian solos.
- [ ] Los colores se cambian en un solo lugar: `res/values/colors.xml` (claro) y `res/values-night/colors.xml` (oscuro).

**HU03 – Firebase**
- [ ] La app abre sin errores de Firebase en Logcat.
- [ ] Al registrarte aparece el usuario en *Authentication* y su documento en *Firestore → users*.
- [ ] Notificaciones: con la app en debug, busca en Logcat el filtro `FixNowFCM` y copia el token. En Firebase Console → *Messaging* → *Enviar mensaje de prueba* pégalo. Ojo: con la app abierta Android no muestra la notificación; ciérrala (segundo plano) para verla. Mostrarla con la app abierta llega en HU08.

**HU04 – Registro/login**
- [ ] Registrarte → llega el correo de verificación (revisa spam). En Home aparece el aviso "Verifica tu correo".
- [ ] Verifica desde el correo, vuelve a la app: el aviso desaparece.
- [ ] Cierra la app por completo y ábrela: sigues dentro (sesión persistente).
- [ ] "¿Olvidaste tu contraseña?" (con el correo escrito) envía el correo de recuperación.

**HU05 – Perfil**
- [ ] Home → Mi perfil → **Tomar foto** (pide permiso de cámara) → la foto aparece redonda.
- [ ] Cambia el nombre → **Guardar cambios** → mensaje "Perfil actualizado".
- [ ] Reabre el perfil: nombre y foto siguen ahí. En *Firestore → users → tu documento* verás el campo `fotoBase64` (un texto largo: es la foto reducida).

**HU06 – Huella**
- [ ] En el emulador: *Settings → Security → Fingerprint* y registra una huella (en el emulador, el botón "Touch sensor" de Extended controls simula el toque).
- [ ] Perfil → activa **Iniciar sesión con huella** → confirma con la huella.
- [ ] Cierra y abre la app: pide la huella al entrar.
- [ ] Si tocas "Usar contraseña", puedes ingresar con correo y contraseña.
- [ ] En un dispositivo sin huella, el interruptor aparece deshabilitado.

## Estructura

```
app/src/main/java/com/fixnow/app/
├── FixNowApp.kt              @HiltAndroidApp
├── core/util/                utilidades (safeCall, validadores, extensiones)
├── domain/                   REGLAS DE NEGOCIO (no depende de Android ni Firebase)
│   ├── model/                User, AuthUser, AuthResult
│   ├── repository/           interfaces (contratos)
│   └── usecase/              una acción = una clase
├── data/                     DATOS (aquí vive Firebase)
│   ├── repository/           implementaciones con Firebase
│   ├── local/                preferencia cifrada de la huella + ImageEncoder (miniatura de la foto)
│   └── mapper/               errores de Firebase → mensajes en español
├── di/                       módulos de Hilt
├── presentation/             PANTALLAS
│   ├── main/  auth/  home/  profile/  designsystem/  biometric/
└── service/                  FixNowMessagingService (FCM)
firebase/                     reglas de Firestore
docs/ARQUITECTURA.md          cómo fluye una acción de punta a punta
```

## Sobre Firebase Storage (y por qué no se usa)

Cloud Storage for Firebase ya no funciona en el plan gratuito Spark: exige el plan Blaze, que pide vincular una tarjeta de pago (aunque el uso dentro de la cuota gratuita no se cobra). Por eso la foto de perfil se guarda así:

1. Se toma con la cámara.
2. `ImageEncoder` la endereza, la recorta en cuadrado y la reduce a 256 × 256 px (JPEG, unos 20–40 KB).
3. Se guarda como texto Base64 en el campo `fotoBase64` del documento del usuario en Firestore, que sí es gratuito en Spark. Las reglas rechazan fotos de más de ~200 KB.

Como el resto del código depende de la interfaz `ProfileRepository`, si algún día tienes Blaze solo se cambia `ProfileRepositoryImpl.saveProfilePhoto`; pantallas, ViewModels y casos de uso no se tocan.

**Para tu informe o exposición:** el backlog dice "Firebase Storage" en HU03 y HU05. Puedes justificar el cambio con esto: *"Firebase Storage requiere el plan de pago Blaze; se optó por una miniatura Base64 en Firestore para mantener el proyecto en el plan gratuito"*. El curso pide usar herramientas de Firebase, y Auth, Firestore y FCM cumplen eso.

**Límite a tener presente:** este método sirve para imágenes pequeñas. Para las fotos del problema del cliente en HU07 (más grandes) hay dos caminos gratuitos sin tarjeta: reducirlas a ~800 px y guardarlas Base64 en su propio documento (unos 100–150 KB, bien bajo el límite de 1 MiB), o usar un servicio externo con plan gratuito como Cloudinary y guardar solo el enlace en Firestore. Lo vemos cuando llegues a esa historia.

## Cosas que debes saber

- **El rol no se puede auto-asignar.** Las reglas de Firestore obligan a que todos nazcan como `cliente` y no dejan cambiar `rol` desde la app. Cuando llegues a HU13 (validación de técnicos), el cambio a `tecnico` lo hará un admin desde una Cloud Function o la consola.
- **La huella desbloquea la sesión que ya existe** en el teléfono; no guarda la contraseña. Si cierras sesión, hay que entrar una vez con correo y contraseña.
- **El login no bloquea correos sin verificar**; solo se muestra el aviso en Home. Si tu criterio de aceptación exige bloquearlo, se agrega en `LoginUserUseCase` con `user.isEmailVerified`.
- **Los textos** de mensajes están escritos directo en español en el código para simplificar. Si más adelante quieres soporte de varios idiomas, se pasan a `strings.xml`.
- **Otro nombre de paquete:** usa *Refactor → Rename* sobre el paquete `com.fixnow.app` y cambia `namespace` y `applicationId` en `app/build.gradle.kts`. El paquete en Firebase y el de `google-services.json` deben coincidir.
- **Trabajo en equipo:** `google-services.json` no está en `.gitignore` a propósito, para que tus compañeros lo reciban con `git pull`. La `google-services.json` no contiene secretos de servidor; lo que protege tus datos son las reglas de seguridad.

## Siguiente paso sugerido

HU07 (solicitud de servicio del cliente): sigue la receta de `docs/ARQUITECTURA.md`, sección "Cómo agregar una funcionalidad nueva".
