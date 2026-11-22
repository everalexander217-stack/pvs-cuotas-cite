package com.example.ai

import android.util.Log
import com.example.ui.theme.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "PvsAssistantRepo"
private const val ASSISTANT_PRIMARY_MODEL = "gemini-3.8-flash"
private const val ASSISTANT_FALLBACK_MODEL = "gemini-3.5-flash-lite"
private const val MAX_INLINE_AUDIO_BYTES = 18 * 1024 * 1024

data class ChatTurn(
    val id: String,
    val role: String, // "user" | "model"
    val text: String,
    val audioTranscript: String? = null,
    val groundingNote: String? = null
)

data class AudioTranscriptionResult(
    val transcript: String,
    val assistantReply: String
)

sealed interface ChatStreamEvent {
    data class Chunk(val accumulatedText: String) : ChatStreamEvent
    data class Completed(val finalText: String, val groundingNote: String?) : ChatStreamEvent
}

class PvsAssistantRepository {

    private val ai by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
    }

    private fun buildSystemInstruction(): Content = content("system") {
        text(
            """
            Eres el Asistente Inteligente de PVS Cuotas, experto en compras por cuotas quincenales, comparación real de precios y finanzas personales.
            Reglas:
            1) Responde en el idioma del usuario (español o inglés).
            2) Sé concreto, amable y práctico: ayuda a evaluar si un producto vale la pena en cuotas quincenales según su precio, presupuesto disponible y alternativas del mercado.
            3) Si el usuario activa búsqueda web, contrasta con precios o referencias actuales y menciona brevemente las fuentes.
            4) Usa montos claros en USD y listas cortas legibles en pantalla móvil.
            """.trimIndent()
        )
    }

    private val groundedChatModel by lazy {
        ai.generativeModel(
            modelName = ASSISTANT_PRIMARY_MODEL,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = buildSystemInstruction()
        )
    }

    private val standardChatModel by lazy {
        ai.generativeModel(
            modelName = ASSISTANT_PRIMARY_MODEL,
            systemInstruction = buildSystemInstruction()
        )
    }

    private val fastFallbackModel by lazy {
        ai.generativeModel(
            modelName = ASSISTANT_FALLBACK_MODEL,
            systemInstruction = buildSystemInstruction()
        )
    }

    private val audioSchema = Schema.obj(
        properties = mapOf(
            "transcript" to Schema.string(
                description = "Exact verbatim transcription of the spoken user audio in its original language."
            ),
            "reply" to Schema.string(
                description = "Helpful conversational response from the PVS financial & shopping assistant addressing what the user said, or empty string if transcribe-only was requested."
            )
        )
    )

    private val audioModel by lazy {
        ai.generativeModel(
            modelName = ASSISTANT_PRIMARY_MODEL,
            generationConfig = generationConfig {
                temperature = 0.35f
                responseMimeType = "application/json"
                responseSchema = audioSchema
            },
            systemInstruction = buildSystemInstruction()
        )
    }

    fun sendChatMessageStream(
        history: List<ChatTurn>,
        userMessage: String,
        useSearchGrounding: Boolean,
        contextSummary: String,
        language: AppLanguage
    ): Flow<ChatStreamEvent> = flow {
        val sdkHistory = history.takeLast(16).map { turn ->
            content(role = if (turn.role == "model") "model" else "user") {
                text(turn.text)
            }
        }
        val contextualPrompt = if (contextSummary.isNotBlank()) {
            "[Contexto PVS actual: $contextSummary]\n\n$userMessage"
        } else {
            userMessage
        }

        val activeModel: GenerativeModel = if (useSearchGrounding) groundedChatModel else standardChatModel
        val buffer = StringBuilder()
        val searchQueries = linkedSetOf<String>()
        val sourceTitles = linkedSetOf<String>()

        try {
            withTimeout<Unit>(60_000L) {
                val chat = activeModel.startChat(history = sdkHistory)
                chat.sendMessageStream(contextualPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        buffer.append(chunkText)
                        emit(ChatStreamEvent.Chunk(buffer.toString()))
                    }
                    chunk.candidates.firstOrNull()?.groundingMetadata?.let { meta ->
                        meta.webSearchQueries.forEach { q ->
                            if (q.isNotBlank()) searchQueries.add(q.trim())
                        }
                        meta.groundingChunks.forEach { gChunk ->
                            val webChunk = gChunk.web
                            val title = webChunk?.title?.takeIf { it.isNotBlank() }
                                ?: webChunk?.domain?.takeIf { it.isNotBlank() }
                            if (title != null) sourceTitles.add(title)
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary chat timed out, trying fallback model")
            buffer.clear()
            withTimeout<Unit>(45_000L) {
                val fallbackChat = fastFallbackModel.startChat(history = sdkHistory)
                fallbackChat.sendMessageStream(contextualPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        buffer.append(chunkText)
                        emit(ChatStreamEvent.Chunk(buffer.toString()))
                    }
                }
            }
        }

        val finalText = buffer.toString().trim()
        if (finalText.isBlank()) {
            throw IllegalStateException(
                if (language == AppLanguage.ES) "El asistente devolvió una respuesta vacía."
                else "Assistant returned an empty response."
            )
        }

        val groundingNote = buildString {
            if (sourceTitles.isNotEmpty()) {
                append(if (language == AppLanguage.ES) "Fuentes Google: " else "Google sources: ")
                append(sourceTitles.take(3).joinToString(" · "))
            } else if (searchQueries.isNotEmpty()) {
                append(if (language == AppLanguage.ES) "Búsqueda Google: " else "Google search: ")
                append(searchQueries.take(2).joinToString(", "))
            }
        }.ifBlank { null }

        emit(ChatStreamEvent.Completed(finalText = finalText, groundingNote = groundingNote))
    }.flowOn(Dispatchers.IO)

    suspend fun transcribeAndProcessAudio(
        audioBytes: ByteArray,
        mimeType: String,
        transcribeOnly: Boolean,
        contextSummary: String,
        language: AppLanguage
    ): Result<AudioTranscriptionResult> {
        if (audioBytes.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    if (language == AppLanguage.ES) "No se capturó audio suficiente."
                    else "Not enough audio was captured."
                )
            )
        }
        if (audioBytes.size > MAX_INLINE_AUDIO_BYTES) {
            return Result.failure(
                IllegalArgumentException(
                    if (language == AppLanguage.ES) "El audio supera el límite permitido de 18 MB."
                    else "Audio exceeds the 18 MB inline limit."
                )
            )
        }

        val instruction = if (transcribeOnly) {
            if (language == AppLanguage.ES) {
                "Transcribe con precisión exacta lo que se dice en este audio en español o en el idioma hablado. En 'reply' escribe un breve resumen de confirmación indicando la transcripción lista."
            } else {
                "Transcribe accurately what is spoken in this audio. In 'reply', write a brief confirmation summarizing the transcript."
            }
        } else {
            if (language == AppLanguage.ES) {
                "1) Transcribe con fidelidad exacta lo que dice la persona en 'transcript'. 2) Responde como Asistente PVS en 'reply' considerando este contexto financiero: $contextSummary"
            } else {
                "1) Accurately transcribe the spoken audio in 'transcript'. 2) Respond as PVS Assistant in 'reply' using this context: $contextSummary"
            }
        }

        return try {
            val raw = withContext(Dispatchers.IO) {
                withTimeout(60_000L) {
                    val promptContent = content {
                        inlineData(audioBytes, mimeType)
                        text(instruction)
                    }
                    audioModel.generateContent(promptContent).text
                }
            } ?: throw IllegalStateException("Audio model returned empty text.")

            val json = JSONObject(raw)
            val transcript = json.optString("transcript").trim()
            val reply = json.optString("reply").trim()

            if (transcript.isBlank() && reply.isBlank()) {
                throw IllegalStateException("Empty transcript returned from audio.")
            }

            Result.success(
                AudioTranscriptionResult(
                    transcript = transcript.ifBlank { "(Audio procesado)" },
                    assistantReply = reply.ifBlank { transcript }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            Result.failure(
                Exception(
                    if (language == AppLanguage.ES) "La transcripción tardó demasiado. Intenta con un audio más corto."
                    else "Audio transcription timed out. Please try a shorter recording."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            Result.failure(
                Exception(
                    e.localizedMessage ?: if (language == AppLanguage.ES) {
                        "No se pudo transcribir el audio."
                    } else {
                        "Could not transcribe audio."
                    }
                )
            )
        }
    }
}
