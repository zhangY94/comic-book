package eu.kanade.tachiyomi

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import mihon.app.di.appGraph
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class AggregateSubscriptionTest {
    @Test
    fun oneSubscriptionLoadsSourcesAndOpensAggregateSearch() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val catalog = InstrumentationRegistry.getArguments().getString("catalog")
        require(!catalog.isNullOrBlank()) { "Provide the test subscription at runtime" }
        val device = UiDevice.getInstance(instrumentation)
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        assertTrue("Aggregate home is visible", device.wait(Until.hasObject(By.text("搜索")), 30_000))

        // The home screen shows the search field first and the subscription field below it.
        var fields = device.findObjects(By.clazz("android.widget.EditText"))
        assertTrue("A subscription address field is available", fields.isNotEmpty())
        fields.last().click()
        fields.last().text = catalog
        device.pressBack()

        fun clickText(label: String) {
            for (attempt in 0..5) {
                val target = device.findObject(By.text(label))
                if (target != null) {
                    target.click()
                    return
                }
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 4 / 5,
                    device.displayWidth / 2,
                    device.displayHeight / 3,
                    20,
                )
            }
            error("Expected action is not visible: $label")
        }

        clickText("保存并准备图源")
        assertTrue(
            "Subscription prepares Chinese sources",
            device.wait(Until.hasObject(By.text(Pattern.compile("[0-9]+ 个线路.*"))), 300_000),
        )

        val graph = context.appGraph
        val packages = graph.sourcePreferences.aggregateManagedPackages.get()
        val sources = graph.extensionManager.getLoadedExtensions()
            .filter { it.pkgName in packages }
            .flatMap { it.sources }
            .filter { it.lang.startsWith("zh") }
        assertTrue("Multiple independent sources loaded", sources.size > 1)
        assertTrue(
            "No source packages required Android installation",
            packages.none {
                runCatching { context.packageManager.getPackageInfo(it, 0) }.isSuccess
            },
        )

        repeat(4) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight / 3,
                device.displayWidth / 2,
                device.displayHeight * 4 / 5,
                15,
            )
        }
        val output = File(context.getExternalFilesDir(null), "aggregate-smoke").apply { mkdirs() }
        device.takeScreenshot(File(output, "home-ready.png"))

        // Typing the query and confirming with the IME search action starts the aggregate search.
        fields = device.findObjects(By.clazz("android.widget.EditText"))
        assertTrue("Search field is available", fields.isNotEmpty())
        fields.first().click()
        fields.first().text = "火影"
        device.pressEnter()

        val rowPattern = Pattern.compile("更新至 .*|读取中…|暂无章节")
        if (!device.wait(Until.hasObject(By.text(rowPattern)), 15_000)) {
            // Fallback when the IME search action is not delivered: tap the trailing action.
            device.findObjects(By.text("搜索")).forEach { it.click() }
        }
        assertTrue(
            "Aggregate search renders result rows",
            device.wait(Until.hasObject(By.text(rowPattern)), 120_000),
        )
        device.takeScreenshot(File(output, "aggregate-results.png"))
        println("AGGREGATE_SMOKE source_count=${sources.size} private_packages=${packages.size}")
    }
}
