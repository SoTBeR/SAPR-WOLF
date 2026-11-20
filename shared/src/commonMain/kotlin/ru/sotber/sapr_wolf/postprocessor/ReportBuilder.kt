package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.nodeCount
import kotlin.math.abs
import kotlin.math.floor

enum class ReportFormat(val extension: String) {
    Html("html"),
    Text("txt"),
}

fun buildReport(
    construction: Construction,
    displacements: DoubleArray,
    projectName: String? = null,
    format: ReportFormat = ReportFormat.Text,
): String = when (format) {
    ReportFormat.Text -> buildTextReport(construction, displacements, projectName)
    ReportFormat.Html -> buildHtmlReport(construction, displacements, projectName)
}

fun buildTextReport(
    construction: Construction,
    displacements: DoubleArray,
    projectName: String? = null,
): String {
    val reactions = calculateReactions(construction, displacements)
    val equilibriumResidual = calculateEquilibriumResidual(construction, reactions)
    val strengths = analyzeStrength(construction, displacements)
    val summary = summarizePostprocessor(construction, displacements, strengths)
    val resolvedName = projectName?.takeIf { it.isNotBlank() } ?: "Без названия"

    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("ОТЧЁТ ПО РАСЧЁТУ СТЕРЖНЕВОЙ СИСТЕМЫ (SAPR-WOLF)")
    sb.appendLine("================================================================================")
    sb.appendLine("Проект:    $resolvedName")
    sb.appendLine("Стержней:  ${construction.rods.size}")
    sb.appendLine("Узлов:     ${construction.nodeCount}")
    sb.appendLine(
        "Опоры:     " + if (construction.fixedNodes.isEmpty()) {
            "нет"
        } else {
            construction.fixedNodes.sorted().joinToString(", ") { "узел ${it + 1}" }
        }
    )
    sb.appendLine()

    sb.appendLine("1. ИСХОДНЫЕ ДАННЫЕ — СТЕРЖНИ")
    sb.appendLine("--------------------------------------------------------------------------------")
    sb.appendLine(formatRow("Стержень", "L", "A", "E", "[σ]", "q"))
    sb.appendLine("--------------------------------------------------------------------------------")
    construction.rods.forEachIndexed { rodIndex, rod ->
        sb.appendLine(
            formatRow(
                "${rodIndex + 1}",
                formatReportNumber(rod.length),
                formatReportNumber(rod.crossSectionArea),
                formatReportNumber(rod.elasticModulus),
                formatReportNumber(rod.allowableStress),
                formatReportNumber(rod.distributedLoad),
            )
        )
    }
    sb.appendLine()

    sb.appendLine("2. РЕЗУЛЬТАТЫ ПО УЗЛАМ (ПЕРЕМЕЩЕНИЯ, СИЛЫ, РЕАКЦИИ)")
    sb.appendLine("--------------------------------------------------------------------------------")
    sb.appendLine(formatRow("Узел", "Опора", "F", "Δ", "R", ""))
    sb.appendLine("--------------------------------------------------------------------------------")
    for (nodeIndex in 0 until construction.nodeCount) {
        val isFixed = nodeIndex in construction.fixedNodes
        val force = construction.nodeForces.getOrElse(nodeIndex) { 0.0 }
        val displacement = displacements.getOrElse(nodeIndex) { 0.0 }
        val reaction = reactions[nodeIndex]
        sb.appendLine(
            formatRow(
                "${nodeIndex + 1}",
                if (isFixed) "да" else "нет",
                formatReportNumber(force),
                formatReportNumber(displacement),
                reaction?.let(::formatReportNumber) ?: "—",
                "",
            )
        )
    }
    sb.appendLine("Невязка статического равновесия ΣR + ΣF + Σ(q·L) = ${formatReportNumber(equilibriumResidual)}")
    sb.appendLine()

    sb.appendLine("3. НАПРЯЖЁННО-ДЕФОРМИРОВАННОЕ СОСТОЯНИЕ СТЕРЖНЕЙ (x = 0 и x = L)")
    sb.appendLine("--------------------------------------------------------------------------------")
    sb.appendLine(formatRow("Стержень", "x", "Nx", "σx", "Ux", ""))
    sb.appendLine("--------------------------------------------------------------------------------")
    construction.rods.forEachIndexed { rodIndex, rod ->
        val leftSection = calculateSection(construction, displacements, rodIndex, 0.0)
        val rightSection = calculateSection(construction, displacements, rodIndex, rod.length)
        sb.appendLine(
            formatRow(
                "${rodIndex + 1}",
                "0",
                formatReportNumber(leftSection.axialForce),
                formatReportNumber(leftSection.normalStress),
                formatReportNumber(leftSection.displacement),
                "",
            )
        )
        sb.appendLine(
            formatRow(
                "${rodIndex + 1}",
                formatReportNumber(rod.length),
                formatReportNumber(rightSection.axialForce),
                formatReportNumber(rightSection.normalStress),
                formatReportNumber(rightSection.displacement),
                "",
            )
        )
    }
    sb.appendLine()

    sb.appendLine("4. ПРОВЕРКА ПРОЧНОСТИ")
    sb.appendLine("--------------------------------------------------------------------------------")
    sb.appendLine(formatRow("Стержень", "σ_max", "[σ]", "n = [σ]/σ_max", "Статус", ""))
    sb.appendLine("--------------------------------------------------------------------------------")
    for (strength in strengths) {
        sb.appendLine(
            formatRow(
                "${strength.rodIndex + 1}",
                formatReportNumber(strength.maxStress),
                formatReportNumber(strength.allowableStress),
                formatReportNumber(strength.safetyFactor),
                if (strength.isSatisfied) "Выполнено" else "ПЕРЕГРУЗ",
                "",
            )
        )
    }
    sb.appendLine()

    sb.appendLine("5. СВОДКА")
    sb.appendLine("--------------------------------------------------------------------------------")
    val mostLoadedText = summary.mostLoadedRodIndex?.let { "стержень ${it + 1} (n = ${formatReportNumber(summary.minSafetyFactor)})" } ?: "—"
    sb.appendLine("Самый нагруженный стержень: $mostLoadedText")
    sb.appendLine("Макс. |Nx|:                 ${formatReportNumber(summary.maxAbsAxialForce)}")
    sb.appendLine("Макс. |σx|:                 ${formatReportNumber(summary.maxAbsNormalStress)}")
    sb.appendLine("Макс. |Ux|:                 ${formatReportNumber(summary.maxAbsDisplacement)}")
    sb.appendLine(
        "Условие прочности:          " + if (summary.overloadedRodIndices.isEmpty()) {
            "выполнено для всех стержней"
        } else {
            "нарушено в стержнях: ${summary.overloadedRodIndices.joinToString(", ") { "${it + 1}" }}"
        }
    )

    return sb.toString()
}

