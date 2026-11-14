package com.gondroid.core.data.featureflags

import com.gondroid.core.domain.repository.FeatureFlagsRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * [FeatureFlagsRepository] respaldado por Firebase Remote Config.
 *
 * Los parámetros se crean en Firebase Console con el nombre exacto de [KEY_PREMIUM_ENABLED].
 * Mientras no exista el parámetro, no haya red o no haya terminado el primer fetch, se usa el
 * valor por defecto local ([DEFAULTS]).
 */
@Singleton
class RemoteConfigFeatureFlagsRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : FeatureFlagsRepository {

    companion object {
        /** Nombre exacto del parámetro booleano en Firebase Console. */
        const val KEY_PREMIUM_ENABLED = "premium_enabled"
        const val DEFAULT_PREMIUM_ENABLED = true

        /** Valores por defecto locales que se registran en Remote Config con `setDefaultsAsync`. */
        val DEFAULTS: Map<String, Any> = mapOf(KEY_PREMIUM_ENABLED to DEFAULT_PREMIUM_ENABLED)
    }

    // Arranca con el último valor activado (Remote Config lo persiste entre sesiones), así un
    // "false" ya descargado se respeta desde el primer frame, antes del fetch de esta sesión.
    private val _isPremiumOfferEnabled = MutableStateFlow(readPremiumEnabled())
    override val isPremiumOfferEnabled: StateFlow<Boolean> = _isPremiumOfferEnabled.asStateFlow()

    override suspend fun refresh() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Remote Config: fetch failed, keeping the last activated/default values")
        }
        _isPremiumOfferEnabled.value = readPremiumEnabled()
    }

    /**
     * `setDefaultsAsync` es asíncrono: si se lee antes de que termine, Remote Config devuelve el
     * valor estático (false para un booleano), lo que ocultaría Premium sin que nadie lo pidiera.
     * Por eso un valor de origen estático cae al default propio.
     */
    private fun readPremiumEnabled(): Boolean {
        val value = remoteConfig.getValue(KEY_PREMIUM_ENABLED)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            DEFAULT_PREMIUM_ENABLED
        } else {
            value.asBoolean()
        }
    }
}
