package com.switchtune.app.ui.result

import com.switchtune.app.core.platform.MusicPlatform
import com.switchtune.app.domain.model.ResolvedSong

/** All states the main screen can be in. Designed so the UI never crashes. */
sealed interface ResultUiState {

    /** No (valid) music link detected — the normal empty state. */
    data object Empty : ResultUiState

    /** Resolving the link through Odesli. */
    data object Resolving : ResultUiState

    /**
     * A song was resolved. [preferred] is the user's optional target platform
     * (null when none is set); the screen shows the full list of available
     * services regardless, with the preferred one highlighted first.
     */
    data class Loaded(
        val resolved: ResolvedSong,
        val preferred: MusicPlatform?,
    ) : ResultUiState

    /** A recoverable failure with a specific reason. */
    data class Failed(val reason: FailureReason) : ResultUiState
}

enum class FailureReason { NO_NETWORK, NOT_FOUND, RATE_LIMITED, GENERIC }
