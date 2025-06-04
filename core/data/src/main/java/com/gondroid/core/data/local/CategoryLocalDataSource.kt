package com.gondroid.core.data.local


const val CLASS_A = "CLASE A"
const val CLASS_B = "CLASE B"

val categoriesLocalDataSource = listOf(
    /**
     * Class licence A
     */
    Category(
        id = "1",
        title = "CLASE A - CATEGORIA I",
        category = "A-I",
        classType = CLASS_A,
        description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "2",
        title = "CLASE A - CATEGORIA II-A",
        category = "A-IIa",
        classType = CLASS_A,
        description = "Los mismos que A-1 y también carros oficiales de transporte de pasajeros como Taxis, Buses, Ambulancias y Transporte Interprovincial. Primero debes obtener la Licencia A-I",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "3",
        title = "CLASE A - CATEGORIA II-B",
        category = "A-IIb",
        classType = CLASS_A,
        description = "Los mismos que A-1, A-IIa y también Microbuses de hasta 16 asientos y 4 toneladas de peso bruto y Minibuses hasta 33 asientos y 7 toneladas de peso bruto. Primero debes obtener la Licencia A-I",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "4",
        title = "CLASE A - CATEGORIA III-A",
        category = "A-IIIa",
        classType = CLASS_A,
        description = " Los mismos que A-I, A-IIa y AIIb y también vehiculos con más de 6 toneladas como omnibuses urbanos, interurbanos, panorámicos y articulados. Primero debes obtener la Licencia A-I",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "5",
        title = "CLASE A - CATEGORIA III-B",
        category = "A-IIIb",
        classType = CLASS_A,
        description = "Los mismos que A-I, A-IIa y A-IIb (pero no A-IIIa) y también vehículos de chasis cabinado, remolques, gruas, cargobus, plataforma, baranda y volquetes. Primero debes obtener la Licencia A-I.",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "6",
        title = "CLASE A - CATEGORIA III-C",
        category = "A-IIIc",
        classType = CLASS_A,
        description = "Los mismos que A-I, A-IIa, AIIb, A-IIIa y A-IIIb. Primero debes obtener la Licencia A-I.",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    /**
     * Class licence B
     */
    Category(
        id = "7",
        title = "CLASE B - CATEGORIA I",
        category = "B-I",
        classType = CLASS_B,
        description = "Vehículos no motorizados de 3 ruedas (triciclos) para transporte público especial de pasajeros.",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "8",
        title = "CLASE B - CATEGORIA II-A",
        category = "B-IIa",
        classType = CLASS_B,
        description = "Bicimotos para transportar pasajeros o mercancías.",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "9",
        title = "CLASE B - CATEGORIA II-B",
        category = "B-IIb",
        classType = CLASS_B,
        description = "Los mismos que B-IIa y también Motocicletas (2 ruedas) o Motocicletas con Sidecar (3 ruedas) para transportar pasajeros o mercancías.",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
    Category(
        id = "10",
        title = "CLASE B - CATEGORIA II-C",
        category = "B-IIc",
        classType = CLASS_B,
        description = "Los mismos que B-IIa y B-IIb y también Mototaxis y Trimotos (3 ruedas) destinadas al transporte de pasajeros",
        image = "a",
        pdf = "CLASE_A_I.pdf"
    ),
)