package com.example.ai

import android.util.Log
import com.example.data.PriceVerdict
import com.example.data.ProductCodeInfo
import com.example.ui.theme.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ProductAiRepository"
private const val PRIMARY_MODEL = "gemini-3.8-flash"
private const val FAST_FALLBACK_MODEL = "gemini-3.5-flash-lite"

data class AiProductAnalysis(
    val productCode: String,
    val powerAndSpecs: String,
    val durability: String,
    val summary: String,
    val estimatedProduct: ProductCodeInfo? = null
)

sealed interface AiAnalysisUiState {
    data object Idle : AiAnalysisUiState
    data object Loading : AiAnalysisUiState
    data class Success(val analysis: AiProductAnalysis) : AiAnalysisUiState
    data class Error(val message: String) : AiAnalysisUiState
}

class ProductAiRepository {

    private val ai by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
    }

    private val knownProductSchema = Schema.obj(
        properties = mapOf(
            "powerAndSpecs" to Schema.string(
                description = "Concise bullet or sentence explaining power, performance, or key technical specs of the product"
            ),
            "durability" to Schema.string(
                description = "Estimated lifespan or durability with normal usage (e.g., 5 to 7 years)"
            ),
            "summary" to Schema.string(
                description = "Clear 2-3 sentence summary explaining what the product does and who it is ideal for"
            )
        )
    )

    private val customProductSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Clean short product title"),
            "category" to Schema.string(description = "Short category name like Tecnología, Hogar, Audio, Calzado"),
            "storePrice" to Schema.double(description = "Realistic store price in USD"),
            "averagePrice" to Schema.double(description = "Realistic average market price in USD"),
            "suggestedQuotas" to Schema.integer(description = "Recommended number of biweekly quotas (2 to 8)"),
            "verdictReason" to Schema.string(description = "One sentence explaining if price is good or expensive vs market"),
            "powerAndSpecs" to Schema.string(description = "Key power, capacity, or technical specifications"),
            "durability" to Schema.string(description = "Estimated durability or lifespan"),
            "summary" to Schema.string(description = "Clear 2-3 sentence summary explaining the product")
        )
    )

    private val knownConfig by lazy {
        generationConfig {
            temperature = 0.4f
            maxOutputTokens = 360
            responseMimeType = "application/json"
            responseSchema = knownProductSchema
        }
    }

    private val customConfig by lazy {
        generationConfig {
            temperature = 0.4f
            maxOutputTokens = 480
            responseMimeType = "application/json"
            responseSchema = customProductSchema
        }
    }

    private val knownModel by lazy {
        ai.generativeModel(modelName = PRIMARY_MODEL, generationConfig = knownConfig)
    }

    private val knownFastModel by lazy {
        ai.generativeModel(modelName = FAST_FALLBACK_MODEL, generationConfig = knownConfig)
    }

    private val customModel by lazy {
        ai.generativeModel(modelName = PRIMARY_MODEL, generationConfig = customConfig)
    }

    private val customFastModel by lazy {
        ai.generativeModel(modelName = FAST_FALLBACK_MODEL, generationConfig = customConfig)
    }

    private suspend fun executeWithFastFallback(
        primary: GenerativeModel,
        fastFallback: GenerativeModel,
        prompt: String,
        language: AppLanguage
    ): String {
        val raw = try {
            withContext(Dispatchers.IO) {
                withTimeout(30_000L) {
                    val builder = StringBuilder()
                    primary.generateContentStream(prompt).collect { chunk ->
                        builder.append(chunk.text.orEmpty())
                    }
                    builder.toString()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary model $PRIMARY_MODEL timed out after 30s; trying $FAST_FALLBACK_MODEL")
            withContext(Dispatchers.IO) {
                withTimeout(25_000L) {
                    val builder = StringBuilder()
                    fastFallback.generateContentStream(prompt).collect { chunk ->
                        builder.append(chunk.text.orEmpty())
                    }
                    builder.toString()
                }
            }
        }

        if (raw.isBlank()) {
            throw IllegalStateException(
                if (language == AppLanguage.ES) "El modelo devolvió una respuesta vacía."
                else "Model returned an empty response."
            )
        }
        return raw
    }

    suspend fun analyzeKnownProduct(
        product: ProductCodeInfo,
        language: AppLanguage
    ): Result<AiProductAnalysis> {
        val langInstruction = if (language == AppLanguage.ES) {
            "Responde en español natural, claro y cercano."
        } else {
            "Respond in clear, concise, friendly English."
        }
        val prompt = """
            Analiza este producto para un comprador que evalúa adquirirlo en cuotas quincenales:
            - Código: ${product.code}
            - Nombre: ${product.name}
            - Categoría: ${product.category}
            - Precio en tienda: $${product.storePrice}
            - Precio promedio de mercado: $${product.averagePrice}
            $langInstruction
            Describe de forma breve y clara:
            1) powerAndSpecs: su potencia, rendimiento, capacidad o especificaciones clave.
            2) durability: cuánto dura aproximadamente con uso normal y resistencia.
            3) summary: resumen claro explicando el producto y para quién conviene.
        """.trimIndent()

        return try {
            val rawJson = executeWithFastFallback(knownModel, knownFastModel, prompt, language)
            val json = JSONObject(rawJson)
            Result.success(
                AiProductAnalysis(
                    productCode = product.code,
                    powerAndSpecs = json.getString("powerAndSpecs"),
                    durability = json.getString("durability"),
                    summary = json.getString("summary")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI product analysis timed out on both primary and fallback models", e)
            Result.failure(
                Exception(
                    if (language == AppLanguage.ES) {
                        "El análisis con IA tardó demasiado. Verifica tu conexión e intenta de nuevo."
                    } else {
                        "AI analysis timed out. Please check your connection and retry."
                    }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI product analysis failed", e)
            Result.failure(
                Exception(
                    e.localizedMessage ?: if (language == AppLanguage.ES) {
                        "No se pudo completar el análisis con IA."
                    } else {
                        "Could not complete AI product analysis."
                    }
                )
            )
        }
    }

    suspend fun analyzeCustomProduct(
        query: String,
        language: AppLanguage
    ): Result<AiProductAnalysis> {
        val cleanQuery = query.trim()
        val langInstruction = if (language == AppLanguage.ES) {
            "Responde en español natural y claro."
        } else {
            "Respond in clear, natural English."
        }
        val prompt = """
            El usuario quiere evaluar la compra del siguiente producto o código: "$cleanQuery".
            $langInstruction
            Estima datos realistas de mercado en dólares USD para este producto y explica sus características:
            - name: nombre corto y claro del producto
            - category: categoría breve
            - storePrice: precio típico en tienda (USD)
            - averagePrice: precio promedio de referencia en mercado (USD)
            - suggestedQuotas: número de cuotas quincenales recomendado (entre 2 y 8)
            - verdictReason: una frase breve comparando el precio de tienda vs el promedio
            - powerAndSpecs: potencia, capacidad o especificaciones técnicas principales
            - durability: vida útil o durabilidad estimada
            - summary: resumen claro explicando qué ofrece el producto y si conviene.
        """.trimIndent()

        return try {
            val rawJson = executeWithFastFallback(customModel, customFastModel, prompt, language)
            val json = JSONObject(rawJson)

            val name = json.getString("name")
            val category = json.getString("category")
            val storePrice = json.getDouble("storePrice").coerceAtLeast(10.0)
            val averagePrice = json.getDouble("averagePrice").coerceAtLeast(10.0)
            val quotas = json.getInt("suggestedQuotas").coerceIn(2, 8)
            val initial = storePrice * 0.25
            val biweekly = (storePrice - initial) / quotas

            val diffRatio = (storePrice - averagePrice) / averagePrice
            val verdict = when {
                diffRatio <= 0.0 -> PriceVerdict.GREEN
                diffRatio <= 0.15 -> PriceVerdict.YELLOW
                else -> PriceVerdict.RED
            }

            val codeSlug = cleanQuery.uppercase()
                .replace(Regex("[^A-Z0-9-]"), "")
                .take(8)
                .ifBlank { "IA-01" }

            val estimated = ProductCodeInfo(
                code = codeSlug,
                name = name,
                category = category,
                storePrice = storePrice,
                averagePrice = averagePrice,
                suggestedInitial = initial,
                quotasCount = quotas,
                biweeklyImpact = biweekly,
                verdict = verdict,
                verdictReason = json.getString("verdictReason")
            )

            Result.success(
                AiProductAnalysis(
                    productCode = codeSlug,
                    powerAndSpecs = json.getString("powerAndSpecs"),
                    durability = json.getString("durability"),
                    summary = json.getString("summary"),
                    estimatedProduct = estimated
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Custom AI product analysis timed out on both primary and fallback models", e)
            Result.failure(
                Exception(
                    if (language == AppLanguage.ES) {
                        "El análisis con IA tardó demasiado. Verifica tu conexión e intenta de nuevo."
                    } else {
                        "AI analysis timed out. Please check your connection and retry."
                    }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Custom AI product analysis failed", e)
            Result.failure(
                Exception(
                    e.localizedMessage ?: if (language == AppLanguage.ES) {
                        "No se pudo consultar el producto con IA."
                    } else {
                        "Could not query product with AI."
                    }
                )
            )
        }
    }
}
