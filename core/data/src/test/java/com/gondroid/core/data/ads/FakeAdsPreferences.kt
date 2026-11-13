package com.gondroid.core.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAdsPreferences : AdsPreferences(dataStore = ThrowingDataStore) {
    private val _pdfDownloadCount = MutableStateFlow(0)
    override val pdfDownloadCount: Flow<Int> get() = _pdfDownloadCount

    override suspend fun incrementPdfDownloadCount(): Int {
        _pdfDownloadCount.value += 1
        return _pdfDownloadCount.value
    }

    override suspend fun currentPdfDownloadCount(): Int = _pdfDownloadCount.value

    private val _evaluationStartCount = MutableStateFlow(0)
    override val evaluationStartCount: Flow<Int> get() = _evaluationStartCount

    override suspend fun incrementEvaluationStartCount(): Int {
        _evaluationStartCount.value += 1
        return _evaluationStartCount.value
    }

    override suspend fun currentEvaluationStartCount(): Int = _evaluationStartCount.value
}

private object ThrowingDataStore : DataStore<Preferences> {
    override val data get() = error("FakeAdsPreferences no debe tocar DataStore real")
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        error("FakeAdsPreferences no debe tocar DataStore real")
}
