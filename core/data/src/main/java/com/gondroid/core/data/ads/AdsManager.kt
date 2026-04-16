package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context

interface AdsManager {

    /**
     * Precarga el intersticial para tenerlo listo. Idempotente: si ya hay uno cargado o
     * una carga en curso, no hace nada.
     */
    fun preloadPdfInterstitial(context: Context)

    /**
     * Devuelve true si según la regla de frecuencia (cada 3 descargas) corresponde mostrar
     * el intersticial en esta descarga.
     */
    suspend fun shouldShowPdfInterstitial(): Boolean

    /**
     * Muestra el intersticial precargado. Si está cargado, al cerrarse invoca [onDismiss].
     * Si no está cargado o falla al mostrar, invoca [onDismiss] inmediatamente. Nunca bloquea.
     */
    fun showPdfInterstitial(activity: Activity, onDismiss: () -> Unit)

    /**
     * Incrementa el contador persistente de descargas. Se llama ANTES de decidir si mostrar
     * el intersticial.
     */
    suspend fun recordPdfDownload()
}
