package eu.kanade.tachiyomi.extension

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.workManager
import mihon.app.di.appGraph
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** Refreshes only the extension stores that the user has added on this device. */
class ExtensionCatalogUpdateWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        if (applicationContext.appGraph.sourcePreferences.aggregateIndexUrl.get().isNotBlank()) {
            return try {
                AggregateCatalog.synchronize(applicationContext)
                Result.success()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Result.retry()
            }
        }
        applicationContext.appGraph.extensionManager.checkForUpdates()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExtensionCatalogUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.CONNECTED,
                        requiresBatteryNotLow = true,
                    ),
                )
                .build()
            context.workManager.enqueueUniquePeriodicWork(
                "ExtensionCatalogUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
