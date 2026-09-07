# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue (aproximadamente) [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). El versionado **no** es SemVer estricto: `versionName` sigue un esquema aproximado `MAJOR.MINOR.PATCH` a criterio del mantenedor, y `versionCode` es un entero secuencial que sube en 1 en cada release — ambos se bumpean manualmente en `gradle/libs.versions.toml` (`projectVersionName`/`projectVersionCode`). No hay automatización de versionado todavía (ver `README.md` → sección Versionado).

## [Sin publicar]

### Added
- 39 preguntas nuevas en el banco B-IIC (ids 205–243). `CLASE_B_IIC.pdf` trae dos tablas numeradas y solo se había extraído la primera, así que faltaba entera la sección de materias específicas. La fila Nº28 de esa tabla queda fuera a propósito: en el PDF no tiene cuarta opción ni respuesta.
- `audit_questions.py`: audita los nueve bancos contra su balotario — estructura, presencia de cada título y opción en el PDF, y la letra de respuesta leída directamente de la columna RESPUESTA. No reutiliza los scripts de extracción, para que un fallo del extractor no pueda esconderse en su propia auditoría.
- `audit_images.py`: comprueba píxel a píxel que cada `.webp` sea la imagen que imprime su fila del PDF, y no la de otra pregunta.

### Fixed
- 111 campos de los bancos que no coincidían con su balotario. B-IIA concentra la mayoría: 13 respuestas que contradecían al PDF —"No respetar los límites de velocidad" estaba marcada como falta Grave cuando el balotario dice Muy Grave, y el límite en caminos rurales como 40 km/h cuando dice 60—, más 56 opciones y 5 títulos.
- Opciones truncadas en A-IIB y A-IIIA: a la opción c de la 85 le faltaba la segunda mitad ("para los asientos posteriores no hay restricción"), y a la a de la 90 y la 159 el final de la frase.
- Opción d de la pregunta 8 en B-IIB y B-IIC: arrastraba pegado el texto de la opción c de otra pregunta.
- Opción d de la 148 en B-IIA: decía que los elementos añadidos a la placa deben ser "del mismo color", cuando el balotario dice justo lo contrario.

## [1.2.3] - 2026-09-06 (versionCode 8)

### Added
- Cobertura de tests para el flujo completo de compra premium (`PremiumRepositoryImpl`: `loadAvailablePlans`, `launchSubscription`, listener de compras, restauración) y para `ConfigurationScreenViewModel`/`MainViewModel`, que no tenían tests.
- `ktlintCheck` corriendo por primera vez en CI (el plugin estaba declarado pero nunca aplicado a ningún módulo).
- Cache de Gradle y badge de estado de CI en el README.
- Workflow de deploy manual a Google Play (Internal Testing) — `workflow_dispatch` únicamente, con spec y plan de implementación documentados.
- Snackbar con acción "Abrir" al descargar el PDF del temario, para abrirlo directamente en un visor externo.
- Zoom básico (pellizcar) en el visor de PDF, por página, con doble tap para resetear.
- Opción "Compartir app" en Configuración (comparte el link de la ficha de Play Store).
- Opción "Política de privacidad" en Configuración, con pantalla WebView propia (`PrivacyScreen`).
- Informe de cobertura HTML unificado con Kover, que agrega los 27 módulos en `app` (`./gradlew :app:koverHtmlReport`). Es informativo: no hay umbral mínimo que rompa el build.
- Firebase Performance Monitoring en release y LeakCanary en debug, para detectar regresiones de arranque y fugas de memoria.
- El APK debug de cada corrida de CI se publica como artefacto (`app-debug`, 14 días de retención), para poder probar un PR sin compilarlo localmente.

