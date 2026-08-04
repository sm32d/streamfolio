package uk.sume.streamfolio.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import uk.sume.streamfolio.ui.screens.settings.SettingsMainScreen
import uk.sume.streamfolio.ui.viewmodel.NewsViewModel

@Composable
fun SettingsScreen(navController: NavController, viewModel: NewsViewModel) {
    SettingsMainScreen(navController = navController, viewModel = viewModel)
}
