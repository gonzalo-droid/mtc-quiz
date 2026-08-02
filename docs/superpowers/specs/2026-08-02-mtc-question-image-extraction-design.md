# Extracción de preguntas e imágenes desde los balotarios PDF

## Contexto

`app/src/main/assets/pdf/` contiene 9 balotarios oficiales del MTC (PDF).
`app/src/main/assets/json/` contiene su transcripción a JSON, en distintos
grados de avance. Ninguno tiene todavía las imágenes (señales de tránsito,
etc.) asociadas a sus preguntas.

## Mapeo PDF ↔ JSON ↔ examId (fijo)

| PDF | JSON | examId | category |
|---|---|---|---|
| CLASE_A_I | a1_questions.json | `a1` | AI |
| CLASE_A_IIA | a2a_questions.json | `a2a` | AIIA |
| CLASE_A_IIB | a2b_questions.json | `a2b` | AIIB |
| CLASE_A_IIIA | a3a_questions.json | `a3a` | AIIIA |
| CLASE_A_IIIB | a3b_questions.json | `a3b` | AIIIB |
| CLASE_A_IIIC | a3c_questions.json | `a3c` | AIIIC |
| CLASE_B_IIA | b2a_questions.json | `b2a` | BIIA |
| CLASE_B_IIB | b2b_questions.json | `b2b` | BIIB |
| CLASE_B_IIC | b2c_questions.json | `b2c` | BIIC |

`a1_questions_test.json` es un fixture de prueba (5 preguntas), no un
balotario — se mueve fuera de `assets/` (ver recomendaciones).

## Estado verificado al iniciar

- **Texto**: `b2b` y `b2c` vacíos (`"data": []`) → 100% pendientes. Los otros
  7 ya tienen contenido (200-339 preguntas) y se dan por completos (decisión
  del usuario).
- **Imágenes**: ningún JSON tiene el campo `imagens` → pendiente en los 9.

## Extracción de texto (b2b, b2c)

El PDF es una tabla (Nº | Materia | Categoría | Tema | Descripción |
Alt.1-4 | Respuesta) con celdas que envuelven en varias líneas. Parsear
`pdftotext -layout` es frágil por el ajuste de línea variable.

**Enfoque**: `pdftohtml -xml` da cada fragmento de texto con su posición
exacta (top/left/width/height), en el mismo sistema de coordenadas que las
imágenes embebidas. Un script Python:

1. Detecta los bordes de columna a partir de la fila de encabezado de la
   primera página.
2. Detecta el inicio de cada pregunta: el número en la columna "Nº" (fuente
   distinta a la del encabezado, x dentro del rango de esa columna).
3. Agrupa el texto entre un número de pregunta y el siguiente, por columna,
   para reconstruir `topic`, `title`, las 4 `options` (el PDF ya incluye el
   prefijo "a) " etc. en el texto) y `answer`.
4. `section` se actualiza cuando aparece un encabezado de sección ("MATERIAS
   GENERALES" / "MATERIAS ESPECÍFICAS"); `category` es fija por examen
   (tabla de arriba) — igual que ya hacen los JSON existentes, aunque la
   columna "Clase/Categoría" del PDF diga "Todas".

Después de generar el JSON, se revisa visualmente una muestra de páginas
contra el PDF renderizado para detectar errores del parser antes de darlo
por bueno.

## Extracción y asociación de imágenes (los 9 exámenes)

`pdftohtml -xml` también extrae los archivos de imagen embebidos con su
posición. Filtrado de logos: imágenes con el mismo tamaño/posición repetidas
en casi todas las páginas (p.ej. el logo del MTC) se descartan
automáticamente.

Para cada imagen no-logo: se ubica en qué banda vertical (pregunta) cae →
se copia a `assets/images/` como `q{n}_{letra}_{examId}.png` (letra `a`,
`b`, `c`... si la pregunta tiene varias imágenes, convertidas a PNG/WebP) →
se actualiza `imagens` en esa pregunta del JSON.

Formato del campo: `"imagens": ["q1_a_a1", "q1_b_a1"]` (array de nombres,
sin extensión).

## Cambio en el modelo Kotlin (obligatorio)

`core/domain/.../Question.kt` tiene un campo `image: String?` sin usar
(ningún JSON lo llena, ninguna UI lo lee). El parser (`Json.Default`, sin
`ignoreUnknownKeys`) revienta si el JSON trae una clave desconocida, así que
hay que reemplazar ese campo muerto por:

```kotlin
val imagens: List<String> = emptyList()
```

## La skill

`.claude/skills/mtc-question-extractor/`:
- `SKILL.md` con el flujo y las convenciones de nombres.
- Script de parseo XML→JSON de preguntas.
- Script de extracción/asociación de imágenes.
- Sin archivo de estado propio: lo pendiente se calcula comparando el PDF
  contra el JSON actual (ids, conteo, presencia de `imagens`).

## Alcance aprobado (incluye recomendaciones)

1. Extracción de texto para `b2b` y `b2c`.
2. Extracción y asociación de imágenes para los 9 exámenes.
3. Wire de imágenes en la UI: `Coil` ya es dependencia en
   evaluation/questionreview pero no se usa; `CardQuestion` pinta siempre un
   fondo estático — se conecta para mostrar `imagens` cuando existan.
4. Test de validación de esquema para todos los `assets/json/*.json`
   (`answer` dentro de `options`, exactamente 4 `options`, ids únicos y sin
   huecos).
5. Mover `a1_questions_test.json` fuera de `assets/` (a recursos de test).
6. Usar WebP en vez de PNG para las imágenes de señales (menor tamaño de
   APK).
