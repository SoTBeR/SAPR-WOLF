package ru.sotber.sapr_wolf.ui.components.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.ui.modifiers.pointerCursor
import ru.sotber.sapr_wolf.ui.theme.*
import sapr_wolf.shared.generated.resources.*

@Composable
fun StartupSplashOverlay(
    visible: Boolean,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val isDarkTheme = MaterialThemeExtraColors.isDark
    val canvasBaseColor = MaterialThemeExtraColors.canvasBase
    val backdropBlurRadius = MaterialThemeBlurRadius.level2

    val scrimTint = remember(isDarkTheme) {
        if (isDarkTheme) {
            Color(0xFF060608).copy(alpha = 0.56f)
        } else {
            Color(0xFF101116).copy(alpha = 0.28f)
        }
    }

    val backdropBlurStyle = remember(scrimTint, canvasBaseColor, backdropBlurRadius) {
        HazeBlurStyle {
            blurRadius(backdropBlurRadius)
            noiseFactor(0f)
            backgroundColor(canvasBaseColor)
            colorEffects(listOf(HazeColorEffect.tint(scrimTint)))
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .hazeBlur(
                input = HazeInput.Backdrop(hazeState),
                style = backdropBlurStyle,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss,
            )
            .padding(MaterialThemeSpacing.large),
    ) {
        StartupSplashCard(
            isDarkTheme = isDarkTheme,
        )
    }
}

@Composable
private fun StartupSplashCard(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardShape = MaterialTheme.shapes.extraLarge
    val iconShape = RoundedCornerShape(16.dp)
    val cardHazeState = rememberHazeState()
    val badgeBlurRadius = MaterialThemeBlurRadius.level2

    val cardBorderColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }
    val iconBorderColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.16f)
    }
    val badgeTintColor = if (isDarkTheme) {
        Color(0xFF0E0F14).copy(alpha = 0.56f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
    val badgeFallbackBg = if (isDarkTheme) {
        Color(0xFF14151B)
    } else {
        Color(0xFFF2F3F7)
    }
    val badgeBlurStyle = remember(badgeTintColor, badgeFallbackBg, badgeBlurRadius) {
        HazeBlurStyle {
            blurRadius(badgeBlurRadius)
            noiseFactor(0f)
            backgroundColor(badgeFallbackBg)
            colorEffects(listOf(HazeColorEffect.tint(badgeTintColor)))
        }
    }
    val badgeBorderColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.14f)
    }
    val primaryTextColor = if (isDarkTheme) {
        Color(0xFFF5F5F7)
    } else {
        Color(0xFF111113)
    }
    val secondaryTextColor = if (isDarkTheme) {
        Color(0xFFE2E4EC).copy(alpha = 0.92f)
    } else {
        Color(0xFF222329).copy(alpha = 0.90f)
    }
    val mutedTextColor = if (isDarkTheme) {
        Color(0xFFB8BCC8).copy(alpha = 0.86f)
    } else {
        Color(0xFF454752).copy(alpha = 0.86f)
    }
    val dividerColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }
    val readabilityScrim = if (isDarkTheme) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF090A0D).copy(alpha = 0.08f),
                0.30f to Color(0xFF090A0D).copy(alpha = 0.28f),
                0.56f to Color(0xFF090A0D).copy(alpha = 0.74f),
                0.80f to Color(0xFF090A0D).copy(alpha = 0.90f),
                1.0f to Color(0xFF090A0D).copy(alpha = 0.95f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFF6F7FA).copy(alpha = 0.08f),
                0.30f to Color(0xFFF6F7FA).copy(alpha = 0.26f),
                0.56f to Color(0xFFF5F6FA).copy(alpha = 0.74f),
                0.80f to Color(0xFFF3F4F8).copy(alpha = 0.90f),
                1.0f to Color(0xFFF1F2F6).copy(alpha = 0.95f),
            ),
        )
    }

    val versionValue = stringResource(Res.string.help_about_version_value)

    Box(
        modifier = modifier
            .widthIn(min = 300.dp, max = 540.dp)
            .fillMaxWidth()
            .height(324.dp)
            .shadow(
                elevation = 28.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.65f else 0.25f),
                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.75f else 0.30f),
            )
            .clip(cardShape)
            .pointerCursor()
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = cardShape,
            ),
    ) {
        SplashShaderBackground(
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(cardHazeState),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(readabilityScrim),
        )

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_app_logo),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = iconShape,
                            ambientColor = Color.Black.copy(alpha = 0.4f),
                            spotColor = Color.Black.copy(alpha = 0.5f),
                        )
                        .clip(iconShape)
                        .border(
                            width = 1.dp,
                            color = iconBorderColor,
                            shape = iconShape,
                        ),
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .hazeBlur(
                            input = HazeInput.Backdrop(cardHazeState),
                            style = badgeBlurStyle,
                        )
                        .border(
                            width = 1.dp,
                            color = badgeBorderColor,
                            shape = CapsuleShape,
                        )
                        .padding(
                            horizontal = MaterialThemeSpacing.mediumSmall,
                            vertical = 5.dp,
                        ),
                ) {
                    Text(
                        text = stringResource(Res.string.splash_version_badge, versionValue),
                        style = MonoSmallTextStyle,
                        color = primaryTextColor,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.6).sp,
                        ),
                        color = primaryTextColor,
                    )
                    Text(
                        text = stringResource(Res.string.help_about_description),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        ),
                        color = secondaryTextColor,
                    )
                }

                HorizontalDivider(color = dividerColor)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.powered_by_sapp),
                        style = MonoSmallTextStyle,
                        color = mutedTextColor,
                    )
                    Text(
                        text = stringResource(Res.string.splash_dismiss_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedTextColor,
                    )
                }
            }
        }
    }
}
