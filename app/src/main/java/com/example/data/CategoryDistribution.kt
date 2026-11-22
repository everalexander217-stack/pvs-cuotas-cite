package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs

enum class CategoryMetricMode {
    BIWEEKLY,
    TOTAL
}

data class CategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val planCount: Int,
    val color: Color
)

object CategoryCatalog {
    val defaultPresets: List<String> = listOf(
        "Tecnología",
        "Hogar",
        "Salud",
        "Telefonía",
        "Audio",
        "Calzado",
        "Computación",
        "Educación",
        "Transporte",
        "General"
    )

    private val customPalette: List<Color> = listOf(
        Color(0xFFEC4899), // Rose
        Color(0xFF06B6D4), // Cyan
        Color(0xFF84CC16), // Lime
        Color(0xFFA855F7), // Purple
        Color(0xFFF97316), // Orange
        Color(0xFF3B82F6), // Blue
        Color(0xFF14B8A6), // Teal
        Color(0xFFEAB308)  // Gold
    )

    fun normalizeCategory(raw: String): String {
        return raw.trim()
            .replace(Regex("\\s+"), " ")
            .take(40)
            .ifBlank { "General" }
    }

    fun collectAvailableCategories(installments: List<InstallmentEntity>): List<String> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<String>()
        defaultPresets.forEach { preset ->
            val key = preset.lowercase()
            if (seen.add(key)) result.add(preset)
        }
        installments.forEach { item ->
            val normalized = normalizeCategory(item.category)
            if (seen.add(normalized.lowercase())) {
                result.add(normalized)
            }
        }
        return result
    }

    fun colorForCategory(category: String, fallbackIndex: Int = 0): Color {
        return when (category.trim().lowercase()) {
            "tecnología", "tecnologia", "tech" -> Color(0xFF38B261)
            "hogar", "casa", "home" -> Color(0xFFF59E0B)
            "salud", "health", "médico", "medico" -> Color(0xFFF43F5E)
            "telefonía", "telefonia", "celular", "celulares" -> Color(0xFF0EA5E9)
            "audio", "sonido", "música", "musica" -> Color(0xFF8B5CF6)
            "calzado", "ropa", "moda" -> Color(0xFF14B8A6)
            "computación", "computacion", "laptop", "pc" -> Color(0xFF10B981)
            "educación", "educacion", "estudios" -> Color(0xFF3B82F6)
            "transporte", "auto", "vehículo", "vehiculo" -> Color(0xFFF97316)
            "general", "otros" -> Color(0xFF64748B)
            else -> {
                val hash = abs(category.trim().lowercase().hashCode() + fallbackIndex)
                customPalette[hash % customPalette.size]
            }
        }
    }

    fun iconForCategory(category: String): ImageVector {
        return when (category.trim().lowercase()) {
            "tecnología", "tecnologia", "tech" -> Icons.Rounded.Tv
            "hogar", "casa", "home" -> Icons.Rounded.Home
            "salud", "health", "médico", "medico" -> Icons.Rounded.Favorite
            "telefonía", "telefonia", "celular", "celulares" -> Icons.Rounded.PhoneAndroid
            "audio", "sonido", "música", "musica" -> Icons.Rounded.Headphones
            "calzado", "ropa", "moda" -> Icons.Rounded.SportsScore
            "computación", "computacion", "laptop", "pc" -> Icons.Rounded.LaptopMac
            "educación", "educacion", "estudios" -> Icons.Rounded.School
            "transporte", "auto", "vehículo", "vehiculo" -> Icons.Rounded.DirectionsCar
            "mascotas", "pets" -> Icons.Rounded.Pets
            "gimnasio", "deporte", "fitness" -> Icons.Rounded.FitnessCenter
            "comida", "restaurante", "alimentos" -> Icons.Rounded.Restaurant
            "general" -> Icons.Rounded.ShoppingBag
            else -> Icons.AutoMirrored.Rounded.Label
        }
    }

    fun computeDistribution(
        installments: List<InstallmentEntity>,
        mode: CategoryMetricMode
    ): List<CategorySlice> {
        val relevant = when (mode) {
            CategoryMetricMode.BIWEEKLY -> installments.filter { !it.isCompleted && it.biweeklyQuota > 0.0 }
            CategoryMetricMode.TOTAL -> installments.filter { it.totalAmount > 0.0 }
        }
        if (relevant.isEmpty()) return emptyList()

        val grouped = LinkedHashMap<String, MutableList<InstallmentEntity>>()
        val canonicalLabel = LinkedHashMap<String, String>()

        relevant.forEach { item ->
            val clean = normalizeCategory(item.category)
            val key = clean.lowercase()
            if (!canonicalLabel.containsKey(key)) {
                canonicalLabel[key] = clean
            }
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }

        val sums = grouped.entries.map { (key, items) ->
            val label = canonicalLabel[key] ?: key
            val sum = items.sumOf {
                if (mode == CategoryMetricMode.BIWEEKLY) it.biweeklyQuota else it.totalAmount
            }
            Triple(label, sum, items.size)
        }.sortedByDescending { it.second }

        val grandTotal = sums.sumOf { it.second }
        if (grandTotal <= 0.0) return emptyList()

        return sums.mapIndexed { index, (label, amount, count) ->
            CategorySlice(
                category = label,
                amount = amount,
                percentage = (amount / grandTotal).toFloat().coerceIn(0f, 1f),
                planCount = count,
                color = colorForCategory(label, fallbackIndex = index)
            )
        }
    }
}
