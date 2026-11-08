package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.ui.theme.CapsuleShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold

enum class MainAppBarButtonStyle {
    Ghost,
    Segment,
    Tonal,
    Primary,
    DestructiveTonal,
}

@Composable
fun MainAppBarButton(
    textRes: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: DrawableResource? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    style: MainAppBarButtonStyle = MainAppBarButtonStyle.Ghost,
    shape: Shape = CapsuleShape,
    textStyle: TextStyle? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialThemeSpacing.mediumSmall,
        vertical = MaterialThemeSpacing.small,
    ),
) {
    MainAppBarButton(
        text = stringResource(textRes),
        onClick = onClick,
        modifier = modifier,
        iconRes = iconRes,
        enabled = enabled,
        isSelected = isSelected,
        style = style,
        shape = shape,
        textStyle = textStyle,
        contentPadding = contentPadding,
    )
}

@Composable
fun MainAppBarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: DrawableResource? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    style: MainAppBarButtonStyle = MainAppBarButtonStyle.Ghost,
    shape: Shape = CapsuleShape,
    textStyle: TextStyle? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialThemeSpacing.mediumSmall,
        vertical = MaterialThemeSpacing.small,
    ),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor by animateColorAsState(
        targetValue = MainAppBarDefaults.containerColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )
    val contentColor by animateColorAsState(
        targetValue = MainAppBarDefaults.contentColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )

    val resolvedTextStyle = textStyle ?: if (isSelected || style == MainAppBarButtonStyle.Primary) {
        MaterialTheme.typography.labelMediumBold
    } else {
        MaterialTheme.typography.labelMediumBold // labelMedium
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = MaterialThemeSpacing.small,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding),
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }

            Text(
                text = text,
                style = resolvedTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MainAppBarIconButton(
    iconRes: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    style: MainAppBarButtonStyle = MainAppBarButtonStyle.Ghost,
    shape: Shape = CapsuleShape,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor by animateColorAsState(
        targetValue = MainAppBarDefaults.containerColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )
    val contentColor by animateColorAsState(
        targetValue = MainAppBarDefaults.contentColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(MaterialThemeSpacing.small),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        }
    }
}
