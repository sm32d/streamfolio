package uk.sume.streamfolio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.work.*
import uk.sume.streamfolio.ui.navigation.AppNavigation
import uk.sume.streamfolio.ui.theme.NewsTheme
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel
import uk.sume.streamfolio.util.ShareLinks
import uk.sume.streamfolio.util.UrlSecurityValidator
import uk.sume.streamfolio.worker.NewsSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()

        viewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        intent?.let { handleArticleIntent(it) }

        setupBackgroundSync()

        setContent {
            val useDynamicColors by viewModel.useDynamicColors.collectAsState()
            NewsTheme(dynamicColor = useDynamicColors) {
                AppNavigation(viewModel = viewModel)
            }
        }
    }

    // Without this, some OEM display stacks (observed on a MediaTek device whose
    // FrameRateCategoryRate maps "high" activity to only 90Hz) auto-classify third-party
    // apps into a refresh-rate category below the display's actual peak, capping this app
    // below the 120Hz other apps reach. Explicitly requesting the highest-refresh-rate mode
    // sets the app-request vote directly instead of leaving it to that default guess.
    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
        val bestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
        if (bestMode != null) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = bestMode.modeId
            }
        }
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "NewsBackgroundSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleArticleIntent(intent)
    }

    // Article to open comes from a widget extra or from a shared https://streamfolio.sume.uk/open link.
    private fun handleArticleIntent(intent: Intent) {
        val url = intent.getStringExtra("article_url")?.let { UrlSecurityValidator.normalizeToHttps(it) }
            ?: ShareLinks.parseArticleUrl(intent.data)
        url?.let { viewModel.setPendingArticleUrl(it) }
    }
}
