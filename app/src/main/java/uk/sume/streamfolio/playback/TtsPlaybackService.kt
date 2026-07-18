package uk.sume.streamfolio.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import uk.sume.streamfolio.MainActivity
import uk.sume.streamfolio.R
import uk.sume.streamfolio.data.model.Article

class TtsPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var playbackManager: TtsPlaybackManager
    private lateinit var mediaSession: MediaSessionCompat

    private var isForegroundStarted = false

    override fun onCreate() {
        super.onCreate()
        playbackManager = TtsPlaybackManager.getInstance(applicationContext)
        createNotificationChannel()
        setupMediaSession()
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAYBACK -> playbackManager.togglePlayback()
            ACTION_NEXT_ARTICLE -> playbackManager.advanceTtsPlaylist()
            ACTION_PREVIOUS_ARTICLE -> playbackManager.playPreviousArticle()
            ACTION_STOP -> {
                playbackManager.stopSpeakingPlaylist()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        refreshForegroundState(forceStart = true)
        return START_STICKY
    }

    override fun onDestroy() {
        playbackManager.forceSavePlaylist()
        serviceScope.cancel()
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "StreamFolioTts").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setSessionActivity(createContentIntent())
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playbackManager.resumePlayback()
                }

                override fun onPause() {
                    playbackManager.pausePlayback()
                }

                override fun onSkipToNext() {
                    playbackManager.advanceTtsPlaylist()
                }

                override fun onSkipToPrevious() {
                    playbackManager.playPreviousArticle()
                }

                override fun onStop() {
                    playbackManager.stopSpeakingPlaylist()
                    stopForegroundCompat()
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            combine(
                playbackManager.currentArticle,
                playbackManager.currentTtsArticleIndex,
                playbackManager.ttsPlaylist,
                playbackManager.ttsHelper.isPlaying,
                playbackManager.sleepTimerRemainingMillis
            ) { article, _, _, _, _ ->
                article
            }.collect {
                refreshForegroundState()
            }
        }
    }

    private fun refreshForegroundState(forceStart: Boolean = false) {
        val article = playbackManager.currentArticle.value
        val sleepRemaining = playbackManager.sleepTimerRemainingMillis.value
        val hasActiveSession = article != null || sleepRemaining != null

        if (!hasActiveSession) {
            stopForegroundCompat()
            stopSelf()
            return
        }

        updateMediaSession(article)
        val notification = buildNotification(
            article = article,
            isPlaying = playbackManager.ttsHelper.isPlaying.value,
            sleepRemainingMillis = sleepRemaining
        )

        if (!isForegroundStarted || forceStart) {
            startForeground(NOTIFICATION_ID, notification)
            isForegroundStarted = true
        } else {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun updateMediaSession(article: Article?) {
        val currentIndex = playbackManager.currentTtsArticleIndex.value
        val playlistSize = playbackManager.ttsPlaylist.value.size
        val canSkipPrevious = currentIndex > 0
        val canSkipNext = currentIndex in 0 until (playlistSize - 1)
        val baseActions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_STOP
        val skipActions = (if (canSkipPrevious) PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS else 0L) or
            (if (canSkipNext) PlaybackStateCompat.ACTION_SKIP_TO_NEXT else 0L)

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(baseActions or skipActions)
            .setState(
                if (playbackManager.ttsHelper.isPlaying.value) {
                    PlaybackStateCompat.STATE_PLAYING
                } else {
                    PlaybackStateCompat.STATE_PAUSED
                },
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                playbackManager.ttsSpeechRate.value
            )
            .build()
        mediaSession.setPlaybackState(playbackState)

        val metadata = MediaMetadataCompat.Builder().apply {
            putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                article?.title ?: getString(R.string.app_name)
            )
            putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                article?.sourceName ?: "Text to speech"
            )
            article?.thumbnailUrl?.takeIf { it.isNotBlank() && it != "failed" }?.let { thumbnail ->
                putString(MediaMetadataCompat.METADATA_KEY_ART_URI, thumbnail)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, thumbnail)
            }
        }.build()
        mediaSession.setMetadata(metadata)
    }

    private fun buildNotification(
        article: Article?,
        isPlaying: Boolean,
        sleepRemainingMillis: Long?
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_tts_notification)
        .setContentTitle(article?.title ?: "TTS player active")
        .setContentText(buildNotificationText(article, sleepRemainingMillis))
        .setContentIntent(createContentIntent())
        .setDeleteIntent(createServicePendingIntent(ACTION_STOP))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .setOngoing(article != null)
        .setSilent(true)
        .addAction(
            android.R.drawable.ic_media_previous,
            "Previous",
            createServicePendingIntent(ACTION_PREVIOUS_ARTICLE)
        )
        .addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            createServicePendingIntent(ACTION_TOGGLE_PLAYBACK)
        )
        .addAction(
            android.R.drawable.ic_media_next,
            "Next",
            createServicePendingIntent(ACTION_NEXT_ARTICLE)
        )
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            createServicePendingIntent(ACTION_STOP)
        )
        .setStyle(
            MediaNotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        .build()

    private fun buildNotificationText(article: Article?, sleepRemainingMillis: Long?): String {
        val source = article?.sourceName ?: "StreamFolio"
        val timerText = sleepRemainingMillis?.let { "Sleep in ${formatSleepTimer(it)}" }
        return listOf(source, timerText).filterNotNull().joinToString(" • ")
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(packageName)
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_CONTENT_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TtsPlaybackService::class.java).apply {
            this.action = action
            setPackage(packageName)
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "TTS Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Playback controls for article text to speech"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun stopForegroundCompat() {
        if (!isForegroundStarted) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForegroundStarted = false
    }

    private fun formatSleepTimer(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) {
            "${minutes}m"
        } else {
            "${seconds}s"
        }
    }

    companion object {
        private const val CHANNEL_ID = "tts_playback"
        private const val NOTIFICATION_ID = 7001
        private const val REQUEST_CONTENT_INTENT = 7002

        private const val ACTION_TOGGLE_PLAYBACK = "uk.sume.streamfolio.action.TOGGLE_TTS_PLAYBACK"
        private const val ACTION_NEXT_ARTICLE = "uk.sume.streamfolio.action.NEXT_TTS_ARTICLE"
        private const val ACTION_PREVIOUS_ARTICLE = "uk.sume.streamfolio.action.PREVIOUS_TTS_ARTICLE"
        private const val ACTION_STOP = "uk.sume.streamfolio.action.STOP_TTS_PLAYBACK"

        fun start(context: Context) {
            val intent = Intent(context, TtsPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
