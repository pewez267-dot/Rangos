package com.switchtune.app.ui.result

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.switchtune.app.R
import com.switchtune.app.core.platform.MusicPlatform
import com.switchtune.app.core.platform.PlatformLauncher
import com.switchtune.app.domain.model.ResolvedSong
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onOpenSettings: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notInstalledTemplate = stringResource(R.string.not_installed)

    fun handleLaunch(result: PlatformLauncher.LaunchResult, platform: MusicPlatform) {
        if (result == PlatformLauncher.LaunchResult.FAILED) {
            scope.launch {
                snackbarHostState.showSnackbar(notInstalledTemplate.format(platform.displayName))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (val s = state) {
                ResultUiState.Empty -> EmptyState(
                    onPaste = { viewModel.submitClipboardText(readClipboardText(context)) },
                )

                ResultUiState.Resolving ->
                    com.switchtune.app.ui.common.FullScreenLoading(stringResource(R.string.resolving))

                is ResultUiState.Loaded -> LoadedState(
                    resolved = s.resolved,
                    preferred = s.preferred,
                    onLaunch = ::handleLaunch,
                )

                is ResultUiState.Failed -> FailedState(reason = s.reason, onRetry = viewModel::retry)
            }
        }
    }
}

@Composable
private fun EmptyState(onPaste: () -> Unit) {
    com.switchtune.app.ui.common.MessageState(
        icon = Icons.Filled.MusicNote,
        title = stringResource(R.string.empty_title),
        subtitle = stringResource(R.string.empty_subtitle),
        primaryActionLabel = stringResource(R.string.paste_link),
        onPrimaryAction = onPaste,
    )
}

@Composable
private fun FailedState(reason: FailureReason, onRetry: () -> Unit) {
    when (reason) {
        FailureReason.NO_NETWORK -> com.switchtune.app.ui.common.MessageState(
            icon = Icons.Filled.SignalWifiOff,
            title = stringResource(R.string.error_no_internet),
            primaryActionLabel = stringResource(R.string.retry),
            onPrimaryAction = onRetry,
        )

        FailureReason.RATE_LIMITED -> com.switchtune.app.ui.common.MessageState(
            icon = Icons.Filled.MusicNote,
            title = stringResource(R.string.error_rate_limited),
            primaryActionLabel = stringResource(R.string.retry),
            onPrimaryAction = onRetry,
        )

        FailureReason.NOT_FOUND -> com.switchtune.app.ui.common.MessageState(
            icon = Icons.Filled.LibraryMusic,
            title = stringResource(R.string.error_not_found),
        )

        FailureReason.GENERIC -> com.switchtune.app.ui.common.MessageState(
            icon = Icons.Filled.MusicNote,
            title = stringResource(R.string.error_generic),
            primaryActionLabel = stringResource(R.string.retry),
            onPrimaryAction = onRetry,
        )
    }
}

@Composable
private fun LoadedState(
    resolved: ResolvedSong,
    preferred: MusicPlatform?,
    onLaunch: (PlatformLauncher.LaunchResult, MusicPlatform) -> Unit,
) {
    val context = LocalContext.current
    // Order: preferred first (if available), then the rest in enum order.
    val available = resolved.availablePlatforms
    val ordered = buildList {
        if (preferred != null && preferred in available) add(preferred)
        addAll(available.filter { it != preferred })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Artwork
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .padding(top = 8.dp),
        ) {
            if (resolved.song.artworkUrl != null) {
                AsyncImage(
                    model = resolved.song.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = resolved.song.title ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = resolved.song.artist ?: stringResource(R.string.unknown_artist),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(20.dp))

        // Edge case: preferred set but the song isn't on it -> search fallback at top.
        if (preferred != null && resolved.linkFor(preferred) == null) {
            Button(
                onClick = {
                    val result = PlatformLauncher.openSearch(context, preferred, resolved.song.searchQuery())
                    onLaunch(result, preferred)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.search_fallback, resolved.song.searchQuery(), preferred.displayName))
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = stringResource(R.string.open_in_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        // Full list of platforms where the song is available. The first one (or
        // the preferred one) is emphasised; the rest are tonal buttons.
        ordered.forEachIndexed { index, platform ->
            val link = resolved.linkFor(platform) ?: return@forEachIndexed
            val onClick = {
                val result = PlatformLauncher.openLink(context, platform, link.webUrl, link.nativeUri)
                onLaunch(result, platform)
            }
            val label = stringResource(R.string.open_in, platform.displayName)
            val isPrimary = index == 0
            if (isPrimary) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 8.dp),
                ) { Text(label) }
            } else {
                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) { Text(label) }
            }
        }
    }
}

/** Reads plain text currently on the clipboard, or null. Safe (returns null on any error). */
private fun readClipboardText(context: Context): String? = runCatching {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    clip.getItemAt(0).coerceToText(context)?.toString()
}.getOrNull()
