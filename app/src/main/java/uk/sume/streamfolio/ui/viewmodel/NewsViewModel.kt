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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder
import uk.sume.streamfolio.util.OpmlFeed
import uk.sume.streamfolio.util.UrlSecurityValidator
import uk.sume.streamfolio.playback.TtsPlaybackManager

data class CategoryArticlesState(
    val category: String = "",
    val groups: List<uk.sume.streamfolio.data.model.ArticleGroup> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository(application)
    val prefs = PreferencesHelper(application)

    private val _groupSimilarStories = MutableStateFlow(prefs.groupSimilarStories)
    val groupSimilarStories: StateFlow<Boolean> = _groupSimilarStories.asStateFlow()
    private val playbackManager = TtsPlaybackManager.getInstance(application)
    val ttsHelper = playbackManager.ttsHelper
    private val requestedThumbnailRetries = mutableSetOf<String>()

    // AI Translation States & Logic
    private val aiTranslateHelper by lazy { uk.sume.streamfolio.data.network.AiHelperFactory.createTranslateHelper(getApplication()) }

    private val _translatedTitle = MutableStateFlow("")
    val translatedTitle: StateFlow<String> = _translatedTitle.asStateFlow()

    private val _translatedBody = MutableStateFlow("")
    val translatedBody: StateFlow<String> = _translatedBody.asStateFlow()

    private val _isTranslationLoading = MutableStateFlow(false)
    val isTranslationLoading: StateFlow<Boolean> = _isTranslationLoading.asStateFlow()

    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError.asStateFlow()

    // On-Device AI Summarization
    private val aiSummaryHelper by lazy { uk.sume.streamfolio.data.network.AiHelperFactory.createSummaryHelper(getApplication()) }
    
    private val _isGeminiSupported = MutableStateFlow<Boolean?>(null)
    val isGeminiSupported: StateFlow<Boolean?> = _isGeminiSupported.asStateFlow()

    private val _aiSummaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val aiSummaryState: StateFlow<AiSummaryState> = _aiSummaryState

    init {
        viewModelScope.launch(Dispatchers.Main) {
            if (!prefs.hasClearedOldTags) {
                repository.clearAllTags()
                prefs.hasClearedOldTags = true
            }
            checkGeminiSupport()
            if (prefs.isAiEnabled) {
                triggerBackgroundAiPreDownload()
            }
            if (prefs.isCompletedOnboarding) {
                refreshCurrentFeed()
                syncAllCategoriesInBackground()
            }
        }
    }

    fun syncAllCategoriesInBackground() {
        viewModelScope.launch(Dispatchers.IO) {
            val categoriesToSync = prefs.selectedCategories.toList()
            val defaultCategories = setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
            for (cat in categoriesToSync) {
                try {
                    val matchingFeeds = customFeeds.value.filter { it.category == cat }
                    if (matchingFeeds.isNotEmpty()) {
                        repository.fetchCustomFeeds(matchingFeeds, cat)
                    }
                    if (defaultCategories.contains(cat) && prefs.isDefaultFeedsEnabled) {
                        repository.fetchDefaultFeeds(
                            category = cat,
                            language = prefs.language,
                            region = prefs.region,
                            disabledFeedUrls = prefs.disabledFeedUrls,
                            enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
                        )
                    }
                    // Debounce to prevent device congestion
                    kotlinx.coroutines.delay(1000)
                } catch (e: Exception) {
                    android.util.Log.e("NewsViewModel", "Background sync failed for category $cat", e)
                }
            }
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

    private val _similarArticlesForDetail = MutableStateFlow<List<Article>>(emptyList())
    val similarArticlesForDetail: StateFlow<List<Article>> = _similarArticlesForDetail.asStateFlow()

    val scrollStates = mutableMapOf<String, androidx.compose.foundation.lazy.LazyListState>()
    val visibleListCounts = mutableMapOf<String, Int>()

    private val _articleLanguage = MutableStateFlow<String?>(null)
    val articleLanguage: StateFlow<String?> = _articleLanguage.asStateFlow()

    fun setShowLyricsVisualizer(show: Boolean) {
        _showLyricsVisualizer.value = show
    }

    fun seekTtsToParagraph(index: Int) {
        playbackManager.seekToParagraph(index)
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

    // TTS playback state shared with the foreground playback service
    val ttsPlaylist: StateFlow<List<Article>> = playbackManager.ttsPlaylist
    val currentTtsArticleIndex: StateFlow<Int> = playbackManager.currentTtsArticleIndex
    val ttsSpeechRate: StateFlow<Float> = playbackManager.ttsSpeechRate
    val sleepTimerRemainingMillis: StateFlow<Long?> = playbackManager.sleepTimerRemainingMillis
    val ttsArticleBody: StateFlow<String> = playbackManager.articleBody
    val isTtsLoadingBody: StateFlow<Boolean> = playbackManager.isLoadingBody

    fun addToTtsPlaylist(article: Article) = playbackManager.addToTtsPlaylist(article)

    fun playNextInTtsPlaylist(article: Article) = playbackManager.playNextInTtsPlaylist(article)

    fun removeFromTtsPlaylist(article: Article) = playbackManager.removeFromTtsPlaylist(article)

    fun clearTtsPlaylist() = playbackManager.clearTtsPlaylist()

    fun moveTtsPlaylistItem(fromIndex: Int, toIndex: Int) =
        playbackManager.moveTtsPlaylistItem(fromIndex, toIndex)

    fun playTtsPlaylist(startIndex: Int) = playbackManager.playTtsPlaylist(startIndex)

    fun stopSpeakingPlaylist() = playbackManager.stopSpeakingPlaylist()

    fun playOrPausePlaylist() = playbackManager.togglePlayback()

    fun advanceTtsPlaylist() = playbackManager.advanceTtsPlaylist()

    fun playPreviousTtsArticle() = playbackManager.playPreviousArticle()

    fun setSleepTimer(durationMinutes: Int?) = playbackManager.setSleepTimer(durationMinutes)

    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()

    // Current category selector
    private val _selectedCategory = MutableStateFlow("Top Stories")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Current publisher filter
    private val _selectedPublisher = MutableStateFlow<String?>(null)
    val selectedPublisher: StateFlow<String?> = _selectedPublisher

    private val _prefsChangedSignal = MutableStateFlow(0)
    private val categoryLastRefreshedMap = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Category articles state holder to prevent cross-category state leaks
    val categoryArticlesState: StateFlow<CategoryArticlesState> = combine(_selectedCategory, _prefsChangedSignal) { category, _ ->
        category
    }
        .flatMapLatest { category ->
            val flow = if (category.startsWith("#")) {
                repository.getAllArticlesUnlimited().map { list ->
                    list.filter { article ->
                        article.tags?.lowercase()?.contains(category.lowercase()) == true
                    }
                }
            } else {
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
            combine(flow, groupSimilarStories) { list, groupEnabled ->
                val groups = if (groupEnabled) {
                    groupSimilarArticles(list)
                } else {
                    list.map { uk.sume.streamfolio.data.model.ArticleGroup(primary = it, secondary = emptyList()) }
                }
                CategoryArticlesState(category = category, groups = groups)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryArticlesState())

    val articles: StateFlow<List<Article>> = categoryArticlesState
        .map { state -> state.groups.map { it.primary } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedArticles: StateFlow<List<uk.sume.streamfolio.data.model.ArticleGroup>> = categoryArticlesState
        .map { state -> state.groups }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
                repository.searchArticlesLocal(query).map { list ->
                    val clean = query.trim()
                    if (clean.length <= 3) {
                        val pattern = Regex("\\b" + Regex.escape(clean) + "\\b", RegexOption.IGNORE_CASE)
                        list.filter { article ->
                            pattern.containsMatchIn(article.title) || 
                            pattern.containsMatchIn(article.description)
                        }
                    } else {
                        list
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingTopics: StateFlow<List<String>> = combine(
        repository.getAllArticles(),
        customFeeds,
        _prefsChangedSignal
    ) { allArticles, feeds, _ ->
        val activeCategories = if (prefs.isDefaultFeedsEnabled) prefs.selectedCategories else emptySet()
        val activeUrls = mutableSetOf<String>()
        
        for (cat in activeCategories) {
            val defaultFeeds = DefaultFeedsConfig.getFeedsFor(
                region = prefs.region,
                category = cat,
                disabledFeedUrls = prefs.disabledFeedUrls,
                enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
            )
            activeUrls.addAll(defaultFeeds)
        }
        
        val enabledCustomFeeds = feeds.filter { 
            !prefs.disabledFeedUrls.contains(it.url) 
        }.map { it.url }
        activeUrls.addAll(enabledCustomFeeds)

        val filteredArticles = allArticles.filter { activeUrls.contains(it.sourceUrl) }

        if (filteredArticles.isEmpty()) {
            listOf("AI", "Climate", "Markets", "Tech", "Science", "Sports", "Health", "Space")
        } else {
            val stopwords = setOf(
                "the", "and", "for", "with", "this", "that", "from", "your", "about", "would", 
                "their", "will", "more", "news", "says", "said", "after", "over", "first", "years", 
                "year", "could", "three", "were", "what", "when", "who", "which", "have", "been", 
                "also", "into", "some", "other", "them", "then", "their", "they", "than", "then",
                "should", "these", "those", "about", "against", "after", "before", "between",
                "under", "through", "during", "without", "around", "about", "against", "among"
            )
            
            val counts = mutableMapOf<String, Int>()
            
            for (article in filteredArticles) {
                if (article.category.isNotBlank() && article.category != "SEARCH" && article.category != "All") {
                    val cat = article.category.trim()
                    counts[cat] = (counts[cat] ?: 0) + 5
                }
                
                if (!article.tags.isNullOrBlank()) {
                    article.tags.split(",").forEach { tag ->
                        val cleanTag = tag.trim()
                        if (cleanTag.isNotBlank() && cleanTag.lowercase() != "news") {
                            counts[cleanTag] = (counts[cleanTag] ?: 0) + 10
                        }
                    }
                }
                
                val words = article.title
                    .split(Regex("[^a-zA-Z0-9#]"))
                    .map { it.trim() }
                    .filter { word ->
                        word.length > 2 && 
                        !stopwords.contains(word.lowercase()) &&
                        word.lowercase() != "news" &&
                        word.any { it.isLetter() }
                    }
                for (word in words) {
                    val displayWord = if (word == word.uppercase()) {
                        word
                    } else {
                        word.replaceFirstChar { it.uppercase() }
                    }
                    counts[displayWord] = (counts[displayWord] ?: 0) + 1
                }
            }
            
            counts.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .filter { it.lowercase() != "search" && it.lowercase() != "all" }
                .take(8)
                .ifEmpty {
                    listOf("AI", "Climate", "Markets", "Tech", "Science", "Sports", "Health", "Space")
                }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("AI", "Climate", "Markets", "Tech", "Science", "Sports", "Health", "Space"))

    // Reader Mode State
    private val _articleBody = MutableStateFlow("")
    val articleBody: StateFlow<String> = _articleBody.asStateFlow()

    private val _isLoadingBody = MutableStateFlow(false)
    val isLoadingBody: StateFlow<Boolean> = _isLoadingBody.asStateFlow()

    // Page Refresh States
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Navigation parameter helper for curation filtering
    var filterCategoryOnSettings: String? = null

    val tabResetEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    
    fun triggerTabReset(route: String) {
        tabResetEvent.tryEmit(route)
    }

    private val _pendingArticleUrl = MutableStateFlow<String?>(null)
    val pendingArticleUrl: StateFlow<String?> = _pendingArticleUrl.asStateFlow()

    fun setPendingArticleUrl(url: String?) {
        val safeUrl = url?.let { UrlSecurityValidator.normalizeToHttps(it) }
        _pendingArticleUrl.value = safeUrl
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _selectedPublisher.value = null

        val lastRefreshed = categoryLastRefreshedMap[category] ?: 0L
        val now = System.currentTimeMillis()
        val isStale = (now - lastRefreshed) > 15 * 60 * 1000L
        val isLoadedForCategory = categoryArticlesState.value.category == category && categoryArticlesState.value.groups.isNotEmpty()

        if (isStale || !isLoadedForCategory) {
            refreshCurrentFeed(silent = true)
        }
    }

    fun selectPublisher(publisher: String?) {
        _selectedPublisher.value = publisher
    }

    fun refreshCurrentFeed(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _isRefreshing.value = true
            }
            val currentCat = _selectedCategory.value
            if (currentCat.startsWith("#")) {
                if (!silent) _isRefreshing.value = false
                return@launch
            }
            
            // Check if it's a custom feed
            val matchingFeeds = customFeeds.value.filter { it.category == currentCat }
            if (matchingFeeds.isNotEmpty()) {
                repository.fetchCustomFeeds(matchingFeeds, currentCat)
            }
            
            val defaultCategories = setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
            if (defaultCategories.contains(currentCat) && prefs.isDefaultFeedsEnabled) {
                repository.fetchDefaultFeeds(
                    category = currentCat,
                    language = prefs.language,
                    region = prefs.region,
                    disabledFeedUrls = prefs.disabledFeedUrls,
                    enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
                )
            }
            categoryLastRefreshedMap[currentCat] = System.currentTimeMillis()
            if (!silent) {
                _isRefreshing.value = false
            }
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

    private val _recentSearches = MutableStateFlow(prefs.recentSearches)
    val recentSearches: StateFlow<List<String>> = _recentSearches

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val currentList = prefs.recentSearches.toMutableList()
        currentList.remove(trimmed)
        currentList.add(0, trimmed)
        val updatedList = currentList.take(8) // Keep top 8 recent searches
        prefs.recentSearches = updatedList
        _recentSearches.value = updatedList
    }

    fun clearRecentSearches() {
        prefs.recentSearches = emptyList()
        _recentSearches.value = emptyList()
    }

    fun removeRecentSearch(query: String) {
        val currentList = prefs.recentSearches.toMutableList()
        currentList.remove(query)
        prefs.recentSearches = currentList
        _recentSearches.value = currentList
    }


    fun toggleBookmark(article: Article) {
        viewModelScope.launch {
            val newStatus = !article.isBookmarked
            repository.toggleBookmark(article.link, newStatus)
            if (_currentArticleDetail.value?.link == article.link) {
                _currentArticleDetail.value = _currentArticleDetail.value?.copy(isBookmarked = newStatus)
            }
        }
    }

    fun deleteBookmark(article: Article) {
        viewModelScope.launch {
            repository.toggleBookmark(article.link, false)
            if (_currentArticleDetail.value?.link == article.link) {
                _currentArticleDetail.value = _currentArticleDetail.value?.copy(isBookmarked = false)
            }
        }
    }

    fun selectArticleForDetail(article: Article) {
        _currentArticleDetail.value = article
    }

    fun loadArticleBody(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _similarArticlesForDetail.value = emptyList()
            
            var art = _currentArticleDetail.value
            if (art == null || art.link != url) {
                art = repository.getArticleByLink(url)
                _currentArticleDetail.value = art
            }

            if (art != null) {
                try {
                    val allArticles = repository.getAllArticlesUnlimitedSync()
                    val similar = allArticles.filter { other ->
                        other.link != art.link && calculateJaccardSimilarity(art.title, other.title) >= 0.35f
                    }
                    android.util.Log.d("NewsViewModel", "Detail similar articles for ${art.title}: ${similar.size}")
                    _similarArticlesForDetail.value = similar
                } catch (e: Exception) {
                    android.util.Log.e("NewsViewModel", "Error loading similar articles: ${e.message}")
                }
            }

            // Check if this article is already active in the reader/speech playlist.
            // If yes, do not clear the body or stop the playback.
            val list = ttsPlaylist.value
            val activeIdx = currentTtsArticleIndex.value
            if (activeIdx != -1 && activeIdx < list.size && list[activeIdx].link == url) {
                _articleBody.value = ttsArticleBody.value
                _isLoadingBody.value = isTtsLoadingBody.value
                return@launch
            }

            _isLoadingBody.value = true
            _articleBody.value = ""
            _articleLanguage.value = null
            withContext(Dispatchers.Main) {
                resetAiSummary()
            }
            if (art?.aiSummary != null) {
                if (art.aiSummary == "blocked_by_safety_policy") {
                    _aiSummaryState.value = AiSummaryState.Error("This article touches upon sensitive topics that trigger Gemini Nano's built-in local safety policies.")
                } else {
                    _aiSummaryState.value = AiSummaryState.Success(art.aiSummary)
                }
            }
            withContext(Dispatchers.Main) {
                ttsHelper.stop() // Stop playing previous article
            }
            
            var cachedArticle = art
            if (cachedArticle != null) {
                repository.updateReadStatus(url, true)
                _currentArticleDetail.value = cachedArticle.copy(isRead = true)
            }

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
                    val transientArticle = repository.scrapeTransientArticle(url)
                    _currentArticleDetail.value = transientArticle

                    val body = repository.fetchArticleBody(url)
                    _articleBody.value = body
                    _isLoadingBody.value = false
                    if (transientArticle != null && body.isNotBlank() && !body.startsWith("Failed to load") && !body.startsWith("Unable to parse")) {
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
            val currentBody = _articleBody.value
            val currentTitle = _currentArticleDetail.value?.title ?: ""
            if (currentBody.isNotBlank() && 
                !currentBody.startsWith("Failed to load") && !currentBody.startsWith("Unable to parse") && !currentBody.startsWith("Unable to load")) {
                val cachedArt = repository.getArticleByLink(url)
                val lang = cachedArt?.detectedLanguage ?: run {
                    val helper = aiTranslateHelper
                    val detected = if (helper != null) {
                        helper.identifyLanguage(currentTitle + " " + currentBody.take(100))
                    } else {
                        "en"
                    }
                    if (cachedArt != null) {
                        repository.updateDetectedLanguage(url, detected)
                    }
                    detected
                }
                _articleLanguage.value = lang
                val target = _translationTargetLanguage.value
                if (prefs.isAiEnabled && prefs.isTranslationEnabled && lang != target) {
                    if (cachedArt?.translatedLanguage == target && cachedArt.translatedTitle != null && cachedArt.translatedBody != null) {
                        _translatedTitle.value = cachedArt.translatedTitle
                        _translatedBody.value = cachedArt.translatedBody
                    } else {
                        translateArticleText(currentTitle, currentBody)
                    }
                }
            }
        }
    }

    fun speakArticle(article: Article) {
        playbackManager.speakArticle(article)
    }

    fun stopSpeaking() {
        playbackManager.stopSpeakingPlaylist()
    }

    // Custom Feed actions
    fun addCustomRssFeed(title: String, url: String, category: String) {
        viewModelScope.launch {
            val safeUrl = UrlSecurityValidator.sanitizeUrl(url, requireHttps = true)
            if (safeUrl == null) {
                android.util.Log.w("NewsViewModel", "Rejected unsafe custom feed URL")
                return@launch
            }
            val feed = CustomFeed(title = title, url = safeUrl, category = category)
            repository.addCustomFeed(feed)
            refreshCurrentFeed()
        }
    }

    fun updateCustomRssFeed(feed: CustomFeed) {
        viewModelScope.launch {
            val safeUrl = UrlSecurityValidator.sanitizeUrl(feed.url, requireHttps = true)
            if (safeUrl == null) {
                android.util.Log.w("NewsViewModel", "Rejected unsafe custom feed URL")
                return@launch
            }
            repository.addCustomFeed(feed.copy(url = safeUrl))
            refreshCurrentFeed()
        }
    }

    fun importCustomRssFeeds(feeds: List<OpmlFeed>, categoryMapping: Map<String, String>, selectedCategories: Set<String>) {
        viewModelScope.launch {
            val existingFeeds = repository.getAllCustomFeedsSync()
            val existingUrls = existingFeeds.map { it.url.trim().lowercase() }.toSet()

            for (opmlFeed in feeds) {
                if (selectedCategories.contains(opmlFeed.category)) {
                    val mappedCategory = categoryMapping[opmlFeed.category] ?: opmlFeed.category
                    val safeUrl = UrlSecurityValidator.sanitizeUrl(opmlFeed.xmlUrl, requireHttps = true)
                    if (safeUrl == null) {
                        android.util.Log.w("NewsViewModel", "Rejected unsafe OPML feed URL: ${UrlSecurityValidator.normalizeUrl(opmlFeed.xmlUrl)?.toHttpUrlOrNull()?.host ?: "[invalid]"}")
                        continue
                    }
                    if (existingUrls.contains(safeUrl.lowercase())) {
                        continue
                    }
                    val feed = CustomFeed(
                        title = opmlFeed.title.trim(),
                        url = safeUrl,
                        category = mappedCategory.trim()
                    )
                    repository.addCustomFeed(feed)
                }
            }
            refreshCurrentFeed()
        }
    }

    private fun reloadPreferencesToStateFlows() {
        readerFontFamily.value = prefs.readerFontFamily
        readerFontSize.value = prefs.readerFontSize
        readerLineSpacing.value = prefs.readerLineSpacing
        _isAiEnabled.value = prefs.isAiEnabled
        _isTranslationEnabled.value = prefs.isTranslationEnabled
        _isSummaryEnabled.value = prefs.isSummaryEnabled
        _isSmartTagsEnabled.value = prefs.isSmartTagsEnabled
        _translationTargetLanguage.value = prefs.translationTargetLanguage
        _hasSeenAiSpotlight.value = prefs.hasSeenAiSpotlight
        _hasSeenSwipeHint.value = prefs.hasSeenSwipeHint
        _swipeLeftAction.value = prefs.swipeLeftAction
        _swipeRightAction.value = prefs.swipeRightAction
        playbackManager.setTtsSpeechRate(prefs.ttsSpeechRate)
    }

    suspend fun generateBackupJson(): String {
        val feeds = repository.getAllCustomFeedsSync()
        val articles = repository.getBookmarkedAndReadArticles()
        return uk.sume.streamfolio.util.BackupHelper.generateBackupJson(getApplication(), feeds, articles)
    }

    fun restoreSettingsBackup(backupJson: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAllCustomFeeds()
                val parsed = uk.sume.streamfolio.util.BackupHelper.parseBackupJson(backupJson)
                repository.addCustomFeeds(parsed.customFeeds)
                if (parsed.articles.isNotEmpty()) {
                    repository.restoreArticles(parsed.articles)
                }
                parsed.applyPreferences(prefs)
                reloadPreferencesToStateFlows()
                refreshCurrentFeed()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
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

    // AI Toggle States
    private val _isAiEnabled = MutableStateFlow(prefs.isAiEnabled)
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()

    private val _isTranslationEnabled = MutableStateFlow(prefs.isTranslationEnabled)
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

    private val _isSummaryEnabled = MutableStateFlow(prefs.isSummaryEnabled)
    val isSummaryEnabled: StateFlow<Boolean> = _isSummaryEnabled.asStateFlow()

    private val _isSmartTagsEnabled = MutableStateFlow(prefs.isSmartTagsEnabled)
    val isSmartTagsEnabled: StateFlow<Boolean> = _isSmartTagsEnabled.asStateFlow()

    private val _translationTargetLanguage = MutableStateFlow(prefs.translationTargetLanguage)
    val translationTargetLanguage: StateFlow<String> = _translationTargetLanguage.asStateFlow()

    private val _hasSeenAiSpotlight = MutableStateFlow(prefs.hasSeenAiSpotlight)
    val hasSeenAiSpotlight: StateFlow<Boolean> = _hasSeenAiSpotlight.asStateFlow()

    private val _hasSeenSwipeHint = MutableStateFlow(prefs.hasSeenSwipeHint)
    val hasSeenSwipeHint: StateFlow<Boolean> = _hasSeenSwipeHint.asStateFlow()

    // Dynamic Tag Filter state
    private val _selectedDynamicTag = MutableStateFlow<String?>(null)
    val selectedDynamicTag: StateFlow<String?> = _selectedDynamicTag.asStateFlow()

    private val _swipeLeftAction = MutableStateFlow(prefs.swipeLeftAction)
    val swipeLeftAction: StateFlow<String> = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(prefs.swipeRightAction)
    val swipeRightAction: StateFlow<String> = _swipeRightAction.asStateFlow()

    private val _useDynamicColors = MutableStateFlow(prefs.useDynamicColors)
    val useDynamicColors: StateFlow<Boolean> = _useDynamicColors.asStateFlow()


    fun setTtsSpeechRate(rate: Float) {
        playbackManager.setTtsSpeechRate(rate)
    }

    fun setUseDynamicColors(enabled: Boolean) {
        prefs.useDynamicColors = enabled
        _useDynamicColors.value = enabled
    }

    fun setGroupSimilarStories(enabled: Boolean) {
        prefs.groupSimilarStories = enabled
        _groupSimilarStories.value = enabled
    }

    fun setSwipeLeftAction(action: String) {
        prefs.swipeLeftAction = action
        _swipeLeftAction.value = action
    }

    fun setSwipeRightAction(action: String) {
        prefs.swipeRightAction = action
        _swipeRightAction.value = action
    }

    fun toggleReadStatus(article: Article) {
        viewModelScope.launch {
            val newStatus = !article.isRead
            repository.updateReadStatus(article.link, newStatus)
            if (_currentArticleDetail.value?.link == article.link) {
                _currentArticleDetail.value = _currentArticleDetail.value?.copy(isRead = newStatus)
            }
        }
    }

    fun markAsRead(article: Article) {
        viewModelScope.launch {
            repository.updateReadStatus(article.link, true)
            if (_currentArticleDetail.value?.link == article.link) {
                _currentArticleDetail.value = _currentArticleDetail.value?.copy(isRead = true)
            }
        }
    }

    fun setDynamicTagFilter(tag: String?) {
        _selectedDynamicTag.value = tag
    }

    fun setAiEnabled(enabled: Boolean) {
        prefs.isAiEnabled = enabled
        _isAiEnabled.value = enabled
        if (enabled) {
            triggerBackgroundAiPreDownload()
        }
    }

    fun setTranslationEnabled(enabled: Boolean) {
        prefs.isTranslationEnabled = enabled
        _isTranslationEnabled.value = enabled
    }

    fun setSummaryEnabled(enabled: Boolean) {
        prefs.isSummaryEnabled = enabled
        _isSummaryEnabled.value = enabled
    }

    fun setSmartTagsEnabled(enabled: Boolean) {
        prefs.isSmartTagsEnabled = enabled
        _isSmartTagsEnabled.value = enabled
    }

    fun setTranslationTargetLanguage(lang: String) {
        prefs.translationTargetLanguage = lang
        _translationTargetLanguage.value = lang
    }

    fun setHasSeenAiSpotlight(seen: Boolean) {
        prefs.hasSeenAiSpotlight = seen
        _hasSeenAiSpotlight.value = seen
    }

    fun setHasSeenSwipeHint(seen: Boolean) {
        prefs.hasSeenSwipeHint = seen
        _hasSeenSwipeHint.value = seen
    }

    fun translateArticleText(title: String, body: String) {
        _isTranslationLoading.value = true
        _translationError.value = null
        viewModelScope.launch {
            try {
                val helper = aiTranslateHelper
                if (helper == null) {
                    _translationError.value = "Translation service is not supported on this device."
                    return@launch
                }
                val detectedSrc = helper.identifyLanguage(title + " " + body.take(100))
                val target = _translationTargetLanguage.value
                
                if (detectedSrc == target) {
                    return@launch
                }

                val tTitle = helper.translateText(title, detectedSrc, target)
                val tBody = helper.translateText(body, detectedSrc, target)

                _translatedTitle.value = tTitle
                _translatedBody.value = tBody
                
                _currentArticleDetail.value?.link?.let { link ->
                    repository.updateTranslation(link, tTitle, tBody, target, detectedSrc)
                }
            } catch (e: Exception) {
                _translationError.value = e.message ?: "Translation download or inference failed."
            } finally {
                _isTranslationLoading.value = false
            }
        }
    }

    fun clearTranslation() {
        _translatedTitle.value = ""
        _translatedBody.value = ""
        _translationError.value = null
    }

    fun triggerBackgroundAiPreDownload() {
        viewModelScope.launch {
            try {
                val helper = aiTranslateHelper
                if (helper != null) {
                    val target = _translationTargetLanguage.value
                    helper.translateText("Hello", "en", target)
                }
            } catch (e: Throwable) {
                android.util.Log.d("NewsViewModel", "Background AI translation pre-download failed/skipped: ${e.message}")
            }

            try {
                val helper = aiSummaryHelper
                if (helper != null) {
                    val status = helper.checkFeatureStatus()
                    if (status == 1) { // DOWNLOADABLE
                        helper.downloadFeature()
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.d("NewsViewModel", "Background AI summary pre-download failed/skipped: ${e.message}")
            }
        }
    }



    fun checkGeminiSupport() {
        viewModelScope.launch {
            try {
                val helper = aiSummaryHelper
                if (helper == null) {
                    _isGeminiSupported.value = false
                } else {
                    val status = helper.checkFeatureStatus()
                    _isGeminiSupported.value = status != 0 // 0 is UNAVAILABLE
                }
            } catch (e: Throwable) {
                _isGeminiSupported.value = false
            }
        }
    }


    fun generateAiSummary(text: String) {
        if (text.isBlank()) {
            _aiSummaryState.value = AiSummaryState.Error("Article body is empty.")
            return
        }
        _aiSummaryState.value = AiSummaryState.Loading
        
        viewModelScope.launch {
            try {
                val helper = aiSummaryHelper
                if (helper == null) {
                    _aiSummaryState.value = AiSummaryState.Error("Gemini Nano features are not supported on this device.")
                    return@launch
                }
                val status = helper.checkFeatureStatus()
                if (status == 1) { // DOWNLOADABLE
                    _aiSummaryState.value = AiSummaryState.DownloadingModel
                    helper.downloadFeature()
                    val summary = helper.summarizeText(text)
                    _aiSummaryState.value = AiSummaryState.Success(summary)
                    _currentArticleDetail.value?.link?.let { link ->
                        repository.updateAiSummary(link, summary)
                    }
                    return@launch
                } else if (status == 2) { // DOWNLOADING
                    _aiSummaryState.value = AiSummaryState.DownloadingModel
                    return@launch
                } else if (status == 0) { // UNAVAILABLE
                    _aiSummaryState.value = AiSummaryState.Error("This feature requires Gemini Nano (AICore), which is not available on this device.")
                    return@launch
                }
                
                val summary = helper.summarizeText(text)
                _aiSummaryState.value = AiSummaryState.Success(summary)
                _currentArticleDetail.value?.link?.let { link ->
                    repository.updateAiSummary(link, summary)
                }
            } catch (e: Throwable) {
                val msg = e.message ?: "Failed to generate summary."
                if (msg.contains("AICore", ignoreCase = true)) {
                    _aiSummaryState.value = AiSummaryState.Error("Gemini Nano (AICore) is not available or requires update on this device.")
                } else {
                    _aiSummaryState.value = AiSummaryState.Error(msg)
                    val isSafetyBlock = msg.contains("safety policy check failed", ignoreCase = true) ||
                                       (e is com.google.mlkit.genai.common.GenAiException && msg.contains("safety", ignoreCase = true))
                    if (isSafetyBlock) {
                        _currentArticleDetail.value?.link?.let { link ->
                            repository.updateAiSummary(link, "blocked_by_safety_policy")
                        }
                    }
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

    fun requestThumbnailOnDemand(article: Article) {
        val link = article.link
        val needsThumbnail = article.thumbnailUrl.isNullOrBlank() || article.thumbnailUrl == "failed"
        if (!needsThumbnail) return
        if (!requestedThumbnailRetries.add(link)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.resolveThumbnailIfNeeded(link)
            } catch (e: Exception) {
                android.util.Log.d("NewsViewModel", "On-demand thumbnail retry failed: ${UrlSecurityValidator.normalizeUrl(link)?.toHttpUrlOrNull()?.host ?: "[invalid]"}: ${e.message}")
            }
        }
    }

    fun triggerPrefsChanged() {
        _prefsChangedSignal.value = _prefsChangedSignal.value + 1
    }

    fun cleanTitle(title: String): String {
        var cleaned = title.lowercase().trim()
        
        // Remove punctuation
        cleaned = cleaned.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        
        // Clean common source suffixes and prefixes
        val sourceSuffixes = listOf(
            "bbc news", "bbc", "reuters", "cnn", "ap", "ap news", "the guardian", 
            "new york times", "nytimes", "washington post", "wapo", "fox news",
            "cna", "the straits times", "straits times", "bloomberg"
        )
        for (suffix in sourceSuffixes) {
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.removeSuffix(suffix).trim()
            }
            if (cleaned.startsWith(suffix)) {
                cleaned = cleaned.removePrefix(suffix).trim()
            }
        }
        
        // Remove standard stopwords and very short words (length <= 2)
        val stopwords = setOf(
            "a", "an", "the", "in", "on", "at", "by", "of", "to", "for", "with", 
            "and", "but", "or", "is", "are", "was", "were", "has", "have", "had", 
            "that", "this", "these", "those", "will", "would", "could", "should",
            "after", "before", "about", "from", "into", "over", "under", "been",
            "says", "said", "reporting", "reports", "latest", "update", "live", "news"
        )
        
        val words = cleaned.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length > 2 && !stopwords.contains(it) }
        
        return words.joinToString(" ")
    }

    fun calculateJaccardSimilarity(title1: String, title2: String): Float {
        val words1 = cleanTitle(title1).split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = cleanTitle(title2).split(" ").filter { it.isNotBlank() }.toSet()
        if (words1.isEmpty() && words2.isEmpty()) return 1.0f
        if (words1.isEmpty() || words2.isEmpty()) return 0.0f
        
        val intersectionSize = words1.intersect(words2).size
        val unionSize = words1.union(words2).size
        return intersectionSize.toFloat() / unionSize.toFloat()
    }

    fun groupSimilarArticles(articles: List<Article>, threshold: Float = 0.35f): List<uk.sume.streamfolio.data.model.ArticleGroup> {
        val groups = mutableListOf<MutableList<Article>>()
        
        for (article in articles) {
            var placed = false
            for (group in groups) {
                val primary = group[0]
                val similarity = calculateJaccardSimilarity(primary.title, article.title)
                if (similarity >= threshold) {
                    group.add(article)
                    placed = true
                    break
                }
            }
            if (!placed) {
                groups.add(mutableListOf(article))
            }
        }
        
        return groups.map { group ->
            val sortedGroup = group.sortedWith(
                compareByDescending<Article> { it.thumbnailUrl != null && it.thumbnailUrl.isNotBlank() && it.thumbnailUrl != "failed" }
                    .thenByDescending { it.description.length }
                    .thenByDescending { it.pubDate }
            )
            uk.sume.streamfolio.data.model.ArticleGroup(
                primary = sortedGroup[0],
                secondary = sortedGroup.drop(1)
            )
        }.sortedByDescending { it.primary.pubDate }
    }
}

sealed interface AiSummaryState {
    object Idle : AiSummaryState
    object Loading : AiSummaryState
    object DownloadingModel : AiSummaryState
    data class Success(val summary: String) : AiSummaryState
    data class Error(val message: String) : AiSummaryState
}
