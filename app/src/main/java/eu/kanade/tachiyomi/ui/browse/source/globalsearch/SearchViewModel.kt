package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.AggregateCatalogPolicy
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mihon.domain.manga.model.toDomainManga
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import tachiyomi.core.common.preference.toggle
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal

abstract class SearchViewModel(
    initialState: State = State(),
    sourcePreferences: SourcePreferences,
    private val sourceManager: SourceManager,
    private val extensionManager: ExtensionManager,
    private val networkToLocalManga: NetworkToLocalManga,
    private val getManga: GetManga,
    private val preferences: SourcePreferences,
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow<State>(initialState)

    // Subclasses can't touch the backing field (Kotlin forbids a visibility modifier on one),
    // so state writes from them go through here.
    protected fun updateState(function: (State) -> State) {
        state.update(function)
    }

    private val coroutineDispatcher = Dispatchers.IO.limitedParallelism(5)
    private val searchPermits = Semaphore(5)
    private var searchJob: Job? = null

    private val enabledLanguages = sourcePreferences.enabledLanguages.get()
    private val disabledSources = sourcePreferences.disabledSources.get()
    protected val pinnedSources = sourcePreferences.pinnedSources.get()
    private val aggregatePackages = sourcePreferences.aggregateManagedPackages.get()

    private var lastQuery: String? = null
    private var lastSourceFilter: SourceFilter? = null

    protected var extensionFilter: String? = null

    open val sortComparator = { map: Map<Source, SearchItemResult> ->
        compareBy<Source>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { "${it.id}" !in pinnedSources },
            { "${it.name.lowercase()} (${it.lang})" },
        )
    }

    init {
        viewModelScope.launch {
            preferences.globalSearchFilterState.changes().collectLatest { onlyShowHasResults ->
                state.update { it.copy(onlyShowHasResults = onlyShowHasResults) }
            }
        }
    }

    @Composable
    fun getManga(initialManga: Manga): androidx.compose.runtime.State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .filterNotNull()
                .collectLatest { manga ->
                    value = manga
                }
        }
    }

    private val _chapterPreviews = MutableStateFlow<Map<String, ChapterPreviewState>>(emptyMap())

    /**
     * Latest chapter of each search result, keyed by [chapterPreviewKey].
     *
     * Search results only carry cover/title/source, so the latest chapter has to be fetched from the
     * source. Requests are made lazily (only for rows the user actually scrolls to), throttled and
     * cached for the lifetime of this screen.
     */
    val chapterPreviews: StateFlow<Map<String, ChapterPreviewState>> = _chapterPreviews

    private val chapterPreviewRequests = ConcurrentHashMap.newKeySet<String>()
    private val chapterPreviewPermits = Semaphore(3)
    private val chapterPreviewDispatcher = Dispatchers.IO.limitedParallelism(3)

    fun requestChapterPreview(source: Source, manga: Manga) {
        val key = chapterPreviewKey(source, manga)
        if (_chapterPreviews.value.containsKey(key) || !chapterPreviewRequests.add(key)) {
            return
        }

        viewModelScope.launch {
            val preview = try {
                withContext(chapterPreviewDispatcher) {
                    chapterPreviewPermits.withPermit {
                        withTimeout(CHAPTER_PREVIEW_TIMEOUT_MS) {
                            source.getMangaUpdate(
                                manga = manga.toSManga(),
                                chapters = emptyList(),
                                fetchDetails = false,
                                fetchChapters = true,
                            ).chapters.toChapterPreview()
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ChapterPreviewState.Unavailable
            } finally {
                chapterPreviewRequests.remove(key)
            }

            _chapterPreviews.update { it + (key to preview) }
        }
    }

    open suspend fun getEnabledSources(): List<Source> {
        return sourceManager.getAll()
            .filter { (it.lang in enabledLanguages || it.isLocal()) && "${it.id}" !in disabledSources }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    private suspend fun getSelectedSources(): List<Source> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        return extensionManager.getLoadedExtensions()
            .filter { if (filter == AggregateCatalogPolicy.SEARCH_FILTER) it.pkgName in aggregatePackages else it.pkgName == filter }
            .flatMap { it.sources }
            .filter { it in enabledSources && (filter != AggregateCatalogPolicy.SEARCH_FILTER || it.lang.startsWith("zh")) }
    }

    fun updateSearchQuery(query: String?) {
        state.update { it.copy(searchQuery = query) }
    }

    fun setSourceFilter(filter: SourceFilter) {
        state.update { it.copy(sourceFilter = filter) }
        search()
    }

    fun toggleFilterResults() {
        preferences.globalSearchFilterState.toggle()
    }

    fun search() {
        val query = state.value.searchQuery
        val sourceFilter = state.value.sourceFilter

        if (query.isNullOrBlank()) return

        val sameQuery = this.lastQuery == query
        if (sameQuery && this.lastSourceFilter == sourceFilter) return

        this.lastQuery = query
        this.lastSourceFilter = sourceFilter

        searchJob?.cancel()

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val sources = getSelectedSources()

            // Reuse previous results if possible
            if (sameQuery) {
                val existingResults = state.value.items
                updateItems(
                    sources
                        .associateWith { existingResults[it] ?: SearchItemResult.Loading },
                )
            } else {
                updateItems(
                    sources
                        .associateWith { SearchItemResult.Loading },
                )
            }

            sources.map { source ->
                async {
                    if (state.value.items[source] !is SearchItemResult.Loading) {
                        return@async
                    }

                    try {
                        val page = withContext(coroutineDispatcher) {
                            searchPermits.withPermit {
                                kotlinx.coroutines.withTimeout(25_000) {
                                    source.getSearchManga(1, query, source.getFilterList())
                                }
                            }
                        }

                        val titles = page.mangas
                            .map { it.toDomainManga(source.id) }
                            .distinctBy { it.url }
                            .let { networkToLocalManga(it) }

                        if (isActive) {
                            updateItem(source, SearchItemResult.Success(titles))
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            updateItem(source, SearchItemResult.Error(e))
                        }
                    }
                }
            }
                .awaitAll()
        }
    }

    private fun updateItems(items: Map<Source, SearchItemResult>) {
        state.update {
            it.copy(
                items = items
                    .toSortedMap(sortComparator(items)),
            )
        }
    }

    private fun updateItem(source: Source, result: SearchItemResult) {
        state.update { currentState ->
            val newItems = currentState.items + (source to result)
            currentState.copy(
                items = newItems.toSortedMap(sortComparator(newItems)),
            )
        }
    }

    fun setMigrateDialog(currentId: Long, target: Manga) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = getManga.await(currentId) ?: return@launch
            state.update { it.copy(dialog = Dialog.Migrate(target, current)) }
        }
    }

    fun clearDialog() {
        state.update { it.copy(dialog = null) }
    }

    @Immutable
    data class State(
        val from: Manga? = null,
        val searchQuery: String? = null,
        val sourceFilter: SourceFilter = SourceFilter.PinnedOnly,
        val onlyShowHasResults: Boolean = false,
        val items: Map<Source, SearchItemResult> = mapOf(),
        val dialog: Dialog? = null,
    ) {
        val progress: Int = items.count { it.value !is SearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }

    sealed interface Dialog {
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }
}

