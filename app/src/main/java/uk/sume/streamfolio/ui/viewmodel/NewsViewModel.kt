package uk.sume.streamfolio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.data.network.NewsRepository
import uk.sume.streamfolio.data.network.DefaultFeedsConfig
import uk.sume.streamfolio.ui.components.TtsHelper
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository(application)
    val prefs = PreferencesHelper(application)
    val ttsHelper = TtsHelper(application)

    init {
        ttsHelper.onArticleCompleted = {
            advanceTtsPlaylist()
        }
    }

    // Typography States
    val readerFontFamily = MutableStateFlow(prefs.readerFontFamily)
    val readerFontSize = MutableStateFlow(prefs.readerFontSize)
    val readerLineSpacing = MutableStateFlow(prefs.readerLineSpacing)

    // TTS Word Range & Seek
    val currentWordRange: StateFlow<Pair<Int, Int>?> = ttsHelper.currentWordRange

    private val _showLyricsVisualizer = MutableStateFlow(false)
    val showLyricsVisualizer: StateFlow<Boolean> = _showLyricsVisualizer

    private val _currentArticleDetail = MutableStateFlow<Article?>(null)
    val currentArticleDetail: StateFlow<Article?> = _currentArticleDetail.asStateFlow()

    fun setShowLyricsVisualizer(show: Boolean) {
        _showLyricsVisualizer.value = show
    }

    fun seekTtsToParagraph(index: Int) {
        ttsHelper.seekToParagraph(index)
    }

    fun updateReaderFontFamily(font: String) {
        prefs.readerFontFamily = font
        readerFontFamily.value = font
    }

    fun updateReaderFontSize(size: Float) {
        prefs.readerFontSize = size
        readerFontSize.value = size
    }

    fun updateReaderLineSpacing(spacing: Float) {
        prefs.readerLineSpacing = spacing
        readerLineSpacing.value = spacing
    }

    // TTS Playlist state
    private val _ttsPlaylist = MutableStateFlow<List<Article>>(emptyList())
    val ttsPlaylist: StateFlow<List<Article>> = _ttsPlaylist

    private val _currentTtsArticleIndex = MutableStateFlow<Int>(-1)
    val currentTtsArticleIndex: StateFlow<Int> = _currentTtsArticleIndex

    fun addToTtsPlaylist(article: Article) {
        val current = _ttsPlaylist.value.toMutableList()
        if (!current.any { it.link == article.link }) {
            current.add(article)
            _ttsPlaylist.value = current
            if (_currentTtsArticleIndex.value == -1) {
                _currentTtsArticleIndex.value = 0
            }
        }
    }

    fun removeFromTtsPlaylist(article: Article) {
        val current = _ttsPlaylist.value.toMutableList()
        val index = current.indexOfFirst { it.link == article.link }
        if (index != -1) {
            current.removeAt(index)
            _ttsPlaylist.value = current
            
            val activeIdx = _currentTtsArticleIndex.value
            if (activeIdx == index) {
                if (current.isEmpty()) {
                    stopSpeakingPlaylist()
                } else {
                    val nextIdx = if (index < current.size) index else current.size - 1
                    playTtsPlaylist(nextIdx)
                }
            } else if (index < activeIdx) {
                _currentTtsArticleIndex.value = activeIdx - 1
            }
        }
    }

    fun clearTtsPlaylist() {
        stopSpeakingPlaylist()
        _ttsPlaylist.value = emptyList()
    }

    fun moveTtsPlaylistItem(fromIndex: Int, toIndex: Int) {
        val current = _ttsPlaylist.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _ttsPlaylist.value = current
            
            // Adjust current playing index if it was moved
            val activeIdx = _currentTtsArticleIndex.value
            if (activeIdx == fromIndex) {
                _currentTtsArticleIndex.value = toIndex
            } else if (activeIdx in (fromIndex + 1)..toIndex) {
                _currentTtsArticleIndex.value = activeIdx - 1
            } else if (activeIdx in toIndex..<fromIndex) {
                _currentTtsArticleIndex.value = activeIdx + 1
            }
        }
    }

    fun playTtsPlaylist(startIndex: Int) {
        val list = _ttsPlaylist.value
        if (startIndex < 0 || startIndex >= list.size) return
        
        _currentTtsArticleIndex.value = startIndex
        val article = list[startIndex]
        
        viewModelScope.launch {
            _isLoadingBody.value = true
            _articleBody.value = ""
            ttsHelper.stop()
            
            val cached = repository.getArticleByLink(article.link)
            val textToSpeak = if (cached != null && cached.fullText != null) {
                _articleBody.value = cached.fullText
                cached.fullText
            } else {
                val body = repository.fetchArticleBody(article.link)
                _articleBody.value = body
                if (body.isNotBlank() && !body.startsWith("Failed to load") && !body.startsWith("Unable to parse")) {
                    repository.updateFullText(article.link, body)
                }
                body
            }
            _isLoadingBody.value = false
            ttsHelper.play(textToSpeak)
        }
    }

    fun stopSpeakingPlaylist() {
        ttsHelper.stop()
        _currentTtsArticleIndex.value = -1
        _articleBody.value = ""
    }

    fun playOrPausePlaylist() {
        if (ttsHelper.isPlaying.value) {
            ttsHelper.pause()
        } else {
            val idx = _currentTtsArticleIndex.value
            if (idx != -1) {
                if (_articleBody.value.isEmpty()) {
                    playTtsPlaylist(idx)
                } else {
                    ttsHelper.resume()
                }
            } else if (_ttsPlaylist.value.isNotEmpty()) {
                playTtsPlaylist(0)
            }
        }
    }

    fun advanceTtsPlaylist() {
        val nextIndex = _currentTtsArticleIndex.value + 1
        if (nextIndex < _ttsPlaylist.value.size) {
            playTtsPlaylist(nextIndex)
        } else {
            stopSpeakingPlaylist()
        }
    }

    // Current category selector
    private val _selectedCategory = MutableStateFlow("Top Stories")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Current publisher filter
    private val _selectedPublisher = MutableStateFlow<String?>(null)
    val selectedPublisher: StateFlow<String?> = _selectedPublisher

    private val _prefsChangedSignal = MutableStateFlow(0)

    // Articles flow
    val articles: StateFlow<List<Article>> = combine(_selectedCategory, _prefsChangedSignal) { category, _ ->
        category
    }
        .flatMapLatest { category ->
            repository.getArticlesByCategory(category).map { list ->
                val enabledUrls = DefaultFeedsConfig.getFeedsFor(
                    region = prefs.region,
                    category = category,
                    disabledFeedUrls = prefs.disabledFeedUrls,
                    enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
                ).toSet()
                
                list.filter { article ->
                    article.customFeedId != null || enabledUrls.contains(article.sourceUrl)
                }
            }
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

    private val _isLoadingSearch = MutableStateFlow(false)
    val isLoadingSearch: StateFlow<Boolean> = _isLoadingSearch

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

    // Navigation parameter helper for curation filtering
    var filterCategoryOnSettings: String? = null

    // Pending article URL clicked from widget
    private val _pendingArticleUrl = MutableStateFlow<String?>(null)
    val pendingArticleUrl: StateFlow<String?> = _pendingArticleUrl.asStateFlow()

    fun setPendingArticleUrl(url: String?) {
        _pendingArticleUrl.value = url
    }

    init {
        // Fetch top stories initially if onboarding is done
        if (prefs.isCompletedOnboarding) {
            refreshCurrentFeed()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _selectedPublisher.value = null
        refreshCurrentFeed()
    }

    fun selectPublisher(publisher: String?) {
        _selectedPublisher.value = publisher
    }

    fun refreshCurrentFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val currentCat = _selectedCategory.value
            
            // Check if it's a custom feed
            val matchingFeeds = customFeeds.value.filter { it.category == currentCat }
            if (matchingFeeds.isNotEmpty()) {
                repository.fetchCustomFeeds(matchingFeeds, currentCat)
            } else {
                repository.fetchDefaultFeeds(
                    category = currentCat,
                    language = prefs.language,
                    region = prefs.region,
                    disabledFeedUrls = prefs.disabledFeedUrls,
                    enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
                )
            }
            _isRefreshing.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                _isLoadingSearch.value = true
                val enabledCats = if (prefs.isDefaultFeedsEnabled) prefs.selectedCategories else emptySet()
                repository.searchNewsOnline(
                    query = query,
                    language = prefs.language,
                    region = prefs.region,
                    activeCategories = enabledCats,
                    customFeeds = customFeeds.value,
                    disabledFeedUrls = prefs.disabledFeedUrls,
                    enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
                )
                _isLoadingSearch.value = false
            }
        } else {
            _isLoadingSearch.value = false
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
            val art = repository.getArticleByLink(url)
            _currentArticleDetail.value = art

            // Check if this article is already active in the reader/speech playlist.
            // If yes, do not clear the body or stop the playback.
            val list = _ttsPlaylist.value
            val activeIdx = _currentTtsArticleIndex.value
            if (activeIdx != -1 && activeIdx < list.size && list[activeIdx].link == url) {
                // Keep playing, do not stop!
                return@launch
            }

            _isLoadingBody.value = true
            _articleBody.value = ""
            resetAiSummary()
            ttsHelper.stop() // Stop playing previous article
            
            var cachedArticle = art
            if (cachedArticle != null && cachedArticle.fullText != null) {
                _articleBody.value = cachedArticle.fullText
                _isLoadingBody.value = false
            } else if (cachedArticle != null) {
                val body = repository.fetchArticleBody(url)
                _articleBody.value = body
                _isLoadingBody.value = false
                if (body.isNotBlank() && !body.startsWith("Failed to load") && !body.startsWith("Unable to parse")) {
                    repository.updateFullText(url, body)
                }
            } else {
                // Scrape page metadata dynamically to build transient article
                try {
                    val transientArticle = withContext(Dispatchers.IO) {
                        val realUrl = repository.resolveGoogleNewsUrl(url)
                        val doc = org.jsoup.Jsoup.connect(realUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .timeout(8000)
                            .get()

                        val title = doc.select("h1").firstOrNull()?.text()?.trim() 
                            ?: doc.title() 
                            ?: "Article Details"
                        val source = doc.select("meta[property=og:site_name]").attr("content").trim().ifBlank {
                            try { java.net.URL(realUrl).host.replace("www.", "") } catch (e: Exception) { "Web Article" }
                        }
                        val thumb = doc.select("meta[property=og:image]").attr("content").trim()

                        Article(
                            link = url,
                            title = title,
                            description = title,
                            pubDate = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US).format(java.util.Date()),
                            sourceName = source,
                            sourceUrl = realUrl,
                            category = "TOP STORIES",
                            thumbnailUrl = thumb.ifBlank { null }
                        )
                    }
                    _currentArticleDetail.value = transientArticle
                    
                    val body = repository.fetchArticleBody(url)
                    _articleBody.value = body
                    _isLoadingBody.value = false
                    if (body.isNotBlank() && !body.startsWith("Failed to load") && !body.startsWith("Unable to parse")) {
                        repository.insertArticle(transientArticle.copy(fullText = body))
                    }
                } catch (e: Exception) {
                    val fallbackArt = Article(
                        link = url,
                        title = "Shared Article",
                        description = "Shared Web Article",
                        pubDate = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US).format(java.util.Date()),
                        sourceName = "Web",
                        sourceUrl = url,
                        category = "NEWS",
                        thumbnailUrl = null
                    )
                    _currentArticleDetail.value = fallbackArt
                    _articleBody.value = "Unable to load article content automatically. Please switch to Web View to read this story."
                    _isLoadingBody.value = false
                }
            }
        }
    }

    fun speakArticle(article: Article) {
        val list = _ttsPlaylist.value
        val activeIdx = _currentTtsArticleIndex.value
        if (activeIdx != -1 && activeIdx < list.size && list[activeIdx].link == article.link) {
            playOrPausePlaylist()
        } else {
            _ttsPlaylist.value = listOf(article)
            playTtsPlaylist(0)
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
                val hasRemaining = customFeeds.value.any { it.category == feed.category && it.id != feed.id }
                if (!hasRemaining) {
                    _selectedCategory.value = "Top Stories"
                }
            }
        }
    }

    fun updatePreferences(language: String, region: String) {
        prefs.language = language
        prefs.region = region
        refreshCurrentFeed()
    }

    // On-Device AI Summarization
    private val aiSummaryHelper by lazy { uk.sume.streamfolio.data.network.AiSummaryHelper(getApplication()) }
    private val _aiSummaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val aiSummaryState: StateFlow<AiSummaryState> = _aiSummaryState

    fun generateAiSummary(text: String) {
        if (text.isBlank()) {
            _aiSummaryState.value = AiSummaryState.Error("Article body is empty.")
            return
        }
        _aiSummaryState.value = AiSummaryState.Loading
        
        viewModelScope.launch {
            try {
                val status = aiSummaryHelper.checkFeatureStatus()
                if (status == com.google.mlkit.genai.common.FeatureStatus.DOWNLOADABLE) {
                    _aiSummaryState.value = AiSummaryState.DownloadingModel
                    aiSummaryHelper.downloadFeature()
                    val result = aiSummaryHelper.summarizeText(text)
                    _aiSummaryState.value = AiSummaryState.Success(result.summary)
                    return@launch
                } else if (status == com.google.mlkit.genai.common.FeatureStatus.DOWNLOADING) {
                    _aiSummaryState.value = AiSummaryState.DownloadingModel
                    return@launch
                } else if (status == com.google.mlkit.genai.common.FeatureStatus.UNAVAILABLE) {
                    _aiSummaryState.value = AiSummaryState.Error("This feature requires Gemini Nano (AICore), which is not available on this device.")
                    return@launch
                }
                
                val result = aiSummaryHelper.summarizeText(text)
                _aiSummaryState.value = AiSummaryState.Success(result.summary)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to generate summary."
                if (msg.contains("AICore", ignoreCase = true)) {
                    _aiSummaryState.value = AiSummaryState.Error("Gemini Nano (AICore) is not available or requires update on this device.")
                } else {
                    _aiSummaryState.value = AiSummaryState.Error(msg)
                }
            }
        }
    }

    fun resetAiSummary() {
        _aiSummaryState.value = AiSummaryState.Idle
    }

    fun deleteArticlesForFeed(sourceUrl: String) {
        viewModelScope.launch {
            repository.deleteArticlesForFeed(sourceUrl)
        }
    }

    fun fetchSingleFeed(url: String, category: String) {
        viewModelScope.launch {
            repository.fetchSingleFeed(url, category)
        }
    }

    fun triggerPrefsChanged() {
        _prefsChangedSignal.value = _prefsChangedSignal.value + 1
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}

sealed interface AiSummaryState {
    object Idle : AiSummaryState
    object Loading : AiSummaryState
    object DownloadingModel : AiSummaryState
    data class Success(val summary: String) : AiSummaryState
    data class Error(val message: String) : AiSummaryState
}
