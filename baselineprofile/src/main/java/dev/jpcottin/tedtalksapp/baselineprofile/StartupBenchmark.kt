package dev.jpcottin.tedtalksapp.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-startup time with no profile vs. with the generated Baseline
 * Profile, so the improvement is quantifiable.
 *
 * Run with:
 *   `./gradlew :app:benchmarkReleaseAndroidTest`
 * Compare `StartupBenchmark_startupNoCompilation` against
 * `StartupBenchmark_startupBaselineProfile` in the benchmark output.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule val rule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() =
        startup(CompilationMode.Partial(baselineProfileMode = androidx.benchmark.macro.BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode,
    ) {
        pressHome()
        startActivityAndWait()
        // Wait for first content so "time to initial display" is meaningful.
        device.wait(Until.hasObject(By.scrollable(true)), 10_000)
    }
}