enum class SourceFilter {
    All,
    PinnedOnly,
}

private const val CHAPTER_PREVIEW_TIMEOUT_MS = 15_000L

fun chapterPreviewKey(source: Source, manga: Manga): String = "${source.id}:${manga.url}"

sealed interface ChapterPreviewState {
    data object Unavailable : ChapterPreviewState

    data class Available(val label: String) : ChapterPreviewState
}

/**
 * Turns the raw chapter list of a source into a short "latest chapter" label.
 *
 * Sources list chapters in different orders and some do not expose recognizable numbers, so prefer
 * the highest recognized chapter number, then fall back to the newest upload date.
 */
private fun List<SChapter>.toChapterPreview(): ChapterPreviewState {
    if (isEmpty()) return ChapterPreviewState.Unavailable

    val latest = filter { it.chapter_number > 0f }.maxByOrNull { it.chapter_number }
        ?: maxByOrNull { it.date_upload }
        ?: return ChapterPreviewState.Unavailable

    val label = latest.name.trim().ifBlank {
        latest.chapter_number
            .takeIf { it > 0f }
            ?.let { number -> "第${number.toString().removeSuffix(".0")}话" }
            .orEmpty()
    }

    return if (label.isBlank()) {
        ChapterPreviewState.Unavailable
    } else {
        ChapterPreviewState.Available(label)
    }
}

sealed interface SearchItemResult {
    data object Loading : SearchItemResult

    data class Error(
        val throwable: Throwable,
    ) : SearchItemResult

    data class Success(
        val result: List<Manga>,
    ) : SearchItemResult {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}
