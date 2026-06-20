package com.switchtune.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchtune.app.core.IncomingLinkBus
import com.switchtune.app.core.linkparser.LinkParser
import com.switchtune.app.core.platform.MusicPlatform
import com.switchtune.app.data.odesli.OdesliRepository
import com.switchtune.app.data.prefs.UserPreferencesRepository
import com.switchtune.app.domain.model.ResolveResult
import com.switchtune.app.domain.model.ResolvedSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val incomingLinkBus: IncomingLinkBus,
    private val odesliRepository: OdesliRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Empty)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    // Cache to avoid re-querying Odesli on config changes or preference toggles.
    private var cachedToken: Long? = null
    private var cachedResolved: ResolvedSong? = null
    private var lastParsed: LinkParser.ParsedLink? = null
    private var lastPreferred: MusicPlatform? = null

    init {
        viewModelScope.launch {
            combine(
                incomingLinkBus.incomingText,
                userPreferencesRepository.preferences,
            ) { incoming, prefs -> incoming to prefs.preferredPlatform }
                .collectLatest { (incoming, preferred) -> handle(incoming, preferred) }
        }
    }

    /** Manually submit clipboard text (used by the "Paste link" button). */
    fun submitClipboardText(text: String?) {
        incomingLinkBus.post(text, IncomingLinkBus.Source.CLIPBOARD)
    }

    private suspend fun handle(incoming: IncomingLinkBus.IncomingText?, preferred: MusicPlatform?) {
        lastPreferred = preferred

        if (incoming == null) {
            _uiState.value = ResultUiState.Empty
            return
        }

        // Edge case: clipboard/shared text that is not a music link -> empty state, no error.
        val parsed = LinkParser.parse(incoming.text)
        if (parsed == null) {
            _uiState.value = ResultUiState.Empty
            return
        }

        // Reuse cached result when only the preferred platform changed or on rotation.
        if (incoming.token == cachedToken) {
            cachedResolved?.let {
                _uiState.value = ResultUiState.Loaded(it, preferred)
                return
            }
        }

        lastParsed = parsed
        resolve(parsed, preferred, token = incoming.token)
    }

    private suspend fun resolve(parsed: LinkParser.ParsedLink, preferred: MusicPlatform?, token: Long?) {
        _uiState.value = ResultUiState.Resolving
        _uiState.value = when (val result = odesliRepository.resolve(parsed.url, parsed.sourcePlatform)) {
            is ResolveResult.Success -> {
                cachedToken = token
                cachedResolved = result.resolved
                ResultUiState.Loaded(result.resolved, preferred)
            }

            ResolveResult.NoNetwork -> ResultUiState.Failed(FailureReason.NO_NETWORK)
            ResolveResult.NotFound -> ResultUiState.Failed(FailureReason.NOT_FOUND)
            ResolveResult.RateLimited -> ResultUiState.Failed(FailureReason.RATE_LIMITED)
            is ResolveResult.Error -> ResultUiState.Failed(FailureReason.GENERIC)
        }
    }

    /** Retries the last resolution (used by error states). */
    fun retry() {
        val parsed = lastParsed ?: return
        viewModelScope.launch { resolve(parsed, lastPreferred, token = cachedToken) }
    }
}
