package ru.sotber.sapr_wolf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.sotber.sapr_wolf.ui.theme.FieldShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoTextStyle

internal fun formatEditableNumber(value: Double): String {
    if (value == 0.0) return "0"
    val text = value.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

internal fun isValidPartialNumericInput(
    text: String,
    allowNegative: Boolean,
    allowScientificNotation: Boolean,
): Boolean {
    if (text.isEmpty()) return true

    var index = 0
    val length = text.length

    if (text[index] == '-') {
        if (!allowNegative) return false
        index++
        if (index == length) return true
    }

    var hasMantissaDigit = false
    var hasDecimalSeparator = false

    while (index < length) {
        val ch = text[index]
        when {
            ch.isDigit() -> {
                hasMantissaDigit = true
                index++
            }
            ch == '.' || ch == ',' -> {
                if (hasDecimalSeparator) return false
                hasDecimalSeparator = true
                index++
            }
            ch == 'e' || ch == 'E' -> {
                if (!allowScientificNotation || !hasMantissaDigit) return false
                index++
                break
            }
            else -> return false
        }
    }

    if (index < length) {
        if (text[index] == '+' || text[index] == '-') {
            index++
        }
        while (index < length) {
            if (!text[index].isDigit()) return false
            index++
        }
    }

    return true
}

@Composable
fun NumericTextField(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    elementKey: Any = Unit,
    allowNegative: Boolean = false,
    allowZero: Boolean = false,
    allowScientificNotation: Boolean = true,
    minValue: Double? = null,
    maxValue: Double? = null,
    clampToRangeOnCommit: Boolean = false,
    formatValue: (Double) -> String = ::formatEditableNumber,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialThemeSpacing.small,
        vertical = 5.dp,
    ),
) {
    val focusManager = LocalFocusManager.current
    var fieldValue by remember(elementKey) {
        val formatted = formatValue(value)
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }
    var isFocused by remember(elementKey) { mutableStateOf(false) }
    var ignoreNextFocusLossCommit by remember(elementKey) { mutableStateOf(false) }
    var keepSelectAllFromInitialFocus by remember(elementKey) { mutableStateOf(false) }

    LaunchedEffect(elementKey, value) {
        if (!isFocused) {
            val formatted = formatValue(value)
            fieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
        }
    }

    fun parseCurrent(): Double? = fieldValue.text.replace(',', '.').toDoubleOrNull()

    fun isInAllowedDomain(parsed: Double): Boolean {
        if (!parsed.isFinite()) return false
        if (!allowNegative && parsed < 0.0) return false
        if (!allowZero && parsed == 0.0) return false
        if (minValue != null && parsed < minValue) return false
        if (maxValue != null && parsed > maxValue) return false
        return true
    }

    fun commitValue() {
        val parsed = parseCurrent()
        if (parsed == null || !parsed.isFinite()) {
            val reverted = formatValue(value)
            fieldValue = TextFieldValue(text = reverted, selection = TextRange(reverted.length))
            return
        }

        if (clampToRangeOnCommit) {
            val lower = minValue ?: if (!allowNegative) 0.0 else -Double.MAX_VALUE
            val upper = maxValue ?: Double.MAX_VALUE
            val clamped = parsed.coerceIn(lower, upper)
            if (isInAllowedDomain(clamped)) {
                if (clamped != value) {
                    onValueChange(clamped)
                }
                val formatted = formatValue(clamped)
                fieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
            } else {
                val reverted = formatValue(value)
                fieldValue = TextFieldValue(text = reverted, selection = TextRange(reverted.length))
            }
            return
        }

        if (isInAllowedDomain(parsed)) {
            if (parsed != value) {
                onValueChange(parsed)
            }
            val formatted = formatValue(parsed)
            fieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        } else {
            val reverted = formatValue(value)
            fieldValue = TextFieldValue(text = reverted, selection = TextRange(reverted.length))
        }
    }

    fun cancelEdit() {
        ignoreNextFocusLossCommit = true
        val reverted = formatValue(value)
        fieldValue = TextFieldValue(text = reverted, selection = TextRange(reverted.length))
        focusManager.clearFocus()
    }

    val isInputInvalid = isFocused && run {
        val parsed = parseCurrent()
        parsed == null || !isInAllowedDomain(parsed)
    }

    val borderColor = when {
        isInputInvalid -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .clip(FieldShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = FieldShape,
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = FieldShape,
            )
            .padding(contentPadding),
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { candidate ->
                if (
                    keepSelectAllFromInitialFocus &&
                    candidate.text == fieldValue.text &&
                    candidate.selection.collapsed
                ) {
                    fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                    return@BasicTextField
                }
                keepSelectAllFromInitialFocus = false
                if (
                    isValidPartialNumericInput(
                        text = candidate.text,
                        allowNegative = allowNegative,
                        allowScientificNotation = allowScientificNotation,
                    )
                ) {
                    fieldValue = candidate
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            textStyle = MonoTextStyle.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(elementKey) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press && isFocused) {
                                keepSelectAllFromInitialFocus = false
                            }
                        }
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    keepSelectAllFromInitialFocus = false
                    when (keyEvent.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            focusManager.clearFocus()
                            true
                        }
                        Key.Escape -> {
                            cancelEdit()
                            true
                        }
                        else -> false
                    }
                }
                .onFocusChanged { focusState ->
                    if (!isFocused && focusState.isFocused) {
                        keepSelectAllFromInitialFocus = true
                        fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                    } else if (isFocused && !focusState.isFocused) {
                        keepSelectAllFromInitialFocus = false
                        if (ignoreNextFocusLossCommit) {
                            ignoreNextFocusLossCommit = false
                        } else {
                            commitValue()
                        }
                    }
                    isFocused = focusState.isFocused
                },
        )
    }
}
