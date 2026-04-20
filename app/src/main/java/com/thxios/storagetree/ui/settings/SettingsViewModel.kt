package com.thxios.storagetree.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thxios.storagetree.data.preferences.PreferencesRepository
import com.thxios.storagetree.domain.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val appSettings = preferencesRepository.appSettings

    fun setShowInstalledApps(value: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowInstalledApps(value) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch { preferencesRepository.setSortOrder(sortOrder) }
    }
}
