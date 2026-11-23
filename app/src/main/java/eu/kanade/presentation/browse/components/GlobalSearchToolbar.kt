package eu.kanade.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AppleMetrics
import eu.kanade.presentation.theme.AppleSearchField
import eu.kanade.presentation.theme.applePalette
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.KeyboardArrowLeft
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Compact search bar: back chevron plus a single inline field.
 * Filter chips are only shown on the migration search screen, where they are actually reachable.
 */
@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    @Suppress("UNUSED_PARAMETER") scrollBehavior: TopAppBarScrollBehavior,
) {
    val palette = applePalette()

    Column(modifier = Modifier.background(palette.page)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 2.dp, end = AppleMetrics.PageHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = navigateUp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = palette.tint,
                )
            }
            AppleSearchField(
                value = searchQuery.orEmpty(),
                onValueChange = { onChangeSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = "搜索",
                onSearch = { onSearch(searchQuery.orEmpty()) },
            )
        }

        if (progress in 1..<total) {
            LinearProgressIndicator(
                progress = { progress / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(start = AppleMetrics.PageHorizontalPadding),
                color = palette.tint,
                trackColor = palette.separator,
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppleMetrics.SectionHorizontalPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!hideSourceFilter) {
                FilterChip(
                    selected = sourceFilter == SourceFilter.PinnedOnly,
                    onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
                    label = { Text(text = stringResource(MR.strings.pinned_sources)) },
                    modifier = Modifier.height(FilterChipDefaults.Height),
                )
                FilterChip(
                    selected = sourceFilter == SourceFilter.All,
                    onClick = { onChangeSearchFilter(SourceFilter.All) },
                    label = { Text(text = stringResource(MR.strings.all)) },
                    modifier = Modifier.height(FilterChipDefaults.Height),
                )
            }
            FilterChip(
                selected = onlyShowHasResults,
                onClick = { onToggleResults() },
                leadingIcon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                label = { Text(text = stringResource(MR.strings.has_results)) },
                modifier = Modifier.height(FilterChipDefaults.Height),
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = palette.separator)
    }
}
