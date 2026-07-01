package uk.sume.streamfolio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import uk.sume.streamfolio.ui.navigation.AppNavigation
import uk.sume.streamfolio.ui.theme.NewsTheme
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        intent?.getStringExtra("article_url")?.let { url ->
            viewModel.pendingArticleUrl = url
        }

        setContent {
            NewsTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("article_url")?.let { url ->
            viewModel.pendingArticleUrl = url
        }
    }
}
