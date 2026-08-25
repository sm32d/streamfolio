package uk.sume.streamfolio.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

// Generates app/src/main/generated/baselineProfiles/baseline-prof.txt.
// Run with: ./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest
// (Gradle provisions the managed device automatically — no connected phone/emulator needed.)
//
// Only cold start is profiled here. Extending this to also cover opening an article would need
// a stable UiAutomator selector for the first list card (e.g. a Modifier.testTag on
// ArticleListItem in HomeScreen.kt) — there's currently nothing else to target it from outside
// Compose, and this couldn't be verified without a device in the environment this was written in.
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = "uk.sume.streamfolio"
    ) {
        pressHome()
        startActivityAndWait()
    }
}
