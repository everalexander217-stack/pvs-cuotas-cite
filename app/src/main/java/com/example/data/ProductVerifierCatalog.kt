package com.example.data

import kotlin.math.roundToInt

enum class PriceVerdict(
    val headline: String,
    val badgeLabel: String
) {
    GREEN(
        headline = "¡Vale la pena!",
        badgeLabel = "Semáforo Verde"
    ),
    YELLOW(
        headline = "Piénsalo dos veces",
        badgeLabel = "Semáforo Amarillo"
    ),
    RED(
        headline = "Está caro, busca otra opción",
        badgeLabel = "Semáforo Rojo"
    )
}

data class ProductCodeInfo(
    val code: String,
    val name: String,
    val category: String,
    val storePrice: Double,
    val averagePrice: Double,
    val suggestedInitial: Double,
    val quotasCount: Int,
    val biweeklyImpact: Double,
    val verdict: PriceVerdict,
    val verdictReason: String
) {
    val percentageDifference: Int
        get() = (((storePrice - averagePrice) / averagePrice) * 100.0).roundToInt()
}

object ProductVerifierCatalog {
    val featuredCodes = listOf("TV-50", "CEL-128", "ZAP-01", "AUD-05")

    val catalog: List<ProductCodeInfo> = listOf(
        ProductCodeInfo(
            code = "TV-50",
            name = "Smart TV 50\" 4K UHD",
            category = "Tecnología",
            storePrice = 389.00,
            averagePrice = 460.00,
            suggestedInitial = 97.25,
            quotasCount = 6,
            biweeklyImpact = 48.63,
            verdict = PriceVerdict.GREEN,
            verdictReason = "Está un 15% por debajo del promedio del mercado. Excelente oportunidad para financiar en cuotas cómodas."
        ),
        ProductCodeInfo(
            code = "CEL-128",
            name = "Smartphone 128GB 5G",
            category = "Telefonía",
            storePrice = 320.00,
            averagePrice = 295.00,
            suggestedInitial = 80.00,
            quotasCount = 6,
            biweeklyImpact = 40.00,
            verdict = PriceVerdict.YELLOW,
            verdictReason = "Cuesta un 8% más que el promedio habitual. Conviene comparar en otra tienda o esperar promoción."
        ),
        ProductCodeInfo(
            code = "ZAP-01",
            name = "Zapatillas Running Pro",
            category = "Calzado",
            storePrice = 145.00,
            averagePrice = 105.00,
            suggestedInitial = 36.25,
            quotasCount = 4,
            biweeklyImpact = 27.19,
            verdict = PriceVerdict.RED,
            verdictReason = "Supera en un 38% el precio promedio real. El sobrecosto encarece innecesariamente tu quincena."
        ),
        ProductCodeInfo(
            code = "AUD-05",
            name = "Audífonos ANC Studio",
            category = "Audio",
            storePrice = 118.00,
            averagePrice = 149.00,
            suggestedInitial = 29.50,
            quotasCount = 4,
            biweeklyImpact = 22.13,
            verdict = PriceVerdict.GREEN,
            verdictReason = "Ahorro real del 21% frente al mercado y un impacto ligero en tu presupuesto quincenal."
        ),
        ProductCodeInfo(
            code = "LAP-16",
            name = "Laptop Ultrabook 16GB",
            category = "Computación",
            storePrice = 790.00,
            averagePrice = 735.00,
            suggestedInitial = 197.50,
            quotasCount = 8,
            biweeklyImpact = 74.06,
            verdict = PriceVerdict.YELLOW,
            verdictReason = "Precio ligeramente por encima del promedio (+7%) y cuota quincenal moderadamente alta."
        ),
        ProductCodeInfo(
            code = "CAF-02",
            name = "Cafetera Espresso Barista",
            category = "Hogar",
            storePrice = 215.00,
            averagePrice = 160.00,
            suggestedInitial = 53.75,
            quotasCount = 4,
            biweeklyImpact = 40.31,
            verdict = PriceVerdict.RED,
            verdictReason = "Sobreprecio del 34% frente a distribuidores oficiales. Te conviene buscar otra oferta."
        )
    )

    fun findByQuery(rawQuery: String): ProductCodeInfo? {
        val normalized = rawQuery.trim().uppercase()
        if (normalized.isEmpty()) return null
        val compact = normalized.replace("-", "").replace(" ", "")
        return catalog.firstOrNull { item ->
            val itemCodeCompact = item.code.uppercase().replace("-", "")
            item.code.equals(normalized, ignoreCase = true) ||
                itemCodeCompact == compact ||
                item.name.contains(rawQuery.trim(), ignoreCase = true)
        }
    }

    private val retailBarcodeMap: Map<String, String> = mapOf(
        "7501001005011" to "TV-50",
        "7501001012804" to "CEL-128",
        "7501001000109" to "ZAP-01",
        "7501001000505" to "AUD-05",
        "7501001001601" to "LAP-16",
        "7501001000208" to "CAF-02"
    )

