package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ProductCodeInfo
import com.example.data.ProductNoteEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestDark
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsLeafBright
import com.example.ui.theme.PvsLeafVibrant
import com.example.ui.theme.PvsMintContainer
import com.example.ui.theme.PvsMintSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room-backed Custom Product Notes card for the product detail view, allowing users to add,
 * quick-tag, inline-edit, and delete personal notes per product.
 */
@Composable
fun ProductNotesCard(
    product: ProductCodeInfo,
    notes: List<ProductNoteEntity>,
    onAddNote: (String) -> Unit,
    onUpdateNote: (ProductNoteEntity, String) -> Unit,
    onDeleteNote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var draftNote by remember(product.code) { mutableStateOf("") }
    var editingNoteId by remember(product.code) { mutableStateOf<Int?>(null) }
    var editingText by remember(product.code) { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_notes_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Note Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.linearGradient(listOf(PvsForestDark, PvsForestGreen))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StickyNote2,
                            contentDescription = null,
                            tint = PvsLeafBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.productNotesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PvsForestDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.productNotesSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (notes.isNotEmpty()) PvsMintContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (notes.isNotEmpty()) PvsLeafVibrant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {}
                        .testTag("product_notes_count_badge")
                ) {
                    Text(
                        text = strings.productNotesCountBadge(notes.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (notes.isNotEmpty()) PvsForestDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Quick-tap Note Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                strings.productNotesQuickChips.forEachIndexed { idx, tagText ->
                    val chipInt = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .pvsBouncyScale(chipInt)
                            .clip(RoundedCornerShape(50))
                            .clickable(
                                interactionSource = chipInt,
                                indication = androidx.compose.material3.ripple()
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draftNote = if (draftNote.isBlank()) tagText else "$draftNote · $tagText"
                            }
                            .testTag("product_note_quick_chip_$idx"),
                        shape = RoundedCornerShape(50),
                        color = PvsMintSurface,
                        border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddComment,
                                contentDescription = null,
                                tint = PvsForestGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PvsForestDark
                            )
                        }
                    }
                }
            }

            // Inline Input + Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    placeholder = {
                        Text(
                            text = strings.productNotesInputHint,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PvsLeafVibrant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (draftNote.isNotBlank()) {
                                onAddNote(draftNote.trim())
                                draftNote = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_product_note_input")
                )

                val saveInt = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        if (draftNote.isNotBlank()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddNote(draftNote.trim())
                            draftNote = ""
                            focusManager.clearFocus()
                        }
                    },
                    enabled = draftNote.isNotBlank(),
                    interactionSource = saveInt,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PvsForestGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier
                        .pvsBouncyScale(saveInt)
                        .testTag("save_product_note_button")
                ) {
                    Text(
                        text = strings.productNotesSaveBtn,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Notes List or Friendly Empty State
            if (notes.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_notes_empty_state"),
                    shape = RoundedCornerShape(16.dp),
                    color = PvsMintSurface.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, PvsLeafVibrant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = PvsForestGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = strings.productNotesEmptyTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PvsForestDark
                            )
                            Text(
                                text = strings.productNotesEmptyBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val dateFormatter = remember { SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { note ->
                        val isEditing = editingNoteId == note.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_note_item_${note.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = PvsMintSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isEditing) PvsForestGreen else PvsLeafVibrant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = isEditing,
                                    enter = fadeIn(tween(180)) + expandVertically(),
                                    exit = fadeOut(tween(140)) + shrinkVertically()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editingText,
                                            onValueChange = { editingText = it },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("edit_note_input_${note.id}")
                                        )
                                        IconButton(
                                            onClick = {
                                                if (editingText.isNotBlank()) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onUpdateNote(note, editingText.trim())
                                                    editingNoteId = null
                                                    focusManager.clearFocus()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("confirm_edit_note_button_${note.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = strings.productNotesUpdateBtn,
                                                tint = PvsForestGreen
                                            )
                                        }
                                        IconButton(
                                            onClick = { editingNoteId = null },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = strings.actionCancel,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (!isEditing) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = note.noteText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = PvsForestDark,
                                                modifier = Modifier.testTag("product_note_text_${note.id}")
                                            )
                                            Text(
                                                text = dateFormatter.format(Date(note.updatedAt)),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = PvsForestGreen.copy(alpha = 0.8f)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    editingNoteId = note.id
                                                    editingText = note.noteText
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("edit_note_button_${note.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = strings.productNotesEditCd,
                                                    tint = PvsForestGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onDeleteNote(note.id)
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_note_button_${note.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = strings.productNotesDeleteCd,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
