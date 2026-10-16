package eu.kanade.tachiyomi.extension

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import mihon.data.extension.service.ExtensionStoreService
import mihon.domain.extension.model.ContentWarning
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** One subscription prepares every supported Chinese source inside the application. */
internal object AggregateCatalog {
    private val mutex = Mutex()

    data class Report(val sources: Int, val failed: Int)

    suspend fun synchronize(
        context: Context,
        indexUrl: String = context.appGraph.sourcePreferences.aggregateIndexUrl.get(),
        progress: (String) -> Unit = {},
    ): Report = mutex.withLock {
        withContext(Dispatchers.IO) {
            val url = AggregateCatalogPolicy.validateUrl(indexUrl)
            val graph = context.appGraph
            val preferences = graph.sourcePreferences
            val service = ExtensionStoreService(graph.networkHelper, graph.json, graph.protoBuf)
            progress("正在读取统一图源订阅…")
            val store = service.fetch(url).getOrThrow()
            val previousKey = if (preferences.aggregateIndexUrl.get() == url) preferences.aggregateSigningKey.get() else ""
            AggregateCatalogPolicy.checkSigningKey(previousKey, store.signingKey)
            val available = service.getExtensions(store).getOrThrow()
                .filter { extension ->
                    extension.sources.any { it.lang.startsWith("zh") } &&
                        extension.contentWarning != ContentWarning.NSFW
                }
            require(available.isNotEmpty()) { "订阅中没有可用的中文图源" }
            require(available.size <= 200) { "订阅图源数量过多" }
            graph.extensionStoreRepository.upsert(store)
            val managed = preferences.aggregateManagedPackages.get().toMutableSet()
            var failed = 0
            available.forEachIndexed { index, extension ->
                progress("正在准备图源 ${index + 1}/${available.size}：${extension.name}")
                try {
                    val installed = ExtensionLoader.getExtensionPackageInfoFromPkgName(context, extension.pkgName)
                    if (installed == null || PackageInfoCompat.getLongVersionCode(installed) < extension.versionCode) {
                        val downloadUrl = AggregateCatalogPolicy.validateUrl(extension.apkUrl)
                        val temporary = File.createTempFile("aggregate-", ".apk", context.cacheDir)
                        try {
                            graph.networkHelper.client.newCall(GET(downloadUrl)).awaitSuccess().use { response ->
                                require(response.body.contentLength() <= AggregateCatalogPolicy.MAX_PACKAGE_BYTES) { "图源文件过大" }
                                response.body.byteStream().use { input ->
                                    temporary.outputStream().use { output ->
                                        val buffer = ByteArray(8192)
                                        var total = 0L
                                        while (true) {
                                            val count = input.read(buffer)
                                            if (count == -1) break
                                            total += count
                                            require(total <= AggregateCatalogPolicy.MAX_PACKAGE_BYTES) { "图源文件过大" }
                                            output.write(buffer, 0, count)
                                        }
                                    }
                                }
                            }
                            val info = requireNotNull(ExtensionLoader.getArchivePackageInfo(context, temporary)) { "图源文件无法识别" }
                            require(info.packageName == extension.pkgName) { "图源身份不一致" }
                            require(PackageInfoCompat.getLongVersionCode(info) == extension.versionCode) { "图源版本不一致" }
                            require(ExtensionLoader.getSignatures(info).orEmpty().any { it.equals(store.signingKey, true) }) { "图源签名校验失败" }
                            ExtensionLoader.installPrivateExtensionFile(context, temporary, info)
                        } finally {
                            temporary.delete()
                        }
                    } else {
                        require(ExtensionLoader.getSignatures(installed).orEmpty().any { it.equals(store.signingKey, true) }) { "已有图源签名不匹配" }
                    }
                    managed.add(extension.pkgName)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    failed++
                }
            }
            preferences.enabledLanguages.set(preferences.enabledLanguages.get() + setOf("zh", "zh-Hans", "zh-Hant"))
            graph.extensionManager.reloadCatalogSources()
            val loaded = graph.extensionManager.getLoadedExtensions().filter { it.pkgName in managed }
            val sources = loaded.flatMap { it.sources }.count { it.lang.startsWith("zh") }
            require(sources > 0) { "图源尚未准备成功，请检查网络后点击同步重试" }
            preferences.aggregateIndexUrl.set(url)
            preferences.aggregateSigningKey.set(store.signingKey)
            preferences.aggregateManagedPackages.set(managed)
            preferences.aggregateLastSync.set(System.currentTimeMillis())
            progress("已就绪 $sources 个中文图源" + if (failed > 0) "，$failed 个暂未同步成功" else "")
            Report(sources, failed)
        }
    }
}