    fun resolveBarcodeValue(rawBarcode: String): String {
        val trimmed = rawBarcode.trim()
        if (trimmed.isEmpty()) return ""
        retailBarcodeMap[trimmed]?.let { return it }

        val upper = trimmed.uppercase()
        catalog.firstOrNull { product ->
            val codeCompact = product.code.uppercase().replace("-", "")
            upper == product.code.uppercase() ||
                upper.replace("-", "") == codeCompact ||
                upper.contains(product.code.uppercase())
        }?.let { return it.code }

        return trimmed
    }

    private val monthLabelsEs = listOf(
        "Dic", "Ene", "Feb", "Mar", "Abr", "May",
        "Jun", "Jul", "Ago", "Sep", "Oct", "Hoy"
    )
    private val monthLabelsEn = listOf(
        "Dec", "Jan", "Feb", "Mar", "Apr", "May",
        "Jun", "Jul", "Aug", "Sep", "Oct", "Now"
    )

    fun priceHistoryFor(
        product: ProductCodeInfo,
        months: Int = 6
    ): List<PriceHistoryPoint> {
        val count = months.coerceIn(3, 12)
        val fullMultipliers: List<Pair<Double, Double>> = when (product.code.uppercase()) {
            "TV-50" -> listOf(
                1.18 to 1.04, 1.15 to 1.03, 1.16 to 1.03, 1.12 to 1.02,
                1.10 to 1.01, 1.09 to 1.01, 1.08 to 1.00, 1.06 to 1.01,
                1.05 to 1.00, 1.03 to 1.00, 1.01 to 1.00, 1.00 to 1.00
            )
            "CEL-128" -> listOf(
                0.96 to 1.02, 0.97 to 1.01, 0.95 to 1.00, 0.98 to 1.00,
                1.00 to 1.01, 0.99 to 1.00, 0.97 to 0.99, 0.98 to 1.00,
                1.01 to 1.00, 1.02 to 1.01, 1.01 to 1.00, 1.00 to 1.00
            )
            "ZAP-01" -> listOf(
                0.80 to 0.98, 0.82 to 0.99, 0.85 to 1.00, 0.84 to 1.00,
                0.87 to 0.99, 0.89 to 1.00, 0.91 to 1.01, 0.93 to 1.00,
                0.95 to 1.00, 0.98 to 1.01, 0.99 to 1.00, 1.00 to 1.00
            )
            "AUD-05" -> listOf(
                1.24 to 1.05, 1.21 to 1.04, 1.19 to 1.03, 1.17 to 1.03,
                1.14 to 1.02, 1.13 to 1.02, 1.11 to 1.01, 1.09 to 1.01,
                1.06 to 1.00, 1.04 to 1.00, 1.02 to 1.00, 1.00 to 1.00
            )
            "LAP-16" -> listOf(
                0.94 to 0.98, 0.95 to 0.99, 0.97 to 1.00, 0.96 to 1.00,
                0.98 to 1.01, 0.99 to 1.01, 0.98 to 1.00, 1.00 to 1.01,
                1.01 to 1.00, 1.02 to 1.01, 1.01 to 1.00, 1.00 to 1.00
            )
            "CAF-02" -> listOf(
                0.78 to 0.97, 0.80 to 0.98, 0.82 to 0.99, 0.84 to 1.00,
                0.86 to 1.00, 0.88 to 0.99, 0.91 to 1.00, 0.94 to 1.01,
                0.96 to 1.00, 0.98 to 1.00, 0.99 to 1.00, 1.00 to 1.00
            )
            else -> {
                val seed = (product.code.hashCode() and 0x7FFF)
                val trendSlope = when (product.verdict) {
                    PriceVerdict.GREEN -> 0.14
                    PriceVerdict.YELLOW -> 0.03
                    PriceVerdict.RED -> -0.15
                }
                List(12) { idx ->
                    val progress = idx / 11.0
                    val wave = kotlin.math.sin((idx + (seed % 5)) * 0.85) * 0.035 * (1.0 - progress)
                    val storeMult = (1.0 + trendSlope * (1.0 - progress) + wave).coerceIn(0.75, 1.35)
                    val avgMult = (1.0 + 0.02 * (1.0 - progress) - wave * 0.4).coerceIn(0.90, 1.12)
                    if (idx == 11) 1.0 to 1.0 else storeMult to avgMult
                }
            }
        }

        val startIdx = 12 - count
        return (startIdx until 12).map { i ->
            val (storeMult, avgMult) = fullMultipliers[i]
            val storeVal = ((product.storePrice * storeMult) * 100.0).roundToInt() / 100.0
            val avgVal = ((product.averagePrice * avgMult) * 100.0).roundToInt() / 100.0
            PriceHistoryPoint(
                labelEs = monthLabelsEs[i],
                labelEn = monthLabelsEn[i],
                storePrice = storeVal,
                marketAvgPrice = avgVal
            )
        }
    }
}

data class PriceHistoryPoint(
    val labelEs: String,
    val labelEn: String,
    val storePrice: Double,
    val marketAvgPrice: Double
)

