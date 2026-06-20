package com.switchtune.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchtune.app.core.platform.MusicPlatform
import com.switchtune.app.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _selected = MutableStateFlow(MusicPlatform.SPOTIFY)
    val selected: StateFlow<MusicPlatform> = _selected.asStateFlow()

    fun select(platform: MusicPlatform) {
        _selected.value = platform
    }

    fun confirm(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.completeOnboarding(_selected.value)
            onComplete()
        }
    }
}
