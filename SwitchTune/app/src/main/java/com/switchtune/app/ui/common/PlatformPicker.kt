package com.switchtune.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.switchtune.app.core.platform.MusicPlatform

/**
 * A vertical list of supported platforms, rendered as premium selectable glass
 * cards with the platform's gradient avatar. No third-party logos.
 */
@Composable
fun PlatformPicker(
    selected: MusicPlatform?,
    onSelect: (MusicPlatform) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MusicPlatform.entries.forEach { platform ->
            val isSelected = platform == selected
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .selectable(selected = isSelected, onClick = { onSelect(platform) }),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    PlatformAvatar(visual = platform.visual(), size = 42.dp)
                    Text(
                        text = platform.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
