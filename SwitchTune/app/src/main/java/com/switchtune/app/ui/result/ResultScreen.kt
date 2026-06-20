package com.switchtune.app.ui.result

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
                ResultUiState.Empty -> EmptyState()
                ResultUiState.Resolving -> ResolvingState()
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
private fun ResolvingState() {
    com.switchtune.app.ui.common.FullScreenLoading(stringResource(R.string.resolving))
}

@Composable
private fun EmptyState() {
    com.switchtune.app.ui.common.MessageState(
        icon = Icons.Filled.MusicNote,
        title = stringResource(R.string.empty_title),
        subtitle = stringResource(R.string.empty_subtitle),
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
    preferred: MusicPlatform,
    onLaunch: (PlatformLauncher.LaunchResult, MusicPlatform) -> Unit,
) {
    val context = LocalContext.current
    val preferredLink = resolved.linkFor(preferred)
    val otherPlatforms = resolved.availablePlatforms.filter { it != preferred }
    var showOthers by remember { mutableStateOf(false) }

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
                .padding(top = 16.dp),
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

        Spacer(Modifier.height(28.dp))

        // Primary action: open in preferred app, or search fallback if not matched.
        if (preferredLink != null) {
            Button(
                onClick = {
                    val result = PlatformLauncher.openLink(
                        context = context,
                        platform = preferred,
                        webUrl = preferredLink.webUrl,
                        nativeUri = preferredLink.nativeUri,
                    )
                    onLaunch(result, preferred)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(stringResource(R.string.open_in, preferred.displayName))
            }
        } else {
            // Edge case: song found, but not on the preferred platform -> search fallback.
            Button(
                onClick = {
                    val result = PlatformLauncher.openSearch(
                        context = context,
                        platform = preferred,
                        query = resolved.song.searchQuery(),
                    )
                    onLaunch(result, preferred)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(stringResource(R.string.search_fallback, resolved.song.searchQuery(), preferred.displayName))
            }
        }

        if (otherPlatforms.isNotEmpty()) {
            TextButton(
                onClick = { showOthers = !showOthers },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.other_platforms))
            }
            AnimatedVisibility(visible = showOthers) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    otherPlatforms.forEach { platform ->
                        val link = resolved.linkFor(platform) ?: return@forEach
                        OutlinedButton(
                            onClick = {
                                val result = PlatformLauncher.openLink(
                                    context = context,
                                    platform = platform,
                                    webUrl = link.webUrl,
                                    nativeUri = link.nativeUri,
                                )
                                onLaunch(result, platform)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.open_in, platform.displayName))
                        }
                    }
                }
            }
        }
    }
}
