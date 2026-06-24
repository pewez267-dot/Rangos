package com.switchtune.app.domain.model

/**
 * Outcome of a link-resolution attempt. Every edge case required by the spec is
 * represented explicitly so the UI can react without ever crashing.
 */
sealed interface ResolveResult {
    data class Success(val resolved: ResolvedSong) : ResolveResult

    /** Odesli responded but found no matches for this link. */
    data object NotFound : ResolveResult

    /** Odesli rate limit reached (HTTP 429). */
    data object RateLimited : ResolveResult

    /** No internet connectivity / network failure. */
    data object NoNetwork : ResolveResult

    /** Any other unexpected failure. */
    data class Error(val cause: Throwable? = null) : ResolveResult
}
