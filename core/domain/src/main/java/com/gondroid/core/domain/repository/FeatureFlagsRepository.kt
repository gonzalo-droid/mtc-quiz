package com.gondroid.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Interruptores que se cambian a distancia (Firebase Remote Config) sin publicar otra versión.
 * Cada flag tiene un valor por defecto local, así la app funciona sin red y antes del primer fetch.
 */
interface FeatureFlagsRepository {

    /**
     * True si se ofrece comprar Premium (ícono de la Home, fila "Hazte Premium", upsells tras un
     * anuncio y el paywall). Solo oculta la *oferta*: no cambia [PremiumRepository.isPremiumFlow],
     * así que quien ya pagó sigue siendo premium y sin anuncios aunque valga false.
     */
    val isPremiumOfferEnabled: StateFlow<Boolean>

    /**
     * Descarga y activa los valores remotos y actualiza los flags. Nunca lanza: si falla (sin red,
     * throttling) se quedan los últimos valores activados o los por defecto. Llamar al iniciar la app.
     */
    suspend fun refresh()
}
