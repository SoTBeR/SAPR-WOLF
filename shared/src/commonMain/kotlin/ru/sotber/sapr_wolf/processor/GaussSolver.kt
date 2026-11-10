package ru.sotber.sapr_wolf.processor

import ru.sotber.sapr_wolf.model.LinearSystem
import kotlin.math.abs

private const val EPSILON = 1e-12

fun solveByGauss(system: LinearSystem): DoubleArray {
    val systemSize = system.size
    if (systemSize == 0) return DoubleArray(0)

    val matrix = Array(systemSize) { rowIndex -> system.matrix[rowIndex].copyOf() }
    val rightPart = system.rightPart.copyOf()

    for (stepIndex in 0 until systemSize) {
        var maxRowIndex = stepIndex
        var maxPivotValue = abs(matrix[stepIndex][stepIndex])

        for (candidateRowIndex in (stepIndex + 1) until systemSize) {
            val candidateValue = abs(matrix[candidateRowIndex][stepIndex])
            if (candidateValue > maxPivotValue) {
                maxPivotValue = candidateValue
                maxRowIndex = candidateRowIndex
            }
        }

        if (maxPivotValue < EPSILON) {
            throw IllegalStateException(
                "Матрица системы вырождена или близка к вырождению на шаге $stepIndex (главный элемент $maxPivotValue < $EPSILON). " +
                        "Проверьте наличие жёстких опор и геометрическую неизменяемость конструкции."
            )
        }

        if (maxRowIndex != stepIndex) {
            val tempRow = matrix[stepIndex]
            matrix[stepIndex] = matrix[maxRowIndex]
            matrix[maxRowIndex] = tempRow

            val tempRightPart = rightPart[stepIndex]
            rightPart[stepIndex] = rightPart[maxRowIndex]
            rightPart[maxRowIndex] = tempRightPart
        }

        val pivot = matrix[stepIndex][stepIndex]
        for (targetRowIndex in (stepIndex + 1) until systemSize) {
            val factor = matrix[targetRowIndex][stepIndex] / pivot
            matrix[targetRowIndex][stepIndex] = 0.0

            for (colIndex in (stepIndex + 1) until systemSize) {
                matrix[targetRowIndex][colIndex] -= factor * matrix[stepIndex][colIndex]
            }
            rightPart[targetRowIndex] -= factor * rightPart[stepIndex]
        }
    }

    val solution = DoubleArray(systemSize)
    for (stepIndex in (systemSize - 1) downTo 0) {
        var accumulatedSum = rightPart[stepIndex]
        for (colIndex in (stepIndex + 1) until systemSize) {
            accumulatedSum -= matrix[stepIndex][colIndex] * solution[colIndex]
        }
        solution[stepIndex] = accumulatedSum / matrix[stepIndex][stepIndex]
    }

    return solution
}
