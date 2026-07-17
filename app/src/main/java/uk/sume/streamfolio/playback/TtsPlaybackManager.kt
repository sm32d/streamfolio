package uk.sume.streamfolio.playback

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
import uk.sume.streamfolio.data.model.PodcastEpisode
import uk.sume.streamfolio.data.network.NewsRepository
import uk.sume.streamfolio.data.network.PodcastRepository
import uk.sume.streamfolio.data.network.PodcastIndexApi
import uk.sume.streamfolio.ui.components.TtsHelper
import uk.sume.streamfolio.util.TranscriptParser
import uk.sume.streamfolio.util.TranscriptSegment
import java.io.File

enum class MediaType {
    NONE, TTS, PODCAST
}

class TtsPlaybackManager private constructor(
    private val appContext: Context
) {

    private val repository = NewsRepository(appContext)
    private val podcastRepository = PodcastRepository(appContext)
    private val prefs = PreferencesHelper(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val ttsHelper = TtsHelper(appContext)
    private var exoPlayer: ExoPlayer? = null

    // --- Media Type State ---
    private val _currentMediaType = MutableStateFlow(MediaType.NONE)
    val currentMediaType: StateFlow<MediaType> = _currentMediaType.asStateFlow()

    // --- TTS Flows ---
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

    // --- Podcast Flows ---
    private val _currentEpisode = MutableStateFlow<PodcastEpisode?>(null)
    val currentEpisode: StateFlow<PodcastEpisode?> = _currentEpisode.asStateFlow()

    private val _isPlayingPodcast = MutableStateFlow(false)
    val isPlayingPodcast: StateFlow<Boolean> = _isPlayingPodcast.asStateFlow()

    private val _podcastPlaybackPosition = MutableStateFlow(0L)
    val podcastPlaybackPosition: StateFlow<Long> = _podcastPlaybackPosition.asStateFlow()

    private val _podcastDuration = MutableStateFlow(0L)
    val podcastDuration: StateFlow<Long> = _podcastDuration.asStateFlow()

    private val _podcastBufferedPosition = MutableStateFlow(0L)
    val podcastBufferedPosition: StateFlow<Long> = _podcastBufferedPosition.asStateFlow()

    private val _podcastPlaybackSpeed = MutableStateFlow(prefs.podcastPlaybackSpeed)
    val podcastPlaybackSpeed: StateFlow<Float> = _podcastPlaybackSpeed.asStateFlow()

    private val _podcastTranscriptSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val podcastTranscriptSegments: StateFlow<List<TranscriptSegment>> = _podcastTranscriptSegments.asStateFlow()

    private val _isLoadingTranscript = MutableStateFlow(false)
    val isLoadingTranscript: StateFlow<Boolean> = _isLoadingTranscript.asStateFlow()

    // --- Shared sleep timer ---
    private val _sleepTimerRemainingMillis = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMillis: StateFlow<Long?> = _sleepTimerRemainingMillis.asStateFlow()

    private var playbackJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var progressPollingJob: Job? = null
    private var transcriptJob: Job? = null

    init {
        ttsHelper.setSpeechRate(prefs.ttsSpeechRate)
        ttsHelper.onArticleCompleted = {
            advanceTtsPlaylist()
        }
    }

    private fun initExoPlayerIfNeeded() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(appContext).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlayingPodcast.value = isPlaying
                        if (isPlaying) {
                            startProgressPolling()
                        } else {
                            stopProgressPolling()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            _podcastDuration.value = duration
                        } else if (state == Player.STATE_ENDED) {
                            handleEpisodeCompleted()
                        }
                    }
                })
                setPlaybackSpeed(prefs.podcastPlaybackSpeed)
            }
        }
    }

    // --- TTS Operations ---
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

        // Pause podcast first
        pauseEpisodeInternal()
        _currentMediaType.value = MediaType.TTS

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
        when (_currentMediaType.value) {
            MediaType.TTS -> {
                if (ttsHelper.isPlaying.value) {
                    ttsHelper.pause()
                } else {
                    ensureServiceRunning()
                    ttsHelper.resume()
                }
            }
            MediaType.PODCAST -> {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        pauseEpisode()
                    } else {
                        resumeEpisode()
                    }
                }
            }
            MediaType.NONE -> {
                resumePlayback()
            }
        }
    }

    fun pausePlayback() {
        when (_currentMediaType.value) {
            MediaType.TTS -> ttsHelper.pause()
            MediaType.PODCAST -> pauseEpisode()
            else -> {}
        }
    }

    fun resumePlayback() {
        ensureServiceRunning()
        when (_currentMediaType.value) {
            MediaType.TTS -> {
                val activeIndex = _currentTtsArticleIndex.value
                when {
                    activeIndex != -1 && _articleBody.value.isEmpty() -> playTtsPlaylist(activeIndex)
                    activeIndex != -1 -> ttsHelper.resume()
                    _ttsPlaylist.value.isNotEmpty() -> playTtsPlaylist(0)
                }
            }
            MediaType.PODCAST -> {
                resumeEpisode()
            }
            MediaType.NONE -> {
                // Default fallback
                if (_ttsPlaylist.value.isNotEmpty()) {
                    playTtsPlaylist(0)
                }
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

    fun playPreviousArticle() {
        val previousIndex = _currentTtsArticleIndex.value - 1
        if (previousIndex >= 0) {
            playTtsPlaylist(previousIndex)
        }
    }

    fun speakArticle(article: Article) {
        val activeArticle = currentArticle.value
        if (_currentMediaType.value == MediaType.TTS && activeArticle?.link == article.link) {
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
        if (_currentMediaType.value == MediaType.TTS) {
            _currentMediaType.value = MediaType.NONE
        }
        cancelSleepTimer()
    }

    fun seekToParagraph(index: Int) {
        if (_currentMediaType.value == MediaType.TTS) {
            ttsHelper.seekToParagraph(index)
        }
    }

    fun setTtsSpeechRate(rate: Float) {
        prefs.ttsSpeechRate = rate
        _ttsSpeechRate.value = rate
        ttsHelper.setSpeechRate(rate)
    }

    // --- Podcast Operations ---
    fun playEpisode(episode: PodcastEpisode) {
        // Stop TTS first
        stopSpeakingPlaylist()
        _currentMediaType.value = MediaType.PODCAST

        ensureServiceRunning()
        initExoPlayerIfNeeded()

        _currentEpisode.value = episode
        _podcastPlaybackPosition.value = episode.playbackPositionMs
        _podcastDuration.value = episode.durationSeconds * 1000L

        loadTranscriptFor(episode)

        val uri = if (episode.isDownloaded && episode.localFilePath != null) {
            val file = File(episode.localFilePath)
            if (file.exists()) Uri.fromFile(file) else Uri.parse(episode.audioUrl)
        } else {
            Uri.parse(episode.audioUrl)
        }

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            seekTo(episode.playbackPositionMs)
            play()
        }
    }

    fun pauseEpisode() {
        pauseEpisodeInternal()
    }

    private fun pauseEpisodeInternal() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
            stopProgressPolling()
        }
    }

    fun resumeEpisode() {
        val episode = _currentEpisode.value ?: return
        _currentMediaType.value = MediaType.PODCAST
        ensureServiceRunning()
        initExoPlayerIfNeeded()

        exoPlayer?.let { player ->
            if (!player.isPlaying) {
                player.play()
            }
        }
    }

    fun seekEpisodeTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _podcastPlaybackPosition.value = positionMs
        _currentEpisode.value?.let { ep ->
            scope.launch {
                podcastRepository.updatePlaybackPosition(ep.episodeId, positionMs)
            }
        }
    }

    fun setPodcastSpeed(speed: Float) {
        prefs.podcastPlaybackSpeed = speed
        _podcastPlaybackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
    }

    fun skipForward() {
        exoPlayer?.let { player ->
            val skipMs = prefs.podcastSkipForwardSec * 1000L
            val target = (player.currentPosition + skipMs).coerceAtMost(player.duration)
            seekEpisodeTo(target)
        }
    }

    fun skipBackward() {
        exoPlayer?.let { player ->
            val skipMs = prefs.podcastSkipBackwardSec * 1000L
            val target = (player.currentPosition - skipMs).coerceAtLeast(0L)
            seekEpisodeTo(target)
        }
    }

    fun toggleEpisodePlayback(episode: PodcastEpisode) {
        val active = _currentEpisode.value
        if (_currentMediaType.value == MediaType.PODCAST && active?.episodeId == episode.episodeId) {
            togglePlayback()
        } else {
            playEpisode(episode)
        }
    }

    private fun handleEpisodeCompleted() {
        stopProgressPolling()
        _isPlayingPodcast.value = false
        _podcastPlaybackPosition.value = 0
        val ep = _currentEpisode.value
        if (ep != null) {
            scope.launch {
                podcastRepository.updatePlaybackPosition(ep.episodeId, 0L)
                podcastRepository.updateReadStatus(ep.episodeId, true)
            }
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    _podcastPlaybackPosition.value = player.currentPosition
                    _podcastBufferedPosition.value = player.bufferedPosition
                    _currentEpisode.value?.let { ep ->
                        podcastRepository.updatePlaybackPosition(ep.episodeId, player.currentPosition)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    private fun loadTranscriptFor(episode: PodcastEpisode) {
        transcriptJob?.cancel()
        _podcastTranscriptSegments.value = emptyList()
        val url = episode.transcriptUrl ?: return

        _isLoadingTranscript.value = true
        transcriptJob = scope.launch(Dispatchers.IO) {
            try {
                val api = PodcastIndexApi()
                val text = api.fetchTranscriptText(url)
                val parsed = TranscriptParser.parse(text)
                _podcastTranscriptSegments.value = parsed
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error downloading/parsing transcript", e)
            } finally {
                _isLoadingTranscript.value = false
            }
        }
    }

    fun stopPodcastPlayback() {
        pauseEpisodeInternal()
        exoPlayer?.stop()
        _currentEpisode.value = null
        _isPlayingPodcast.value = false
        _podcastPlaybackPosition.value = 0L
        _podcastDuration.value = 0L
        _podcastBufferedPosition.value = 0L
        _podcastTranscriptSegments.value = emptyList()
        if (_currentMediaType.value == MediaType.PODCAST) {
            _currentMediaType.value = MediaType.NONE
        }
        cancelSleepTimer()
    }

    fun releaseExoPlayer() {
        stopProgressPolling()
        exoPlayer?.release()
        exoPlayer = null
    }

    // --- Sleep Timer Operations ---
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
            stopAllPlayback()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMillis.value = null
    }

    fun stopAllPlayback() {
        stopSpeakingPlaylist()
        stopPodcastPlayback()
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
