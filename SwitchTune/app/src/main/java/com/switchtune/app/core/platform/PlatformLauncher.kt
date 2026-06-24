package com.switchtune.app.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * Centralises all the Intent logic for opening destination apps. Handles the
 * required edge cases: native deep link, web fallback, search fallback, and
 * Play Store install. Never throws to callers — returns a [LaunchResult].
 */
object PlatformLauncher {

    enum class LaunchResult { OPENED_APP, OPENED_WEB, FAILED }

    fun isInstalled(context: Context, platform: MusicPlatform): Boolean = runCatching {
        context.packageManager.getLaunchIntentForPackage(platform.packageName) != null
    }.getOrDefault(false)

    /**
     * Opens the resolved song in the destination app. Strategy:
     * 1. Native URI (e.g. "spotify:track:..") targeted at the app, if present.
     * 2. Web URL forced into the destination app package, if installed.
     * 3. Web URL in the browser as a last resort.
     */
    fun openLink(context: Context, platform: MusicPlatform, webUrl: String, nativeUri: String?): LaunchResult {
        if (!nativeUri.isNullOrBlank()) {
            if (startView(context, nativeUri.toUri(), platform.packageName)) return LaunchResult.OPENED_APP
        }
        if (isInstalled(context, platform)) {
            if (startView(context, webUrl.toUri(), platform.packageName)) return LaunchResult.OPENED_APP
        }
        return if (startView(context, webUrl.toUri(), targetPackage = null)) {
            LaunchResult.OPENED_WEB
        } else {
            LaunchResult.FAILED
        }
    }

    /** Opens a plain web URL in the browser (web fallback when app not installed). */
    fun openWeb(context: Context, url: String): Boolean =
        startView(context, url.toUri(), targetPackage = null)

    /**
     * Search fallback: opens a search for the song in the destination app (or
     * its website if not installed). Used when Odesli has no direct match.
     */
    fun openSearch(context: Context, platform: MusicPlatform, query: String): LaunchResult {
        val encoded = Uri.encode(query)
        val searchUrl = when (platform) {
            MusicPlatform.SPOTIFY -> "https://open.spotify.com/search/$encoded"
            MusicPlatform.APPLE_MUSIC -> "https://music.apple.com/search?term=$encoded"
            MusicPlatform.YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$encoded"
            MusicPlatform.YOUTUBE -> "https://www.youtube.com/results?search_query=$encoded"
            MusicPlatform.AMAZON_MUSIC -> "https://music.amazon.com/search/$encoded"
            MusicPlatform.DEEZER -> "https://www.deezer.com/search/$encoded"
            MusicPlatform.TIDAL -> "https://listen.tidal.com/search?q=$encoded"
        }
        if (isInstalled(context, platform) && startView(context, searchUrl.toUri(), platform.packageName)) {
            return LaunchResult.OPENED_APP
        }
        return if (startView(context, searchUrl.toUri(), null)) LaunchResult.OPENED_WEB else LaunchResult.FAILED
    }

    /** Opens the destination app's Play Store page so the user can install it. */
    fun openPlayStore(context: Context, platform: MusicPlatform): Boolean {
        val market = "market://details?id=${platform.packageName}".toUri()
        if (startView(context, market, "com.android.vending")) return true
        return startView(context, "https://play.google.com/store/apps/details?id=${platform.packageName}".toUri(), null)
    }

    private fun startView(context: Context, uri: Uri, targetPackage: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (targetPackage != null) setPackage(targetPackage)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
