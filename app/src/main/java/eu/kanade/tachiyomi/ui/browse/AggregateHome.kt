package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.theme.AppleFootnote
import eu.kanade.presentation.theme.AppleGroupRow
import eu.kanade.presentation.theme.AppleInlineTextField
import eu.kanade.presentation.theme.AppleLargeTitle
import eu.kanade.presentation.theme.AppleMetrics
import eu.kanade.presentation.theme.AppleSearchField
import eu.kanade.presentation.theme.applePalette
import eu.kanade.tachiyomi.extension.AggregateCatalog
import eu.kanade.tachiyomi.extension.AggregateCatalogPolicy
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Sync
import java.text.DateFormat
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException

@Composable
internal fun AggregateHome() {
    val context = LocalContext.current
    val graph = context.appGraph
    val preferences = graph.sourcePreferences
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow
    val palette = applePalette()

    var address by rememberSaveable { mutableStateOf(preferences.aggregateIndexUrl.get()) }
    var query by rememberSaveable { mutableStateOf("") }
    var configuring by rememberSaveable { mutableStateOf(address.isBlank()) }
    var running by remember { mutableStateOf(false) }
    var count by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf(false) }
    var lastSync by remember { mutableStateOf(preferences.aggregateLastSync.get()) }

    suspend fun synchronize() {
        running = true
        failure = false
        try {
            val result = AggregateCatalog.synchronize(context, address) { message = it }
            count = result.sources
            lastSync = preferences.aggregateLastSync.get()
            configuring = false
            message = ""
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failure = true
            message = error.message ?: "同步失败，请检查网络后重试"
        } finally {
            running = false
        }
    }

    fun search() {
        if (query.isNotBlank() && count > 0 && !running) {
            navigator.push(GlobalSearchScreen(query.trim(), AggregateCatalogPolicy.SEARCH_FILTER))
        }
    }

    LaunchedEffect(Unit) {
        (context as? MainActivity)?.ready = true
        val loaded = graph.extensionManager.getLoadedExtensions()
        count = loaded.filter { it.pkgName in preferences.aggregateManagedPackages.get() }
            .flatMap { it.sources }.count { it.lang.startsWith("zh") }
        // Upgrade existing installations that already have exactly one configured repository.
        if (address.isBlank()) {
            graph.extensionStoreRepository.getAll().singleOrNull()?.let { address = it.indexUrl }
        }
        if (address.isNotBlank() && (count == 0 || System.currentTimeMillis() - lastSync > 24 * 60 * 60 * 1000L)) {
            synchronize()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        AppleLargeTitle(
            title = "搜索",
            action = {
                IconButton(onClick = { configuring = !configuring }, enabled = !running) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Settings,
                        contentDescription = "图源订阅设置",
                        tint = palette.secondaryLabel,
                    )
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppleMetrics.SectionHorizontalPadding,
                    end = AppleMetrics.SectionHorizontalPadding,
                    top = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppleSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = "漫画名、作者",
                onSearch = { search() },
            )
            if (query.isNotBlank()) {
                Text(
                    text = "搜索",
                    modifier = Modifier
                        .clickable(enabled = count > 0 && !running) { search() }
                        .padding(vertical = 6.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (count > 0 && !running) palette.tint else palette.tertiaryLabel,
                )
            }
        }

        if (running) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppleMetrics.SectionHorizontalPadding,
                        end = AppleMetrics.SectionHorizontalPadding,
                        top = 16.dp,
                    )
                    .height(2.dp),
                color = palette.tint,
                trackColor = palette.separator,
            )
        }

        Spacer(Modifier.height(18.dp))
        AppleFootnote(
            text = statusLine(count = count, running = running, lastSync = lastSync),
            modifier = Modifier.padding(horizontal = AppleMetrics.SectionHorizontalPadding),
            textAlign = TextAlign.Start,
        )

        if (failure && message.isNotBlank()) {
            Text(
                text = message,
                modifier = Modifier.padding(
                    start = AppleMetrics.SectionHorizontalPadding,
                    end = AppleMetrics.SectionHorizontalPadding,
                    top = 10.dp,
                ),
                fontSize = 13.sp,
                color = palette.error,
            )
        }

        if (configuring) {
            Spacer(Modifier.height(AppleMetrics.SectionTopPadding))
            Text(
                text = "图源订阅",
                modifier = Modifier.padding(
                    start = AppleMetrics.SectionHorizontalPadding,
                    bottom = 7.dp,
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.secondaryLabel,
            )
            Column(modifier = Modifier.padding(horizontal = AppleMetrics.PageHorizontalPadding)) {
                val canSave = address.isNotBlank() && !running
                val save: () -> Unit = { scope.launch { synchronize() } }
                AppleGroupRow(isFirst = true, isLast = false) {
                    AppleInlineTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.weight(1f),
                        placeholder = "粘贴图源地址",
                    )
                }
                AppleGroupRow(
                    isFirst = false,
                    isLast = true,
                    onClick = if (canSave) save else null,
                ) {
                    Text(
                        text = "保存并准备图源",
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        color = if (canSave) palette.tint else palette.tertiaryLabel,
                    )
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (canSave) palette.tint else palette.tertiaryLabel,
                    )
                }
                Spacer(Modifier.height(10.dp))
                AppleFootnote(
                    text = "只需一次，之后每天自动同步",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

private fun statusLine(count: Int, running: Boolean, lastSync: Long): String {
    if (running) return "正在准备图源…"
    if (count == 0) return "尚未准备图源"
    val synced = relativeTime(lastSync) ?: return "$count 个线路"
    return "$count 个线路 · $synced"
}

private fun relativeTime(timestamp: Long): String? {
    if (timestamp <= 0L) return null
    val delta = System.currentTimeMillis() - timestamp
    return when {
        delta < 60_000L -> "刚刚同步"
        delta < 3_600_000L -> "${delta / 60_000L} 分钟前同步"
        delta < 86_400_000L -> "${delta / 3_600_000L} 小时前同步"
        delta < 7 * 86_400_000L -> "${delta / 86_400_000L} 天前同步"
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
    }
}
