# 🚗 mtc-quiz

Aplicación Android para practicar el examen de reglas de tránsito del Ministerio de Transportes y Comunicaciones del Perú. Está diseñada como un cuestionario interactivo con preguntas de selección múltiple basadas en el temario oficial.

---

## 🌟 Objetivos del proyecto

El principal objetivo de este proyecto fue **practicar y consolidar el uso de arquitectura modular en Android**, aplicando principios como Clean Architecture, inyección de dependencias, testing y separación clara de responsabilidades.

---

## 🛠️ Tecnologías utilizadas

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: Clean Architecture + MVVM
- **Persistencia local**: Room (SQLite)
- **Networking**: Retrofit2 + Moshi
- **Inyección de dependencias**: Hilt
- **Firebase**: Firebase Authentication (login y registro)
- **Coroutines + Flow**: para asincronía y manejo de estados reactivos
- **Testing**: JUnit4, MockK

---

## 🚀 Funcionalidades principales

- 📋 Visualización de preguntas y opciones
- ✅ Evaluación automática de respuestas
- 📊 Registro de resultados
- 👤 Login y autenticación con Firebase
- 🌙 Soporte para dark mode
- 📡 Modo offline con Room

---

## 🏗️ Arquitectura general

La aplicación sigue **Clean Architecture**, separando responsabilidades en tres capas:

```
presentation/   ← UI + ViewModels (Compose)
domain/         ← Casos de uso + modelos puros
data/           ← Repositorios + fuentes (Room, Retrofit)
```

Además, se implementa una **estructura modular**:

```
mtc-quiz/
├── app/                  # Módulo principal de ejecución
├── core/                 # Código común reutilizable
├── feature-login/        # Funcionalidad de autenticación
├── feature-quiz/         # Lógica y vistas del cuestionario
├── data/                 # Implementaciones de repositorios y fuentes
├── domain/               # Interfaces y lógica de negocio
└── design-system/        # Temas, estilos y componentes visuales
```

Cada módulo tiene responsabilidad única y puede ser probado o mantenido de forma independiente.

---

## 🎨 Diseño del sistema

- **Un solo **``** centralizado** en `design-system`
- Uso de tipografía y colores personalizados
- Componentes UI desacoplados, reutilizables
- Navegación modular usando `NavHost` por feature
- Inyección de dependencias distribuida por módulos (Hilt + `@InstallIn`)

---

## 🧰 Instalación y configuración

1. Clona el proyecto:

   ```bash
   git clone https://github.com/gonzalo-droid/mtc-quiz.git
   cd mtc-quiz
   ```

2. Agrega tu archivo `google-services.json` en `app/` para habilitar Firebase.

3. Sincroniza dependencias y compila desde Android Studio.

4. Ejecuta en un dispositivo o emulador compatible (API 26+).

---

## 🤝 Contribuciones

¡Contribuciones son bienvenidas!

Puedes ayudar de las siguientes formas:

- Reportando errores o sugerencias en [Issues](https://github.com/gonzalo-droid/mtc-quiz/issues)
- Enviando Pull Requests con mejoras
- Proponiendo nuevas funcionalidades

---

**Hecho con 💻 por **[**@gonzalo-droid**](https://github.com/gonzalo-droid)
