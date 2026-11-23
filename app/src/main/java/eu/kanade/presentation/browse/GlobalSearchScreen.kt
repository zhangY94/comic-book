package eu.kanade.presentation.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.GlobalSearchEmptyRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorRow
import eu.kanade.presentation.browse.components.GlobalSearchLoadingRow
import eu.kanade.presentation.browse.components.GlobalSearchResultRow
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.theme.AppleMetrics
import eu.kanade.presentation.theme.AppleSectionHeader
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.ChapterPreviewState
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.chapterPreviewKey
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun GlobalSearchScreen(
    state: SearchViewModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    aggregateMode: Boolean = false,
    chapterPreviews: Map<String, ChapterPreviewState> = emptyMap(),
    onRequestChapterPreview: (Source, Manga) -> Unit = { _, _ -> },
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = aggregateMode,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            chapterPreviews = chapterPreviews,
            onRequestChapterPreview = onRequestChapterPreview,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GlobalSearchContent(
    items: Map<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
    chapterPreviews: Map<String, ChapterPreviewState> = emptyMap(),
    onRequestChapterPreview: (Source, Manga) -> Unit = { _, _ -> },
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        items.forEach { (source, result) ->
            sourceSection(
                source = source,
                result = result,
                fromSourceId = fromSourceId,
                getManga = getManga,
                chapterPreviews = chapterPreviews,
                onRequestChapterPreview = onRequestChapterPreview,
                onClickSource = onClickSource,
                onClickItem = onClickItem,
                onLongClickItem = onLongClickItem,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.sourceSection(
    source: Source,
    result: SearchItemResult,
    fromSourceId: Long?,
    getManga: @Composable (Manga) -> State<Manga>,
    chapterPreviews: Map<String, ChapterPreviewState>,
    onRequestChapterPreview: (Source, Manga) -> Unit,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    val resultCount = (result as? SearchItemResult.Success)?.result?.size

    stickyHeader(key = "source:${source.id}") {
        AppleSectionHeader(
            title = if (fromSourceId == source.id) "▶ ${source.name}" else source.name,
            trailing = when (resultCount) {
                null -> null
                0 -> "无结果"
                else -> "$resultCount"
            },
            onClick = { onClickSource(source) },
        )
    }

    val cardModifier = Modifier.padding(horizontal = AppleMetrics.PageHorizontalPadding)

    when (result) {
        SearchItemResult.Loading -> {
            item(key = "loading:${source.id}") {
                GlobalSearchLoadingRow(modifier = cardModifier)
            }
        }
        is SearchItemResult.Error -> {
            item(key = "error:${source.id}") {
                GlobalSearchErrorRow(message = result.throwable.message, modifier = cardModifier)
            }
        }
        is SearchItemResult.Success -> {
            if (result.isEmpty) {
                item(key = "empty:${source.id}") {
                    GlobalSearchEmptyRow(modifier = cardModifier)
                }
            } else {
                itemsIndexed(
                    items = result.result,
                    key = { _, manga -> "${source.id}:${manga.url}" },
                    contentType = { _, _ -> "result" },
                ) { index, manga ->
                    val liveManga by getManga(manga)
                    GlobalSearchResultRow(
                        manga = liveManga,
                        chapterPreview = chapterPreviews[chapterPreviewKey(source, liveManga)],
                        onRequestChapterPreview = { onRequestChapterPreview(source, liveManga) },
                        onClick = { onClickItem(liveManga) },
                        onLongClick = { onLongClickItem(liveManga) },
                        isFirst = index == 0,
                        isLast = index == result.result.lastIndex,
                        modifier = cardModifier,
                    )
                }
            }
        }
    }
}
