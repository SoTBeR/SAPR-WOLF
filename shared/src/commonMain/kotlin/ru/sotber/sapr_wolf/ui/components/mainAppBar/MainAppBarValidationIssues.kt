package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import ru.sotber.sapr_wolf.model.ValidationIssue
import ru.sotber.sapr_wolf.model.localizedMessage
import ru.sotber.sapr_wolf.ui.theme.CapsuleShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoSmallTextStyle
import ru.sotber.sapr_wolf.ui.theme.Shapes
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold

@Composable
fun MainAppBarValidationIssues(
    hazeState: HazeState?,
    issues: List<ValidationIssue>,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = MaterialThemeExtraColors.warning

    val sortedIssues = remember(issues) {
        issues.sortedBy { if (it.isError) 0 else 1 }
    }
    val errorCount = remember(issues) { issues.count { it.isError } }
    val warningCount = remember(issues) { issues.count { it.isWarning } }

    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialThemeSpacing.extraSmall),
        ) {
            Text(
                text = "Проблемы конструкции",
                style = MaterialTheme.typography.labelMediumBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (errorCount > 0) {
                    ValidationCountBadge(
                        count = errorCount,
                        accentColor = errorColor,
                    )
                }

                if (warningCount > 0) {
                    ValidationCountBadge(
                        count = warningCount,
                        accentColor = warningColor,
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            for (issue in sortedIssues) {
                ValidationIssueRow(
                    issue = issue,
                    accentColor = if (issue.isError) errorColor else warningColor,
                )
            }
        }
    }
}

@Composable
private fun ValidationCountBadge(
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = accentColor.copy(alpha = 0.25f),
                shape = CapsuleShape,
            )
            .padding(
                horizontal = MaterialThemeSpacing.small,
                vertical = MaterialThemeSpacing.extraSmall,
            ),
    ) {
        Text(
            text = count.toString(),
            style = MonoSmallTextStyle.copy(color = accentColor),
            maxLines = 1,
        )
    }
}

@Composable
private fun ValidationIssueRow(
    issue: ValidationIssue,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    color = accentColor.copy(alpha = 0.25f),
                    shape = Shapes.extraSmall,
                )
                .padding(
                    horizontal = MaterialThemeSpacing.small,
                    vertical = MaterialThemeSpacing.extraSmall,
                ),
        ) {
            Text(
                text = issue.code,
                style = MonoSmallTextStyle.copy(color = accentColor),
                maxLines = 1,
            )
        }

        Text(
            text = issue.localizedMessage(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
