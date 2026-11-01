package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.SpanType

@Composable
fun RichTextToolbar(
    onToggleSpan: (SpanType, String?) -> Unit,
    onInsertBullet: () -> Unit,
    onInsertNumbered: () -> Unit,
    onConvertToTodoList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHighlightMenu by remember { mutableStateOf(false) }
    var showTextColorMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bold
            ToolbarButton(
                icon = Icons.Outlined.FormatBold,
                description = "Bold",
                testTag = "toolbar_bold",
                onClick = { onToggleSpan(SpanType.BOLD, null) }
            )

            // Italic
            ToolbarButton(
                icon = Icons.Outlined.FormatItalic,
                description = "Italic",
                testTag = "toolbar_italic",
                onClick = { onToggleSpan(SpanType.ITALIC, null) }
            )

            // Underline
            ToolbarButton(
                icon = Icons.Outlined.FormatUnderlined,
                description = "Underline",
                testTag = "toolbar_underline",
                onClick = { onToggleSpan(SpanType.UNDERLINE, null) }
            )

            // Strikethrough
            ToolbarButton(
                icon = Icons.Outlined.FormatStrikethrough,
                description = "Strikethrough",
                testTag = "toolbar_strikethrough",
                onClick = { onToggleSpan(SpanType.STRIKETHROUGH, null) }
            )

            // Highlight Color Dropdown
            Box {
                ToolbarButton(
                    icon = Icons.Outlined.Highlight,
                    description = "Highlight",
                    testTag = "toolbar_highlight",
                    onClick = { showHighlightMenu = true }
                )

                DropdownMenu(
                    expanded = showHighlightMenu,
                    onDismissRequest = { showHighlightMenu = false }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Highlight Color",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val highlights = listOf(
                                "#FFF176" to "Yellow",
                                "#A5D6A7" to "Green",
                                "#CE93D8" to "Lilac",
                                "#FFAB91" to "Coral",
                                "#80DEEA" to "Cyan"
                            )
                            highlights.forEach { (hex, _) ->
                                val color = Color(0xFF000000 or hex.removePrefix("#").toLong(16))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable {
                                            onToggleSpan(SpanType.HIGHLIGHT, hex)
                                            showHighlightMenu = false
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Text Color Dropdown
            Box {
                ToolbarButton(
                    icon = Icons.Outlined.FormatColorText,
                    description = "Text Color",
                    testTag = "toolbar_text_color",
                    onClick = { showTextColorMenu = true }
                )

                DropdownMenu(
                    expanded = showTextColorMenu,
                    onDismissRequest = { showTextColorMenu = false }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Text Color",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val textColors = listOf(
                                "#5B61D6" to "Primary",
                                "#E53935" to "Red",
                                "#43A047" to "Green",
                                "#1E88E5" to "Blue",
                                "#8E24AA" to "Purple"
                            )
                            textColors.forEach { (hex, _) ->
                                val color = Color(0xFF000000 or hex.removePrefix("#").toLong(16))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable {
                                            onToggleSpan(SpanType.COLOR, hex)
                                            showTextColorMenu = false
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Divider in toolbar
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // Bullet List
            ToolbarButton(
                icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                description = "Bullet List",
                testTag = "toolbar_bullet_list",
                onClick = onInsertBullet
            )

            // Numbered List
            ToolbarButton(
                icon = Icons.Outlined.FormatListNumbered,
                description = "Numbered List",
                testTag = "toolbar_numbered_list",
                onClick = onInsertNumbered
            )

            // Convert to Checklist
            ToolbarButton(
                icon = Icons.Outlined.Checklist,
                description = "Checklist",
                testTag = "toolbar_convert_checklist",
                onClick = onConvertToTodoList
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}
