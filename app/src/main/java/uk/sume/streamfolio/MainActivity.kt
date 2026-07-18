package uk.sume.streamfolio

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
import uk.sume.streamfolio.util.UrlSecurityValidator
import uk.sume.streamfolio.worker.NewsSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        intent?.getStringExtra("article_url")?.let { url ->
            val safeUrl = UrlSecurityValidator.normalizeToHttps(url)
            viewModel.setPendingArticleUrl(safeUrl)
        }

        setupBackgroundSync()

        setContent {
            val useDynamicColors by viewModel.useDynamicColors.collectAsState()
            NewsTheme(dynamicColor = useDynamicColors) {
                AppNavigation(viewModel = viewModel)
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
        intent.getStringExtra("article_url")?.let { url ->
            val safeUrl = UrlSecurityValidator.normalizeToHttps(url)
            viewModel.setPendingArticleUrl(safeUrl)
        }
    }
}
