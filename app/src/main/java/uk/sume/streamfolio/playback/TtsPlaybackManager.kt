package uk.sume.streamfolio.playback

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.network.NewsRepository
import uk.sume.streamfolio.ui.components.TtsHelper

class TtsPlaybackManager private constructor(
    private val appContext: Context
) {

    private val repository = NewsRepository(appContext)
    private val prefs = PreferencesHelper(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val ttsHelper = TtsHelper(appContext)

    private val _ttsPlaylist = MutableStateFlow<List<Article>>(emptyList())
    val ttsPlaylist: StateFlow<List<Article>> = _ttsPlaylist.asStateFlow()

    private val _currentTtsArticleIndex = MutableStateFlow(-1)
    val currentTtsArticleIndex: StateFlow<Int> = _currentTtsArticleIndex.asStateFlow()

    val currentArticle: StateFlow<Article?> = combine(_ttsPlaylist, _currentTtsArticleIndex) { playlist, index ->
        playlist.getOrNull(index)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val _articleBody = MutableStateFlow("")
    val articleBody: StateFlow<String> = _articleBody.asStateFlow()

    private val _isLoadingBody = MutableStateFlow(false)
    val isLoadingBody: StateFlow<Boolean> = _isLoadingBody.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(prefs.ttsSpeechRate)
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

    private val _sleepTimerRemainingMillis = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMillis: StateFlow<Long?> = _sleepTimerRemainingMillis.asStateFlow()

    private var playbackJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        ttsHelper.setSpeechRate(prefs.ttsSpeechRate)
        ttsHelper.onArticleCompleted = {
            advanceTtsPlaylist()
        }
    }

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
        if (index == -1) return

        current.removeAt(index)
        _ttsPlaylist.value = current

        val activeIndex = _currentTtsArticleIndex.value
        when {
            current.isEmpty() -> {
                stopSpeakingPlaylist()
            }
            activeIndex == index -> {
                val nextIndex = if (index < current.size) index else current.lastIndex
                playTtsPlaylist(nextIndex)
            }
            index < activeIndex -> {
                _currentTtsArticleIndex.value = activeIndex - 1
            }
        }
    }

    fun clearTtsPlaylist() {
        stopSpeakingPlaylist()
        _ttsPlaylist.value = emptyList()
    }

    fun moveTtsPlaylistItem(fromIndex: Int, toIndex: Int) {
        val current = _ttsPlaylist.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _ttsPlaylist.value = current

        val activeIndex = _currentTtsArticleIndex.value
        if (activeIndex == fromIndex) {
            _currentTtsArticleIndex.value = toIndex
        } else if (activeIndex in (fromIndex + 1)..toIndex) {
            _currentTtsArticleIndex.value = activeIndex - 1
        } else if (activeIndex in toIndex..<fromIndex) {
            _currentTtsArticleIndex.value = activeIndex + 1
        }
    }

    fun playTtsPlaylist(startIndex: Int) {
        val playlist = _ttsPlaylist.value
        if (startIndex !in playlist.indices) return

        ensureServiceRunning()
        _currentTtsArticleIndex.value = startIndex
        val article = playlist[startIndex]

        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                _isLoadingBody.value = true
                _articleBody.value = ""
                ttsHelper.stop()

                val cached = repository.getArticleByLink(article.link)
                val textToSpeak = if (cached?.fullText != null) {
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
            } catch (error: Exception) {
                _isLoadingBody.value = false
                _articleBody.value = "Failed to load article body for playback."
                stopSpeakingPlaylist()
            }
        }
    }

    fun togglePlayback() {
        if (ttsHelper.isPlaying.value) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun pausePlayback() {
        ttsHelper.pause()
    }

    fun resumePlayback() {
        ensureServiceRunning()
        val activeIndex = _currentTtsArticleIndex.value
        when {
            activeIndex != -1 && _articleBody.value.isEmpty() -> playTtsPlaylist(activeIndex)
            activeIndex != -1 -> ttsHelper.resume()
            _ttsPlaylist.value.isNotEmpty() -> playTtsPlaylist(0)
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

    fun playPreviousArticle() {
        val previousIndex = _currentTtsArticleIndex.value - 1
        if (previousIndex >= 0) {
            playTtsPlaylist(previousIndex)
        }
    }

    fun speakArticle(article: Article) {
        val activeArticle = currentArticle.value
        if (activeArticle?.link == article.link) {
            togglePlayback()
        } else {
            _ttsPlaylist.value = listOf(article)
            playTtsPlaylist(0)
        }
    }

    fun stopSpeakingPlaylist() {
        playbackJob?.cancel()
        ttsHelper.stop()
        _currentTtsArticleIndex.value = -1
        _articleBody.value = ""
        _isLoadingBody.value = false
        cancelSleepTimer()
    }

    fun seekToParagraph(index: Int) {
        ttsHelper.seekToParagraph(index)
    }

    fun setTtsSpeechRate(rate: Float) {
        prefs.ttsSpeechRate = rate
        _ttsSpeechRate.value = rate
        ttsHelper.setSpeechRate(rate)
    }

    fun setSleepTimer(durationMinutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        if (durationMinutes == null) {
            _sleepTimerRemainingMillis.value = null
            return
        }

        ensureServiceRunning()
        val durationMillis = durationMinutes * 60_000L
        val endsAt = SystemClock.elapsedRealtime() + durationMillis
        _sleepTimerRemainingMillis.value = durationMillis

        sleepTimerJob = scope.launch {
            while (true) {
                val remaining = endsAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) break
                _sleepTimerRemainingMillis.value = remaining
                delay(1_000L)
            }
            stopSpeakingPlaylist()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMillis.value = null
    }

    private fun ensureServiceRunning() {
        TtsPlaybackService.start(appContext)
    }

    companion object {
        @Volatile
        private var instance: TtsPlaybackManager? = null

        fun getInstance(context: Context): TtsPlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: TtsPlaybackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
