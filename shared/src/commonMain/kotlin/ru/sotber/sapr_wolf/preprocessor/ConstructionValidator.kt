package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.ValidationIssue
import ru.sotber.sapr_wolf.model.ValidationSeverity
import ru.sotber.sapr_wolf.model.nodeCount
import kotlin.math.abs

object ConstructionValidator {

    fun validate(construction: Construction): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val totalNodes = construction.nodeCount

        if (construction.rods.isEmpty()) {
            issues.add(
                ValidationIssue(
                    code = "E01",
                    severity = ValidationSeverity.ERROR,
                    message = "Нет ни одного стержня. Добавьте хотя бы один стержень."
                )
            )

            return issues
        }

        if (construction.nodeForces.isNotEmpty() && construction.nodeForces.size != totalNodes) {
            issues.add(
                ValidationIssue(
                    code = "E09",
                    severity = ValidationSeverity.ERROR,
                    message = "Нарушена структура данных: количество узловых сил (${construction.nodeForces.size}) должно равняться числу узлов ($totalNodes)."
                )
            )
        }

        for (rodIndex in construction.rods.indices) {
            val rod = construction.rods[rodIndex]

            val hasInvalidNumberInRod = rod.length.isNaN() || rod.length.isInfinite() ||
                    rod.crossSectionArea.isNaN() || rod.crossSectionArea.isInfinite() ||
                    rod.elasticModulus.isNaN() || rod.elasticModulus.isInfinite() ||
                    rod.allowableStress.isNaN() || rod.allowableStress.isInfinite() ||
                    rod.distributedLoad.isNaN() || rod.distributedLoad.isInfinite()

            if (hasInvalidNumberInRod) {
                issues.add(
                    ValidationIssue(
                        code = "E08",
                        severity = ValidationSeverity.ERROR,
                        message = "Некорректное значение (NaN или бесконечность) в стержне ${rodIndex + 1}.",
                        rodIndex = rodIndex
                    )
                )
            }

            if (rod.length <= 0.0) {
                issues.add(
                    ValidationIssue(
                        code = "E02",
                        severity = ValidationSeverity.ERROR,
                        message = "Длина стержня ${rodIndex + 1} должна быть строго положительной (L > 0).",
                        rodIndex = rodIndex
                    )
                )
            }

            if (rod.crossSectionArea <= 0.0) {
                issues.add(
                    ValidationIssue(
                        code = "E03",
                        severity = ValidationSeverity.ERROR,
                        message = "Площадь поперечного сечения стержня ${rodIndex + 1} должна быть строго положительной (A > 0).",
                        rodIndex = rodIndex
                    )
                )
            }

            if (rod.elasticModulus <= 0.0) {
                issues.add(
                    ValidationIssue(
                        code = "E04",
                        severity = ValidationSeverity.ERROR,
                        message = "Модуль упругости стержня ${rodIndex + 1} должен быть строго положительным (E > 0).",
                        rodIndex = rodIndex
                    )
                )
            }

            if (rod.allowableStress <= 0.0) {
                issues.add(
                    ValidationIssue(
                        code = "E05",
                        severity = ValidationSeverity.ERROR,
                        message = "Допускаемое напряжение стержня ${rodIndex + 1} должно быть строго положительным ([σ] > 0).",
                        rodIndex = rodIndex
                    )
                )
            }
        }

        for (nodeIndex in construction.nodeForces.indices) {
            val force = construction.nodeForces[nodeIndex]
            if (force.isNaN() || force.isInfinite()) {
                issues.add(
                    ValidationIssue(
                        code = "E08",
                        severity = ValidationSeverity.ERROR,
                        message = "Некорректное значение внешней силы (NaN или бесконечность) в узле ${nodeIndex + 1}.",
                        nodeIndex = nodeIndex
                    )
                )
            }
        }

        if (construction.fixedNodes.isEmpty()) {
            issues.add(
                ValidationIssue(
                    code = "E06",
                    severity = ValidationSeverity.ERROR,
                    message = "Нет опор: конструкция может свободно перемещаться как жёсткое целое."
                )
            )
        }

        for (fixedNodeIndex in construction.fixedNodes) {
            if (fixedNodeIndex !in 0 until totalNodes) {
                issues.add(
                    ValidationIssue(
                        code = "E07",
                        severity = ValidationSeverity.ERROR,
                        message = "Номер узла с опорой ($fixedNodeIndex) вне допустимого диапазона [0, ${totalNodes - 1}].",
                        nodeIndex = fixedNodeIndex
                    )
                )
            }
        }

        val hasAnyNodeForce = construction.nodeForces.any { abs(it) > 1e-12 }
        val hasAnyDistributedLoad = construction.rods.any { abs(it.distributedLoad) > 1e-12 }
        if (!hasAnyNodeForce && !hasAnyDistributedLoad) {
            issues.add(
                ValidationIssue(
                    code = "W01",
                    severity = ValidationSeverity.WARNING,
                    message = "Нагрузок нет: все перемещения узлов и внутренние усилия будут нулевыми."
                )
            )
        }

        for (fixedNodeIndex in construction.fixedNodes) {
            if (fixedNodeIndex in construction.nodeForces.indices) {
                val forceAtSupport = construction.nodeForces[fixedNodeIndex]
                if (abs(forceAtSupport) > 1e-12) {
                    issues.add(
                        ValidationIssue(
                            code = "W02",
                            severity = ValidationSeverity.WARNING,
                            message = "Сила приложена в узле ${fixedNodeIndex + 1} с опорой: она будет полностью воспринята опорой.",
                            nodeIndex = fixedNodeIndex
                        )
                    )
                }
            }
        }

        return issues
    }
    fun hasErrors(construction: Construction): Boolean =
        validate(construction).any { it.isError }
}
