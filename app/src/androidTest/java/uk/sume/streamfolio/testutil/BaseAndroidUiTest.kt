package uk.sume.streamfolio.testutil

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import uk.sume.streamfolio.MainActivity

/**
 * Shared setup for on-device Compose UI tests (P2). See AndroidTestSupport's kdoc for why
 * MainActivity is launched manually (createEmptyComposeRule() + ActivityScenario.launch(), not
 * createAndroidComposeRule<MainActivity>()'s auto-launch) rather than via the rule itself.
 *
 * The InstrumentationRegistry.waitForIdleSync() calls guard against a previous test method's
 * MainActivity/NewsViewModel not being fully torn down (background coroutines cancelled,
 * ViewModelStore cleared) before the next test's resetAppState() + fresh launch - observed
 * concretely as flaky "seeded content never appears" failures when running a full test class
 * that did not reproduce running any single test method in isolation.
 */
abstract class BaseAndroidUiTest {

    @get:Rule
    val composeRule: ComposeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun baseSetUp() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        AndroidTestSupport.resetAppState()
    }

    @After
    fun baseTearDown() {
        // Auto-closes whatever launchApp() started, so individual test methods/helpers don't
        // need their own `.use { }` block - forgetting one leaks the Activity into the next test
        // method (same instrumentation process), which was a real source of flakiness here.
        scenario?.close()
        scenario = null
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // waitForIdleSync() alone was not enough to prevent occasional cross-test flakiness
        // (observed as e.g. a still-tearing-down previous screen's composables briefly
        // coexisting with the next test's freshly launched one, producing spurious duplicate
        // node matches) - a short real-time settle window closes that gap.
        Thread.sleep(300)
    }

    protected fun launchApp(): ActivityScenario<MainActivity> {
        val launched = ActivityScenario.launch(MainActivity::class.java)
        scenario = launched
        return launched
    }
}
