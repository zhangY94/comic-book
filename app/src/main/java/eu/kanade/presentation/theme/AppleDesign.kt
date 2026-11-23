package eu.kanade.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Search

/**
 * Measurements of the iOS-flavoured layout used by the custom screens.
 * Keep new screens on these numbers so the whole app stays visually consistent.
 */
object AppleMetrics {
    /** Corner radius of a grouped list card. */
    val GroupRadius = 14.dp

    /** Horizontal padding inside a list row. */
    val RowHorizontalPadding = 14.dp

    /** Vertical padding inside a list row. */
    val RowVerticalPadding = 10.dp

    /** Horizontal padding of section headers and large titles. */
    val SectionHorizontalPadding = 20.dp

    /** Distance between a section header and the card below it. */
    val SectionTopPadding = 18.dp

    /** Page side margin for cards. */
    val PageHorizontalPadding = 16.dp

    /** Thickness of the separator inside a card. */
    val Hairline = 0.5.dp

    /** Minimum touch target height of an interactive row. */
    val MinRowHeight = 44.dp
}

/**
 * Colors shared by the custom screens. They are derived from the active [MaterialTheme]
 * color scheme so light/dark and AMOLED keep working without extra branches.
 */
@Immutable
data class ApplePalette(
    val page: Color,
    val card: Color,
    val field: Color,
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val tint: Color,
    val error: Color,
)

@Composable
@ReadOnlyComposable
fun applePalette(): ApplePalette = with(MaterialTheme.colorScheme) {
    ApplePalette(
        page = background,
        card = surfaceContainerLowest,
        field = secondaryContainer,
        separator = outlineVariant,
        label = onBackground,
        secondaryLabel = onSurfaceVariant,
        tertiaryLabel = outline,
        tint = primary,
        error = error,
    )
}

/**
 * A row of a grouped list. Rows are drawn individually so that lazy lists keep working;
 * [isFirst] / [isLast] round the card's outer corners and hide the top separator.
 */
@Composable
fun AppleGroupRow(
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = applePalette()
    val radius = AppleMetrics.GroupRadius
    val shape: Shape = when {
        isFirst && isLast -> RoundedCornerShape(radius)
        isFirst -> RoundedCornerShape(topStart = radius, topEnd = radius)
        isLast -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RectangleShape
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.card, shape),
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = AppleMetrics.RowHorizontalPadding)
                    .height(AppleMetrics.Hairline)
                    .background(palette.separator),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        onClick != null && onLongClick != null ->
                            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        onClick != null -> Modifier.clickable(onClick = onClick)
                        else -> Modifier
                    },
                )
                .padding(
                    horizontal = AppleMetrics.RowHorizontalPadding,
                    vertical = AppleMetrics.RowVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Section title above a grouped card, optionally with a trailing count. */
@Composable
fun AppleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val palette = applePalette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.page)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                start = AppleMetrics.SectionHorizontalPadding,
                end = AppleMetrics.SectionHorizontalPadding,
                top = AppleMetrics.SectionTopPadding,
                bottom = 7.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(text = trailing, fontSize = 13.sp, color = palette.secondaryLabel)
        }
    }
}

/** Big iOS-style page title with an optional trailing action. */
@Composable
fun AppleLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val palette = applePalette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = AppleMetrics.SectionHorizontalPadding, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            color = palette.label,
        )
        action?.invoke()
    }
}

/** Search field without a Material container, so it reads like an iOS search bar. */
@Composable
fun AppleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    onSearch: () -> Unit = {},
) {
    val palette = applePalette()
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = palette.label),
        cursorBrush = SolidColor(palette.tint),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch()
                focusManager.clearFocus()
            },
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.field)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = palette.secondaryLabel,
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, fontSize = 16.sp, color = palette.secondaryLabel)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = palette.secondaryLabel,
                        )
                    }
                }
            }
        },
    )
}

/** Plain inline text field used inside a grouped row (no container, no label). */
@Composable
fun AppleInlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val palette = applePalette()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = palette.label),
        cursorBrush = SolidColor(palette.tint),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(text = placeholder, fontSize = 16.sp, color = palette.secondaryLabel)
                }
                innerTextField()
            }
        },
    )
}

/** Filled, single primary action of a screen. */
@Composable
fun ApplePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Single line of status text, e.g. "3 个线路 · 刚刚同步". */
@Composable
fun AppleFootnote(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 12.sp,
        color = applePalette().secondaryLabel,
        textAlign = textAlign,
    )
}
