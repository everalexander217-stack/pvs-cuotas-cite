package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.data.InstallmentEntity
import com.example.data.ProductCodeInfo
import com.example.ui.theme.AppStrings
import kotlin.math.abs

/**
 * Builds a clean, pre-filled share message containing the scanned/verified product name,
 * code, price vs. market average, PVS verdict, and estimated biweekly installment details.
 */
fun buildProductShareText(
    product: ProductCodeInfo,
    scannedBarcode: String?,
    strings: AppStrings
): String {
    val diffPct = product.percentageDifference
    val diffLabel = if (diffPct <= 0) {
        strings.verifierSavingsPct(abs(diffPct))
    } else {
        strings.verifierMarkupPct(diffPct)
    }

    val codeText = if (!scannedBarcode.isNullOrBlank() && !scannedBarcode.equals(product.code, ignoreCase = true)) {
        "${product.code} ($scannedBarcode)"
    } else {
        product.code
    }

    val planSummary = strings.verifierPlanSummary(
        product.quotasCount,
        formatUsd(product.suggestedInitial)
    )

    return buildString {
        appendLine(strings.shareCardHeader)
        appendLine()
        appendLine("${product.name} (${product.category})")
        appendLine(strings.shareLineCode(codeText))
        appendLine(
            strings.shareLinePrice(
                formatUsd(product.storePrice),
                formatUsd(product.averagePrice),
                diffLabel
            )
        )
        appendLine(strings.shareLineVerdict(strings.verdictHeadline(product.verdict)))
        append(
            strings.shareLineInstallments(
                formatUsd(product.biweeklyImpact),
                planSummary
            )
        )
    }
}

/**
 * Builds a pre-filled share message for an active installment plan in Mis Cuotas.
 */
fun buildInstallmentShareText(
    item: InstallmentEntity,
    strings: AppStrings
): String {
    return buildString {
        appendLine(strings.shareInstallmentHeader)
        appendLine()
        appendLine("${item.title} (${item.category})")
        if (item.code.isNotBlank()) {
            appendLine(strings.shareLineCode(item.code))
        }
        appendLine(
            strings.shareInstallmentTotalLine(
                formatUsd(item.totalAmount),
                formatUsd(item.initialPaid)
            )
        )
        append(
            strings.shareInstallmentScheduleLine(
                formatUsd(item.biweeklyQuota),
                strings.quotasOfTotal(item.remainingQuotas, item.totalQuotas),
                item.nextPaymentDate
            )
        )
    }
}

/**
 * Creates a system share chooser Intent configured with text/plain extras.
 */
fun buildShareChooserIntent(
    subject: String,
    message: String,
    chooserTitle: String,
    isActivityContext: Boolean = true
): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, message)
    }
    return Intent.createChooser(sendIntent, chooserTitle).apply {
        if (!isActivityContext) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

fun shareProductViaChooser(
    context: Context,
    product: ProductCodeInfo,
    scannedBarcode: String?,
    strings: AppStrings
) {
    val message = buildProductShareText(product, scannedBarcode, strings)
    val subject = strings.shareSubjectProduct(product.name)
    val chooser = buildShareChooserIntent(
        subject = subject,
        message = message,
        chooserTitle = strings.shareChooserTitle,
        isActivityContext = context is Activity
    )
    runCatching { context.startActivity(chooser) }
}

fun shareInstallmentViaChooser(
    context: Context,
    item: InstallmentEntity,
    strings: AppStrings
) {
    val message = buildInstallmentShareText(item, strings)
    val subject = strings.shareSubjectProduct(item.title)
    val chooser = buildShareChooserIntent(
        subject = subject,
        message = message,
        chooserTitle = strings.shareChooserTitle,
        isActivityContext = context is Activity
    )
    runCatching { context.startActivity(chooser) }
}
