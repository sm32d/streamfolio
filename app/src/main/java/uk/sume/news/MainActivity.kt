package uk.sume.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.sume.news.ui.navigation.AppNavigation
import uk.sume.news.ui.theme.NewsTheme
import uk.sume.news.ui.viewmodel.NewsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsTheme {
                val viewModel: NewsViewModel = viewModel()
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}