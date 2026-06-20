package com.switchtune.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.switchtune.app.core.platform.MusicPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "switchtune_prefs")

/** User state stored locally on the device. No remote storage, no PII. */
data class UserPreferences(
    val preferredPlatform: MusicPlatform?,
    val onboardingComplete: Boolean,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PREFERRED_PLATFORM = stringPreferencesKey("preferred_platform")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            preferredPlatform = prefs[Keys.PREFERRED_PLATFORM]
                ?.let { name -> runCatching { MusicPlatform.valueOf(name) }.getOrNull() },
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
        )
    }

    suspend fun setPreferredPlatform(platform: MusicPlatform) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_PLATFORM] = platform.name
        }
    }

    suspend fun completeOnboarding(platform: MusicPlatform) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_PLATFORM] = platform.name
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }
}
