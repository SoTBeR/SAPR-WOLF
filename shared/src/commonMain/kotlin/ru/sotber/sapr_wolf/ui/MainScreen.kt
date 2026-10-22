package ru.sotber.sapr_wolf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.ui.theme.CanvasBaseDark

@Composable
fun MainScreen() {
    var construction by remember {
        mutableStateOf(
            Construction(
                rods = listOf(
                    Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 1.0),
                    Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
                    Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
                    Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = -1.0)
                ),
                nodeForces = listOf(0.0, 0.0, 1.0, 0.0, 0.0),
                fixedNodes = setOf(0, 4)
            )
        )
    }

    var selectedRodIndex by remember { mutableStateOf<Int?>(null) }
    var selectedNodeIndex by remember { mutableStateOf<Int?>(null) }
    val viewportState = remember { ViewportState() }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBaseDark)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Spacebar) {
                    val isShift = keyEvent.isShiftPressed
                    viewportState.resetTo1to1(horizontalOnly = isShift)
                    true
                } else {
                    false
                }
            }
    ) {
        ConstructionCanvas(
            construction = construction,
            viewportState = viewportState,
            selectedRodIndex = selectedRodIndex,
            selectedNodeIndex = selectedNodeIndex,
            onSelectRod = { rodIdx ->
                selectedRodIndex = rodIdx
                selectedNodeIndex = null
                focusRequester.requestFocus()
            },
            onSelectNode = { nodeIdx ->
                selectedNodeIndex = nodeIdx
                selectedRodIndex = null
                focusRequester.requestFocus()
            },
            onAddRodLeft = {
                val firstRod = construction.rods.firstOrNull()
                val newRod = firstRod?.copy(distributedLoad = 0.0)
                    ?: Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0)

                val wasLeftEdgeFixed = 0 in construction.fixedNodes
                val newFixedNodes = if (wasLeftEdgeFixed) {
                    (construction.fixedNodes - 0).map { it + 1 }.toSet() + 0
                } else {
                    construction.fixedNodes.map { it + 1 }.toSet()
                }

                construction = construction.copy(
                    rods = listOf(newRod) + construction.rods,
                    nodeForces = listOf(0.0) + construction.nodeForces,
                    fixedNodes = newFixedNodes
                )
                selectedRodIndex = selectedRodIndex?.let { it + 1 }
                selectedNodeIndex = selectedNodeIndex?.let { it + 1 }
                focusRequester.requestFocus()
            },
            onAddRodRight = {
                val lastRod = construction.rods.lastOrNull()
                val newRod = lastRod?.copy(distributedLoad = 0.0)
                    ?: Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0)

                val oldLastNodeIdx = construction.rods.size
                val wasRightEdgeFixed = oldLastNodeIdx in construction.fixedNodes
                val newLastNodeIdx = oldLastNodeIdx + 1
                val newFixedNodes = if (wasRightEdgeFixed) {
                    (construction.fixedNodes - oldLastNodeIdx) + newLastNodeIdx
                } else {
                    construction.fixedNodes
                }

                construction = construction.copy(
                    rods = construction.rods + newRod,
                    nodeForces = construction.nodeForces + 0.0,
                    fixedNodes = newFixedNodes
                )
                focusRequester.requestFocus()
            }
        )
    }
}