### Changed
- Google Play Billing Library `7.1.1` → `9.1.0` (requisito de Google Play, deadline 2026-08-30). `BillingClient` ahora se inyecta vía `BillingClientFactory`/Hilt en vez de construirse inline, habilitando los tests de compra.
- CI simplificado: de `fastlane android beta` (nunca había pasado, requería credenciales inexistentes) a `ktlintCheck` + `test` + `assembleDebug` directo con Gradle — sin necesidad de ningún GitHub Secret.
- "Calificar app" ya no dispara el In-App Review API de Google desde el menú de Configuración — redirige directo a la ficha de Play Store. Google desaconseja disparar ese API desde un botón de menú, y el diálogo no se mostraba casi nunca por límite de cuota.
- Descarga del PDF migrada a `MediaStore` en Android 10+ (antes usaba `File` directo a la carpeta pública de Descargas, lo que fallaba con `EACCES` bajo scoped storage).
- URL de "Términos y condiciones" corregida (apuntaba a un placeholder). Los links legales de la pantalla Premium ahora abren las mismas pantallas WebView internas de Configuración en vez de un navegador externo — antes ambos links de Premium apuntaban por error a la misma URL placeholder.
- Etiqueta "Personaliza tu configuración y sigue estudiando" reemplazada por "Ajustes de evaluación", que describe la pantalla en vez de acompañar.
- Las preguntas de una evaluación ahora se barajan (`QuizRepositoryImpl`, `shuffled()`) antes de recortar al número configurado. Antes siempre salían las primeras N del banco y en el mismo orden, así que repetir una categoría daba exactamente el mismo examen.
- CI: `test` y `assembleDebug` corren en una sola invocación de Gradle en vez de dos pasos separados — cada `./gradlew` reconfigura los ~27 módulos, así que dividirlos pagaba esa fase dos veces. Además se activaron `org.gradle.parallel` y `org.gradle.caching`.

### Fixed
- Botón "Saltar" del onboarding no respondía al toque — el `HorizontalPager` quedaba encima del botón en el z-order de Compose y absorbía el tap.
- Buscador de texto dentro del visor de PDF removido (solo funcionaba en API 35+ y complicaba el visor sin aportar mucho).
- Banner de AdMob renderizado detrás de la barra de navegación del sistema en dispositivos con navegación de 3 botones (`BannerAdSlot` no consumía `WindowInsets.navigationBars`).
- Botón "Nosotros" en Configuración ocultado — navegaba a una pantalla ("About") que nunca se implementó.
- `.gitignore`: `/build` estaba anclado a la raíz y no excluía los `build/` de cada módulo.
- La evaluación numeraba cada pregunta con su `id` del banco de la categoría, que se veía desordenado desde que las preguntas se barajan; ahora usa la posición dentro de la evaluación.
- El pipeline fallaba con `null cannot be cast to non-null type kotlin.String` al quitar las credenciales de firma de `gradle.properties`: el bloque `signingConfigs` se evalúa en la fase de configuración de *cualquier* tarea (ktlint y tests incluidos), y las leía como no-nulas. Ahora la config de firma release solo se crea si están las cuatro.

### Removed
- Credenciales de firma release (`MTC_KEYSTORE_PATH`, `MTC_KEYSTORE_PASSWORD`, `MTC_KEY_ALIAS`, `MTC_KEY_PASSWORD`) fuera de `gradle.properties`, que está versionado en un repo público. Ahora se leen de `~/.gradle/gradle.properties` o de variables de entorno.

## [1.2.2] - 2026-08-07 (versionCode 7)

### Added
- Ícono de la app actualizado.

### Changed
- Imágenes de categoría redimensionadas.

### Fixed
- `PremiumRepository.isPremiumFlow` tipado explícitamente como `StateFlow<Boolean>` (no `Flow`), eliminando un cast inseguro en `AdsManagerImpl`.

## [1.2.1] - 2026-08-06 (versionCode 6)

### Added
- Suscripción **mensual** (además de la anual existente), con precios reales de Play Console mostrados en `PremiumScreen`.
- `AnalyticsManager` para eventos del funnel de compra premium (paywall visto, suscripción iniciada, compra completada/cancelada/fallida, restauración).
- `QuestionAnswerCard`/`AnswerOptionRow` — nueva superficie unificada de pregunta+respuestas, con estados conscientes del tema.
- `BannerAdSlot` extraído como componente compartido; banner también visible en Detail (antes solo Home).
- Lista de categorías en Home rediseñada: de carrusel horizontal (`HorizontalPager`, una tarjeta visible a la vez) a lista vertical scrolleable con las 9 categorías visibles.
- Código de colores por categoría según clase de licencia.

