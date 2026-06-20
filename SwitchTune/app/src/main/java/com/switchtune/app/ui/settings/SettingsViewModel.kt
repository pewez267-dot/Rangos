package com.switchtune.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchtune.app.core.platform.MusicPlatform
import com.switchtune.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val preferredPlatform: StateFlow<MusicPlatform?> =
        userPreferencesRepository.preferences
            .map { it.preferredPlatform }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPreferred(platform: MusicPlatform) {
        viewModelScope.launch { userPreferencesRepository.setPreferredPlatform(platform) }
    }
}
