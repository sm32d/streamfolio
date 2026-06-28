package uk.sume.news.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.sume.news.data.local.PreferencesHelper
import uk.sume.news.data.model.Article
import uk.sume.news.data.model.CustomFeed
import uk.sume.news.data.network.NewsRepository
import uk.sume.news.ui.components.TtsHelper
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository(application)
    val prefs = PreferencesHelper(application)
    val ttsHelper = TtsHelper(application)

    // Current category selector
    private val _selectedCategory = MutableStateFlow("Top Stories")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Articles flow
    val articles: StateFlow<List<Article>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getArticlesByCategory(category)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarked articles flow
    val bookmarkedArticles: StateFlow<List<Article>> = repository.getBookmarkedArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Custom Feeds flow
    val customFeeds: StateFlow<List<CustomFeed>> = repository.getCustomFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local & Remote search flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<Article>> = _searchQuery
        .debounce(500)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchArticlesLocal(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reader Mode State
    private val _articleBody = MutableStateFlow("")
    val articleBody: StateFlow<String> = _articleBody

    private val _isLoadingBody = MutableStateFlow(false)
    val isLoadingBody: StateFlow<Boolean> = _isLoadingBody

    // Page Refresh States
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        // Fetch top stories initially if onboarding is done
        if (prefs.isCompletedOnboarding) {
            refreshCurrentFeed()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        refreshCurrentFeed()
    }

    fun refreshCurrentFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val currentCat = _selectedCategory.value
            
            // Check if it's a custom feed
            val customFeed = customFeeds.value.firstOrNull { it.category == currentCat }
            if (customFeed != null) {
                repository.fetchCustomFeed(customFeed)
            } else {
                repository.fetchGoogleNews(
                    category = currentCat,
                    language = prefs.language,
                    region = prefs.region
                )
            }
            _isRefreshing.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.searchNewsOnline(query, prefs.language, prefs.region)
        }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch {
            repository.toggleBookmark(article.link, !article.isBookmarked)
        }
    }

    fun deleteBookmark(article: Article) {
        viewModelScope.launch {
            repository.toggleBookmark(article.link, false)
        }
    }

    fun loadArticleBody(url: String) {
        viewModelScope.launch {
            _isLoadingBody.value = true
            _articleBody.value = ""
            ttsHelper.stop() // Stop playing previous article
            val body = repository.fetchArticleBody(url)
            _articleBody.value = body
            _isLoadingBody.value = false
        }
    }

    // TTS Actions
    fun speakArticle() {
        if (ttsHelper.isPlaying.value) {
            ttsHelper.pause()
        } else {
            if (ttsHelper.currentParagraphIndex.value > 0) {
                ttsHelper.resume()
            } else {
                ttsHelper.play(_articleBody.value)
            }
        }
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    // Custom Feed actions
    fun addCustomRssFeed(title: String, url: String, category: String) {
        viewModelScope.launch {
            val feed = CustomFeed(title = title, url = url, category = category)
            repository.addCustomFeed(feed)
            refreshCurrentFeed()
        }
    }

    fun removeCustomRssFeed(feed: CustomFeed) {
        viewModelScope.launch {
            repository.deleteCustomFeed(feed)
            if (_selectedCategory.value == feed.category) {
                _selectedCategory.value = "Top Stories"
            }
        }
    }

    fun updatePreferences(language: String, region: String) {
        prefs.language = language
        prefs.region = region
        refreshCurrentFeed()
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
