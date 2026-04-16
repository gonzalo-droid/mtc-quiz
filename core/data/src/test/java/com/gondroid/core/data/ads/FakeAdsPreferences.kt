package com.gondroid.core.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAdsPreferences : AdsPreferences(dataStore = ThrowingDataStore) {
    private val _pdfCount = MutableStateFlow(0)
    override val pdfDownloadCount: Flow<Int> get() = _pdfCount

    override suspend fun incrementPdfDownloadCount(): Int {
        _pdfCount.value += 1
        return _pdfCount.value
    }

    override suspend fun currentPdfDownloadCount(): Int = _pdfCount.value

    private val _evalCount = MutableStateFlow(0)
    override val evaluationStartCount: Flow<Int> get() = _evalCount

    override suspend fun incrementEvaluationStartCount(): Int {
        _evalCount.value += 1
        return _evalCount.value
    }

    override suspend fun currentEvaluationStartCount(): Int = _evalCount.value
}

private object ThrowingDataStore : DataStore<Preferences> {
    override val data get() = error("FakeAdsPreferences no debe tocar DataStore real")
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        error("FakeAdsPreferences no debe tocar DataStore real")
}
