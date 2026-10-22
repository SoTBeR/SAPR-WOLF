package ru.sotber.sapr_wolf.processor

import ru.sotber.sapr_wolf.model.Construction

fun calculateDisplacements(construction: Construction): Result<DoubleArray> = runCatching {
    if (construction.rods.isEmpty()) {
        throw IllegalArgumentException("Конструкция не содержит стержней.")
    }
    if (construction.fixedNodes.isEmpty()) {
        throw IllegalStateException("В конструкции отсутствуют жёсткие опоры.")
    }

    val initialSystem = assembleGlobalSystem(construction)
    val boundaryConditionSystem = applyFixedNodes(initialSystem, construction.fixedNodes)
    solveByGauss(boundaryConditionSystem)
}
