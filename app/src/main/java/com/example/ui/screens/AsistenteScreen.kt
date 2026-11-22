package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.ChatTurn
import com.example.audio.VoiceRecorderHelper
import com.example.ui.PvsUiState
import com.example.ui.components.PvsHeaderBar
import com.example.ui.components.pvsBouncyScale
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PvsForestDark
import com.example.ui.theme.PvsForestGreen
import com.example.ui.theme.PvsLeafBright
import com.example.ui.theme.PvsLeafVibrant
import com.example.ui.theme.SemaphoreRedGlow
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AsistenteScreen(
    state: PvsUiState,
    onSendMessage: (String) -> Unit,
    onSendVoiceAudio: (ByteArray, Boolean) -> Unit,
    onToggleSearchGrounding: (Boolean) -> Unit,
    onToggleVoiceReadout: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onDismissError: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTutorial: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var transcribeOnlyMode by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var audioPermissionError by remember { mutableStateOf<String?>(null) }

    val voiceRecorder = remember(context) { VoiceRecorderHelper(context) }

    // Text-to-Speech setup for conversational voice read-back
    var ttsReady by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        ttsEngine = engine
        onDispose {
            voiceRecorder.cancelRecording()
            engine.stop()
            engine.shutdown()
        }
    }

    LaunchedEffect(ttsReady, state.language) {
        if (ttsReady) {
            val locale = if (state.language == AppLanguage.ES) Locale("es", "ES") else Locale.US
            ttsEngine?.language = locale
        }
    }

    // Automatically read latest assistant message if Voice Readout mode is toggled ON
    val lastMessage = state.chatMessages.lastOrNull()
    LaunchedEffect(lastMessage?.id, state.chatVoiceReadout, state.isChatBusy) {
        if (state.chatVoiceReadout && !state.isChatBusy && ttsReady &&
            lastMessage != null && lastMessage.role == "model"
        ) {
            ttsEngine?.speak(
                lastMessage.text.take(600),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "pvs_msg_${lastMessage.id}"
            )
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds += 1
            }
        }
    }

    LaunchedEffect(state.chatMessages.size, state.streamingChatText) {
        val total = state.chatMessages.size + if (state.streamingChatText.isNotEmpty()) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            audioPermissionError = null
            val started = voiceRecorder.startRecording()
            if (started) {
                isRecording = true
            } else {
                audioPermissionError = strings.assistantMicStartError
            }
        } else {
            audioPermissionError = strings.assistantMicPermissionDenied
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("asistente_screen")
    ) {
        PvsHeaderBar(
            title = strings.assistantTitle,
            subtitle = strings.assistantSubtitle,
            language = state.language,
            alertCount = state.upcomingAlerts.size,
            onToggleLanguage = onToggleLanguage,
            onOpenNotifications = onOpenNotifications,
            onOpenTutorial = onOpenTutorial,
            trailingAction = if (state.chatMessages.isNotEmpty()) {
                {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            ttsEngine?.stop()
                            onClearChat()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = strings.assistantClearChatCd,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null
        )

        // Mode & Feature Filter Chips (Google Search Grounding + Voice TTS + Transcribe Only)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.chatSearchGroundingEnabled,
                onClick = { onToggleSearchGrounding(!state.chatSearchGroundingEnabled) },
                label = { Text(strings.assistantGroundingChip) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("grounding_toggle_chip")
            )

            FilterChip(
                selected = state.chatVoiceReadout,
                onClick = {
                    val next = !state.chatVoiceReadout
                    if (!next) ttsEngine?.stop()
                    onToggleVoiceReadout(next)
                },
                label = { Text(strings.assistantVoiceTtsChip) },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.chatVoiceReadout) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("voice_tts_chip")
            )

            FilterChip(
                selected = transcribeOnlyMode,
                onClick = { transcribeOnlyMode = !transcribeOnlyMode },
                label = { Text(strings.assistantTranscribeOnlyChip) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("transcribe_only_chip")
            )
        }

        // Chat Messages Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.chatMessages.isEmpty() && state.streamingChatText.isEmpty()) {
                AssistantEmptyWelcome(
                    prompts = strings.assistantStarterPrompts,
                    onSelectPrompt = { prompt ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSendMessage(prompt)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.chatMessages, key = { it.id }) { turn ->
                        ChatBubbleItem(
                            turn = turn,
                            onSpeak = {
                                if (ttsReady) {
                                    ttsEngine?.speak(
                                        turn.text.take(600),
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "pvs_speak_${turn.id}"
                                    )
                                }
                            }
                        )
                    }

                    if (state.streamingChatText.isNotEmpty()) {
                        item(key = "streaming_turn") {
                            ChatBubbleItem(
                                turn = ChatTurn(
                                    id = "streaming",
                                    role = "model",
                                    text = state.streamingChatText
                                ),
                                isStreaming = true,
                                onSpeak = {}
                            )
                        }
                    } else if (state.isChatBusy) {
                        item(key = "thinking_indicator") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PvsLeafVibrant
                                )
                                Text(
                                    text = strings.assistantThinkingLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error banner (Chat / Audio)
        val shownError = audioPermissionError ?: state.chatError
        AnimatedVisibility(visible = !shownError.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shownError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            audioPermissionError = null
                            onDismissError()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.notifDismissCd,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Bottom Composer / Voice Recorder Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (isRecording) {
                ActiveRecordingBar(
                    seconds = recordingSeconds,
                    transcribeOnly = transcribeOnlyMode,
                    onCancel = {
                        voiceRecorder.cancelRecording()
                        isRecording = false
                    },
                    onStopAndSend = {
                        val bytes = voiceRecorder.stopAndReadBytes()
                        isRecording = false
                        if (bytes != null && bytes.isNotEmpty()) {
                            onSendVoiceAudio(bytes, transcribeOnlyMode)
                        } else {
                            audioPermissionError = strings.assistantMicEmptyError
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = strings.assistantInputHint,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val trimmed = inputText.trim()
                                if (trimmed.isNotEmpty() && !state.isChatBusy) {
                                    inputText = ""
                                    onSendMessage(trimmed)
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_message_input")
                    )

                    // Record Audio / Transcribe Button
                    val micInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                audioPermissionError = null
                                val started = voiceRecorder.startRecording()
                                if (started) {
                                    isRecording = true
                                } else {
                                    audioPermissionError = strings.assistantMicStartError
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = !state.isChatBusy,
                        interactionSource = micInteraction,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .pvsBouncyScale(micInteraction)
                            .testTag("assistant_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = strings.assistantMicCd,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Send Text Button
                    val sendInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = {
                            val trimmed = inputText.trim()
                            if (trimmed.isNotEmpty() && !state.isChatBusy) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                inputText = ""
                                onSendMessage(trimmed)
                            }
                        },
                        enabled = inputText.isNotBlank() && !state.isChatBusy,
                        interactionSource = sendInteraction,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !state.isChatBusy) PvsForestGreen
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .pvsBouncyScale(sendInteraction)
                            .testTag("assistant_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = strings.assistantSendCd,
                            tint = if (inputText.isNotBlank() && !state.isChatBusy) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRecordingBar(
    seconds: Int,
    transcribeOnly: Boolean,
    onCancel: () -> Unit,
    onStopAndSend: () -> Unit
) {
    val strings = LocalAppStrings.current
    val pulse = rememberInfiniteTransition(label = "mic_pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_dot_alpha"
    )
    val mins = seconds / 60
    val secs = seconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(SemaphoreRedGlow.copy(alpha = alpha))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (transcribeOnly) strings.assistantRecordingTranscribeLabel else strings.assistantRecordingVoiceLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = timerText,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = SemaphoreRedGlow
            )
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("cancel_recording_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.actionCancel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onStopAndSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PvsForestGreen)
                .testTag("stop_and_send_audio_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = strings.assistantStopSendCd,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(
    turn: ChatTurn,
    isStreaming: Boolean = false,
    onSpeak: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isModel = turn.role == "model"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isModel) 6.dp else 20.dp,
                bottomEnd = if (isModel) 20.dp else 6.dp
            ),
            color = if (isModel) MaterialTheme.colorScheme.surfaceVariant else PvsForestGreen,
            border = if (isModel) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!turn.audioTranscript.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isModel) PvsLeafVibrant else PvsLeafBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = strings.assistantAudioTranscriptBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isModel) MaterialTheme.colorScheme.primary else PvsLeafBright
                        )
                    }
                }

                Text(
                    text = turn.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isModel) MaterialTheme.colorScheme.onSurface else Color.White
                )

                if (!turn.groundingNote.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = turn.groundingNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (isModel && !isStreaming) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = strings.assistantSpeakMessageCd,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantEmptyWelcome(
    prompts: List<String>,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = strings.assistantEmptyTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.assistantEmptyBody,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEachIndexed { idx, prompt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(prompt) }
                        .testTag("assistant_prompt_chip_$idx"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = PvsLeafVibrant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
