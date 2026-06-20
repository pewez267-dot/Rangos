package com.switchtune.app.ui.result

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.switchtune.app.ui.common.AppBackground
import com.switchtune.app.ui.common.visual
import com.switchtune.app.ui.theme.Background
import kotlinx.coroutines.launch

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

    AppBackground {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Crossfade(targetState = state, animationSpec = tween(280), label = "result") { s ->
            Box(modifier = Modifier.padding(padding)) {
                when (s) {
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
            icon = Icons.Outlined.LibraryMusic,
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
    val matchedCount = MusicPlatform.entries.count { resolved.linkFor(it) != null }
    // Display order: preferred first, then the curated enum order.
    val ordered = buildList {
        if (preferred != null) add(preferred)
        addAll(MusicPlatform.entries.filter { it != preferred })
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Hero(resolved = resolved, matchedCount = matchedCount)
            }
            item {
                Text(
                    text = stringResource(R.string.open_in_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                )
            }
            items(ordered, key = { it.name }) { platform ->
                val link = resolved.linkFor(platform)
                PlatformRow(
                    platform = platform,
                    matched = link != null,
                    isPreferred = platform == preferred,
                    onClick = {
                        val result = if (link != null) {
                            PlatformLauncher.openLink(context, platform, link.webUrl, link.nativeUri)
                        } else {
                            PlatformLauncher.openSearch(context, platform, resolved.song.searchQuery())
                        }
                        onLaunch(result, platform)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun Hero(resolved: ResolvedSong, matchedCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
    ) {
        // Blurred artwork backdrop (API 31+; gracefully unblurred on older devices).
        if (resolved.song.artworkUrl != null) {
            AsyncImage(
                model = resolved.song.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
                    .graphicsLayer { alpha = 0.55f },
            )
        }
        // Scrim so text/cards stay readable and blend into the background.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Background.copy(alpha = 0.65f),
                        1f to Background,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .size(184.dp)
                    .aspectRatio(1f),
            ) {
                if (resolved.song.artworkUrl != null) {
                    AsyncImage(
                        model = resolved.song.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                }
            }

            Text(
                text = resolved.song.title ?: stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (matchedCount > 0) {
                Text(
                    text = stringResource(R.string.available_on_count, matchedCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PlatformRow(
    platform: MusicPlatform,
    matched: Boolean,
    isPreferred: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = platform.visual()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(visual.accent.copy(alpha = if (matched) 1f else 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = visual.badge,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = platform.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        if (matched) R.string.row_open else R.string.row_search,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (matched) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (isPreferred) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.your_pick),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Icon(
                imageVector = if (matched) Icons.AutoMirrored.Filled.ArrowForwardIos else Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
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