### Changed
- **Arquitectura de billing**: `BillingManager` reemplazado por `PremiumRepository` (estado) + `BillingLauncher` (lanzar compra) — separación para que consumidores de solo-lectura (ad-gating) no dependan de la superficie completa de `BillingManager`.
- `CoroutineScope` inyectado en `PremiumRepositoryImpl` en vez de construirse internamente, habilitando tests con `TestScope`.
- `QuestionsScreen` y `EvaluationScreen` migradas a `QuestionAnswerCard`.

### Fixed
- Botón de suscripción quedaba inactivo cuando la lista de planes llegaba vacía; condición de carrera en el mensaje de restauración de compra.
- Prefijos de letra duplicados en opciones de preguntas de las categorías B2B/B2C.
- `contentDescription`s de respuesta correcta/incorrecta traducidos a español.
- Parpadeo de un frame en el tinte de `QuestionImage`.
- Import roto de `TestDataModule` que bloqueaba la compilación de `androidTest`.

### Removed
- `CardAnswer`, `CardQuestion` — superseded por `QuestionAnswerCard`/`AnswerOptionRow`.

## [1.2.0] - 2026-08-05 (versionCode 5)

### Added
- **Upgrade a Android 16 (API 36)** — `compileSdk`/`targetSdk` 36, Robolectric 4.16.1.
- Pipeline de extracción automatizada del banco de preguntas desde los PDFs del balotario oficial del MTC (parser de layout por columnas/bandas, extracción de categorías B2B/B2C).
- Imágenes reales por categoría en Home y Detail, e imágenes de preguntas renderizadas vía Coil en `CardQuestion`.
- Manejo de estado offline con reintento en pantallas WebView.

### Fixed
- Numerosas correcciones de datos del banco de preguntas: opciones en blanco, texto de opción no coincidente con su imagen de señal, fugas de texto (título/fundamento) entre preguntas contiguas, orden incorrecto de imágenes a/b/c/d.
- Categoría B-I (no implementada) removida del flujo; `pathJson` de categorías de licencia B corregido.

### Removed
- `Category.image`, `CardTypeEnum` y sus drawables huérfanos — reemplazados por el enfoque de imágenes vía Coil.

## [1.1.0] - 2026-04-16 (versionCode 3–4)

### Added
- Integración de Google AdMob: banner en Home, intersticiales antes de evaluación y antes de descargar PDF (límite 1-de-cada-3).
- Pantalla de suscripción Premium (UI con gradiente, selector de plan, ad-gating).
- Pantalla de estadísticas de progreso, con desglose por categoría.
- Racha diaria de estudio (streak) con badge visual en Home.
- Historial de evaluaciones, con preguntas falladas persistidas por evaluación.
- Pantalla de trámites/tarifas asociadas (WebView).
- In-App Review API con fallback a la ficha de Play Store.
- Toggle de modo oscuro/claro persistido.
- Rediseño de paleta de colores y tipografía.

### Changed
- Configuración reorganizada: Estadísticas e Historial movidos de Home a la sección Configuración.

## [1.0.1] - 2025-12-30 (versionCode 2)

### Added
- Descarga de PDF del temario por categoría.
- Ícono de la app.

### Changed
- Flujo de login deshabilitado temporalmente (acceso libre a la app, sin autenticación obligatoria).
- Dependencias de autenticación movidas al módulo `core`.

### Fixed
- Reglas de ProGuard para el build de release.

## [1.0.0] - 2025-06-08 (versionCode 1)

Versión inicial: arquitectura multi-módulo Clean Architecture, plugins de convención Gradle, autenticación con Google Sign-In, design system y capa de presentación compartida (`presentation-ui`).

> Desarrollo previo a este release (scaffolding inicial del proyecto) no está desglosado aquí — ver `git log` para el historial completo.
