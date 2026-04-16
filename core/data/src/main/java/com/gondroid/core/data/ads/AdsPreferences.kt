package com.gondroid.core.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AdsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    open val pdfDownloadCount: Flow<Int> by lazy {
        dataStore.data.map { prefs -> prefs[PDF_DOWNLOAD_COUNT] ?: 0 }
    }

    open suspend fun incrementPdfDownloadCount(): Int {
        var newValue = 0
        dataStore.edit { prefs ->
            val current = prefs[PDF_DOWNLOAD_COUNT] ?: 0
            newValue = current + 1
            prefs[PDF_DOWNLOAD_COUNT] = newValue
        }
        return newValue
    }

    open suspend fun currentPdfDownloadCount(): Int = pdfDownloadCount.first()

    private companion object {
        val PDF_DOWNLOAD_COUNT = intPreferencesKey("pdf_download_count")
    }
}
