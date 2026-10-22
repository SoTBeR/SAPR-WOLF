package ru.sotber.sapr_wolf.model

class LinearSystem(
    val matrix: Array<DoubleArray>,
    val rightPart: DoubleArray,
) {
    val size: Int get() = rightPart.size

    fun copy(): LinearSystem {
        val newMatrix = Array(matrix.size) {
            i -> matrix[i].copyOf()
        }

        val newRightPart = rightPart.copyOf()

        return LinearSystem(
            matrix = newMatrix,
            rightPart = newRightPart,
        )
    }
}
