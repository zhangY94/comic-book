package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.extension.AggregateCatalog
import eu.kanade.tachiyomi.extension.AggregateCatalogPolicy
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("漫画聚合搜索", style = MaterialTheme.typography.headlineMedium)
        Text("输入书名，同时搜索多个图源，再选择最合适的来源阅读。", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("漫画名称") },
            placeholder = { Text("输入你想看的漫画") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
        )
        Button(onClick = { search() }, enabled = query.isNotBlank() && count > 0 && !running, modifier = Modifier.fillMaxWidth()) {
            Text("搜索全部图源")
        }
        Text(if (count > 0) "已就绪 $count 个中文图源 · 搜索结果按来源展示" else "首次使用：添加一次图源地址，即可自动准备全部图源")
        if (running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (message.isNotBlank()) {
            Text(message, color = if (failure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (lastSync > 0) Text("上次同步：${DateFormat.getDateTimeInstance().format(Date(lastSync))}", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { configuring = !configuring }, enabled = !running) {
            Text(if (configuring) "收起订阅设置" else "图源订阅设置")
        }
        if (configuring) {
            Text("统一图源地址", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("图源订阅地址") },
                placeholder = { Text("粘贴你的图源地址") },
                enabled = !running,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Text("只添加你信任的订阅。保存后自动准备图源，无需逐个安装；以后每天自动同步。", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { scope.launch { synchronize() } }, enabled = address.isNotBlank() && !running) {
            Text(if (count == 0) "保存并准备全部图源" else "立即同步图源")
        }
        Text("图片由手机直接加载。某个来源失效或较慢时，返回搜索结果选择其他来源即可。", style = MaterialTheme.typography.bodySmall)
    }
}