fun buildHtmlReport(
    construction: Construction,
    displacements: DoubleArray,
    projectName: String? = null,
): String {
    val reactions = calculateReactions(construction, displacements)
    val equilibriumResidual = calculateEquilibriumResidual(construction, reactions)
    val strengths = analyzeStrength(construction, displacements)
    val summary = summarizePostprocessor(construction, displacements, strengths)
    val resolvedName = escapeHtml(projectName?.takeIf { it.isNotBlank() } ?: "Без названия")

    val supportsText = if (construction.fixedNodes.isEmpty()) {
        "нет"
    } else {
        construction.fixedNodes.sorted().joinToString(", ") { "узел ${it + 1}" }
    }

    val sb = StringBuilder()
    sb.appendLine("<!DOCTYPE html>")
    sb.appendLine("<html lang=\"ru\">")
    sb.appendLine("<head>")
    sb.appendLine("  <meta charset=\"UTF-8\">")
    sb.appendLine("  <title>Отчёт SAPR-WOLF — $resolvedName</title>")
    sb.appendLine("  <style>")
    sb.appendLine("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #111113; background: #f8f8fa; margin: 32px; line-height: 1.5; }")
    sb.appendLine("    .container { max-width: 960px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e5ea; border-radius: 12px; padding: 28px 32px; }")
    sb.appendLine("    h1 { font-size: 22px; margin: 0 0 8px 0; }")
    sb.appendLine("    h2 { font-size: 16px; margin: 24px 0 10px 0; border-bottom: 1px solid #e5e5ea; padding-bottom: 6px; }")
    sb.appendLine("    .meta { color: #6e6e73; font-size: 13px; margin-bottom: 16px; }")
    sb.appendLine("    table { width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 12px; }")
    sb.appendLine("    th, td { border: 1px solid #e5e5ea; padding: 6px 10px; text-align: right; font-variant-numeric: tabular-nums; }")
    sb.appendLine("    th { background: #f2f2f4; font-weight: 600; color: #111113; }")
    sb.appendLine("    td:first-child, th:first-child { text-align: left; }")
    sb.appendLine("    .ok { color: #15803d; font-weight: 600; }")
    sb.appendLine("    .overload { color: #d92d20; font-weight: 700; background: #fef2f2; }")
    sb.appendLine("    .summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }")
    sb.appendLine("    .card { background: #f8f8fa; border: 1px solid #e5e5ea; border-radius: 8px; padding: 10px 14px; font-size: 13px; }")
    sb.appendLine("    .card-label { color: #6e6e73; font-size: 12px; }")
    sb.appendLine("    .card-value { font-size: 15px; font-weight: 600; margin-top: 2px; }")
    sb.appendLine("  </style>")
    sb.appendLine("</head>")
    sb.appendLine("<body>")
    sb.appendLine("<div class=\"container\">")
    sb.appendLine("  <h1>Отчёт по расчёту стержневой системы</h1>")
    sb.appendLine("  <div class=\"meta\">Проект: <strong>$resolvedName</strong> · Стержней: ${construction.rods.size} · Узлов: ${construction.nodeCount} · Опоры: $supportsText</div>")

    sb.appendLine("  <h2>1. Исходные данные — стержни</h2>")
    sb.appendLine("  <table>")
    sb.appendLine("    <thead><tr><th>Стержень</th><th>L</th><th>A</th><th>E</th><th>[σ]</th><th>q</th></tr></thead>")
    sb.appendLine("    <tbody>")
    construction.rods.forEachIndexed { rodIndex, rod ->
        sb.appendLine(
            "      <tr><td>${rodIndex + 1}</td>" +
                "<td>${formatReportNumber(rod.length)}</td>" +
                "<td>${formatReportNumber(rod.crossSectionArea)}</td>" +
                "<td>${formatReportNumber(rod.elasticModulus)}</td>" +
                "<td>${formatReportNumber(rod.allowableStress)}</td>" +
                "<td>${formatReportNumber(rod.distributedLoad)}</td></tr>"
        )
    }
    sb.appendLine("    </tbody>")
    sb.appendLine("  </table>")

    sb.appendLine("  <h2>2. Результаты по узлам</h2>")
    sb.appendLine("  <table>")
    sb.appendLine("    <thead><tr><th>Узел</th><th>Опора</th><th>F</th><th>Δ</th><th>R</th></tr></thead>")
    sb.appendLine("    <tbody>")
    for (nodeIndex in 0 until construction.nodeCount) {
        val isFixed = nodeIndex in construction.fixedNodes
        val force = construction.nodeForces.getOrElse(nodeIndex) { 0.0 }
        val displacement = displacements.getOrElse(nodeIndex) { 0.0 }
        val reaction = reactions[nodeIndex]
        sb.appendLine(
            "      <tr><td>${nodeIndex + 1}</td>" +
                "<td>${if (isFixed) "да" else "нет"}</td>" +
                "<td>${formatReportNumber(force)}</td>" +
                "<td>${formatReportNumber(displacement)}</td>" +
                "<td>${reaction?.let(::formatReportNumber) ?: "—"}</td></tr>"
        )
    }
    sb.appendLine("    </tbody>")
    sb.appendLine("  </table>")
    sb.appendLine("  <div class=\"meta\">Невязка статического равновесия ΣR + ΣF + Σ(q·L) = <strong>${formatReportNumber(equilibriumResidual)}</strong></div>")

    sb.appendLine("  <h2>3. Напряжённо-деформированное состояние стержней</h2>")
    sb.appendLine("  <table>")
    sb.appendLine("    <thead><tr><th>Стержень</th><th>x</th><th>N<sub>x</sub></th><th>σ<sub>x</sub></th><th>U<sub>x</sub></th></tr></thead>")
    sb.appendLine("    <tbody>")
    construction.rods.forEachIndexed { rodIndex, rod ->
        val leftSection = calculateSection(construction, displacements, rodIndex, 0.0)
        val rightSection = calculateSection(construction, displacements, rodIndex, rod.length)
        sb.appendLine(
            "      <tr><td>${rodIndex + 1}</td><td>0</td>" +
                "<td>${formatReportNumber(leftSection.axialForce)}</td>" +
                "<td>${formatReportNumber(leftSection.normalStress)}</td>" +
                "<td>${formatReportNumber(leftSection.displacement)}</td></tr>"
        )
        sb.appendLine(
            "      <tr><td>${rodIndex + 1}</td><td>${formatReportNumber(rod.length)}</td>" +
                "<td>${formatReportNumber(rightSection.axialForce)}</td>" +
                "<td>${formatReportNumber(rightSection.normalStress)}</td>" +
                "<td>${formatReportNumber(rightSection.displacement)}</td></tr>"
        )
    }
    sb.appendLine("    </tbody>")
    sb.appendLine("  </table>")

    sb.appendLine("  <h2>4. Проверка прочности</h2>")
    sb.appendLine("  <table>")
    sb.appendLine("    <thead><tr><th>Стержень</th><th>σ<sub>max</sub></th><th>[σ]</th><th>Запас n<sub>p</sub></th><th>Статус</th></tr></thead>")
    sb.appendLine("    <tbody>")
    for (strength in strengths) {
        val statusClass = if (strength.isSatisfied) "ok" else "overload"
        val statusLabel = if (strength.isSatisfied) "Выполнено" else "ПЕРЕГРУЗ (n < 1)"
        sb.appendLine(
            "      <tr class=\"${if (strength.isSatisfied) "" else "overload"}\">" +
                "<td>${strength.rodIndex + 1}</td>" +
                "<td>${formatReportNumber(strength.maxStress)}</td>" +
                "<td>${formatReportNumber(strength.allowableStress)}</td>" +
                "<td>${formatReportNumber(strength.safetyFactor)}</td>" +
                "<td class=\"$statusClass\">$statusLabel</td></tr>"
        )
    }
    sb.appendLine("    </tbody>")
    sb.appendLine("  </table>")

    sb.appendLine("  <h2>5. Сводка</h2>")
    val mostLoadedText = summary.mostLoadedRodIndex?.let {
        "Стержень ${it + 1} (n = ${formatReportNumber(summary.minSafetyFactor)})"
    } ?: "—"
    val strengthSummaryText = if (summary.overloadedRodIndices.isEmpty()) {
        "Выполнено для всех стержней"
    } else {
        "Перегруз: стержни ${summary.overloadedRodIndices.joinToString(", ") { "${it + 1}" }}"
    }
    sb.appendLine("  <div class=\"summary\">")
    sb.appendLine("    <div class=\"card\"><div class=\"card-label\">Самый нагруженный стержень</div><div class=\"card-value\">$mostLoadedText</div></div>")
    sb.appendLine("    <div class=\"card\"><div class=\"card-label\">Условие прочности</div><div class=\"card-value ${if (summary.overloadedRodIndices.isEmpty()) "ok" else "overload"}\">$strengthSummaryText</div></div>")
    sb.appendLine("    <div class=\"card\"><div class=\"card-label\">Макс. |N<sub>x</sub>| / |σ<sub>x</sub>|</div><div class=\"card-value\">${formatReportNumber(summary.maxAbsAxialForce)} / ${formatReportNumber(summary.maxAbsNormalStress)}</div></div>")
    sb.appendLine("    <div class=\"card\"><div class=\"card-label\">Макс. |U<sub>x</sub>|</div><div class=\"card-value\">${formatReportNumber(summary.maxAbsDisplacement)}</div></div>")
    sb.appendLine("  </div>")

    sb.appendLine("</div>")
    sb.appendLine("</body>")
    sb.appendLine("</html>")

    return sb.toString()
}

private fun formatRow(vararg columns: String): String {
    val widths = intArrayOf(10, 12, 14, 14, 14, 12)
    return columns.mapIndexed { index, col ->
        val width = widths.getOrElse(index) { 12 }
        if (index == 0) col.padEnd(width) else col.padStart(width)
    }.joinToString(" ").trimEnd()
}

private fun formatReportNumber(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    val factor = 1_000L
    val scaledDouble = abs(value) * factor.toDouble()
    if (!scaledDouble.isFinite() || scaledDouble >= (Long.MAX_VALUE / 2).toDouble()) {
        return value.toString()
    }
    val scaled = floor(scaledDouble + 0.5 + 1e-12).toLong()
    if (scaled == 0L) return "0"
    val sign = if (value < 0.0) "-" else ""
    val integerPart = scaled / factor
    val fractionalPart = (scaled % factor)
        .toString()
        .padStart(3, '0')
        .trimEnd('0')
    return if (fractionalPart.isEmpty()) {
        "$sign$integerPart"
    } else {
        "$sign$integerPart.$fractionalPart"
    }
}

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
