package com.switchtune.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.switchtune.app.core.platform.MusicPlatform

/**
 * A vertical list of supported platforms, rendered as radio-style options.
 * Uses plain text names only (no third-party logos), per product rules.
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
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .selectable(selected = isSelected, onClick = { onSelect(platform) }),
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    RadioButton(selected = isSelected, onClick = { onSelect(platform) })
                    Text(
                        text = platform.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
