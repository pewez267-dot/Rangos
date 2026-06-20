package com.switchtune.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.switchtune.app.ui.theme.Background
import com.switchtune.app.ui.theme.BrandMagenta
import com.switchtune.app.ui.theme.BrandViolet

private val BrandGradient = Brush.horizontalGradient(listOf(BrandViolet, BrandMagenta))

/** App-wide background: a deep gradient with a soft violet glow at the top. */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF1A1330),
                    0.45f to Background,
                    1f to Color(0xFF080711),
                ),
            ),
    ) { content() }
}

/** Primary gradient call-to-action button. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun FullScreenLoading(message: String? = null) {
    AppBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = BrandViolet,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(46.dp),
                )
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Premium message state used for empty/error screens: a glowing gradient icon
 * badge, headline, supporting text, and up to two actions.
 */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AppBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(BrandViolet.copy(alpha = 0.35f), BrandMagenta.copy(alpha = 0.18f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (primaryActionLabel != null && onPrimaryAction != null) {
                GradientButton(
                    text = primaryActionLabel,
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                )
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Text(secondaryActionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


/** A circular gradient avatar with a white glyph — the app's platform mark. */
@Composable
fun PlatformAvatar(
    visual: PlatformVisual,
    size: androidx.compose.ui.unit.Dp,
    dimmed: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    visual.gradient.map { it.copy(alpha = if (dimmed) 0.45f else 1f) },
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (dimmed) 0.85f else 1f),
            modifier = Modifier.size(size * 0.52f),
        )
    }
}
