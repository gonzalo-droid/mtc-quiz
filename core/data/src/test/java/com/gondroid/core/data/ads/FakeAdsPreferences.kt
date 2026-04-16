package com.gondroid.core.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAdsPreferences : AdsPreferences(dataStore = ThrowingDataStore) {
    private val _count = MutableStateFlow(0)
    override val pdfDownloadCount: Flow<Int> get() = _count

    override suspend fun incrementPdfDownloadCount(): Int {
        _count.value += 1
        return _count.value
    }

    override suspend fun currentPdfDownloadCount(): Int = _count.value
}

private object ThrowingDataStore : DataStore<Preferences> {
    override val data get() = error("FakeAdsPreferences no debe tocar DataStore real")
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        error("FakeAdsPreferences no debe tocar DataStore real")
}
