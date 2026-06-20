package com.switchtune.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries the latest incoming text (from the Share Sheet or the clipboard) from
 * [com.switchtune.app.MainActivity] to the result screen's ViewModel. Kept as a
 * singleton so a new share Intent while the app is open is picked up.
 */
@Singleton
class IncomingLinkBus @Inject constructor() {

    private val _incomingText = MutableStateFlow<IncomingText?>(null)
    val incomingText: StateFlow<IncomingText?> = _incomingText.asStateFlow()

    fun post(text: String?, source: Source) {
        if (text.isNullOrBlank()) return
        _incomingText.value = IncomingText(text, source, System.nanoTime())
    }

    fun consume() {
        _incomingText.value = null
    }

    enum class Source { SHARE, CLIPBOARD }

    data class IncomingText(val text: String, val source: Source, val token: Long)
}
