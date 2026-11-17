package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context

interface AdsManager {

    /**
     * Precarga el anuncio con recompensa de la descarga de PDF. Idempotente: si ya hay uno
     * cargado o una carga en curso, no hace nada. No pide nada a usuarios premium ni sin
     * consentimiento UMP.
     */
    fun preloadPdfRewarded(context: Context)

    /**
     * True si hay un anuncio con recompensa listo para mostrar. Si es false (premium, sin
     * consentimiento, sin red o sin inventario) la descarga sigue sin anuncio: nunca se bloquea
     * un PDF por falta de anuncio.
     */
    fun isPdfRewardedReady(): Boolean

    /**
     * Muestra el anuncio con recompensa y avisa el resultado con [onResult]:
     * [RewardedOutcome.EARNED] si lo vio completo, [RewardedOutcome.SKIPPED] si lo cerró antes
     * y [RewardedOutcome.UNAVAILABLE] si no había anuncio o falló al mostrarse.
     */
    fun showPdfRewarded(activity: Activity, onResult: (RewardedOutcome) -> Unit)

    /**
     * Precarga el intersticial de evaluación para tenerlo listo. Idempotente.
     */
    fun preloadEvaluationInterstitial(context: Context)

    /**
     * Devuelve true si según la regla de frecuencia (cada 3 evaluaciones) corresponde mostrar
     * el intersticial al iniciar esta evaluación.
     */
    suspend fun shouldShowEvaluationInterstitial(): Boolean

    /**
     * Muestra el intersticial de evaluación precargado. Si no está cargado o falla, invoca
     * [onDismiss] inmediatamente. Nunca bloquea. `adWasShown` es true sólo si el anuncio se vio.
     */
    fun showEvaluationInterstitial(activity: Activity, onDismiss: (adWasShown: Boolean) -> Unit)

    /**
     * Incrementa el contador persistente de evaluaciones iniciadas. Se llama ANTES de decidir
     * si mostrar el intersticial.
     */
    suspend fun recordEvaluationStart()
}

/** Cómo terminó un anuncio con recompensa. */
enum class RewardedOutcome {
    /** El usuario vio el anuncio completo y ganó la recompensa. */
    EARNED,

    /** El usuario cerró el anuncio antes de ganar la recompensa. */
    SKIPPED,

    /** No había anuncio que mostrar, o falló al mostrarse: el usuario no vio nada. */
    UNAVAILABLE
}
