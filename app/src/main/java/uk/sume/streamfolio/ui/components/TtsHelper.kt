package uk.sume.streamfolio.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex

    private val _currentWordRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentWordRange: StateFlow<Pair<Int, Int>?> = _currentWordRange

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate

    private var paragraphsList: List<String> = emptyList()
    private var activeParagraphOffset = 0
    private var lastPausedCharOffset = 0

    val paragraphsCount: Int
        get() = paragraphsList.size

    var onArticleCompleted: (() -> Unit)? = null

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                _currentWordRange.value = null
                utteranceId?.toIntOrNull()?.let { index ->
                    if (_currentParagraphIndex.value != index) {
                        _currentParagraphIndex.value = index
                        activeParagraphOffset = 0
                        lastPausedCharOffset = 0
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                _currentWordRange.value = null
                activeParagraphOffset = 0
                lastPausedCharOffset = 0
                utteranceId?.toIntOrNull()?.let { index ->
                    val nextIndex = index + 1
                    if (nextIndex < paragraphsList.size) {
                        speakParagraph(nextIndex)
                    } else {
                        _isPlaying.value = false
                        _currentParagraphIndex.value = 0
                        onArticleCompleted?.invoke()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
                _currentWordRange.value = null
                activeParagraphOffset = 0
                lastPausedCharOffset = 0
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                super.onError(utteranceId, errorCode)
                _isPlaying.value = false
                _currentWordRange.value = null
                activeParagraphOffset = 0
                lastPausedCharOffset = 0
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                val absoluteStart = start + activeParagraphOffset
                val absoluteEnd = end + activeParagraphOffset
                _currentWordRange.value = Pair(absoluteStart, absoluteEnd)
                lastPausedCharOffset = absoluteStart
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            selectBestVoice()
            tts?.setSpeechRate(_speechRate.value)
            tts?.setPitch(0.98f) // Slightly lower pitch for more natural resonance
            isInitialized = true
        } else {
            Log.e("TtsHelper", "Initialization failed")
        }
    }

    private fun selectBestVoice() {
        val ttsEngine = tts ?: return
        try {
            val currentLocale = ttsEngine.language ?: Locale.getDefault()
            val availableVoices = ttsEngine.voices ?: return
            
            // Filter voices for current language
            val localeVoices = availableVoices.filter { 
                it.locale.language == currentLocale.language 
            }
            
            if (localeVoices.isEmpty()) return

            // Sort voices by quality and network dependency:
            // 1. Prefer higher quality voices
            // 2. Prefer embedded (offline) high-quality voices to avoid latency/network dependence
            val bestVoice = localeVoices.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.isNetworkConnectionRequired }
            ).firstOrNull()

            bestVoice?.let {
                ttsEngine.voice = it
                Log.d("TtsHelper", "Selected best voice: ${it.name} (Quality: ${it.quality}, Network: ${it.isNetworkConnectionRequired})")
            }
        } catch (e: Exception) {
            Log.e("TtsHelper", "Error selecting best voice", e)
        }
    }


    fun playOrPause(text: String) {
        if (!isInitialized) return
        if (_isPlaying.value) {
            pause()
        } else {
            val paragraphs = text.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
            if (paragraphs.isEmpty()) return
            
            if (paragraphsList == paragraphs) {
                resume()
            } else {
                paragraphsList = paragraphs
                _currentParagraphIndex.value = 0
                speakParagraph(0)
            }
        }
    }

    fun play(text: String) {
        if (!isInitialized) return
        val paragraphs = text.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return
        
        paragraphsList = paragraphs
        _currentParagraphIndex.value = 0
        speakParagraph(0)
    }

    fun resume() {
        if (!isInitialized || paragraphsList.isEmpty()) return
        speakParagraph(_currentParagraphIndex.value)
    }

    fun pause() {
        if (!isInitialized) return
        activeParagraphOffset = lastPausedCharOffset
        tts?.stop()
        _isPlaying.value = false
    }

    fun stop() {
        if (!isInitialized) return
        tts?.stop()
        _isPlaying.value = false
        _currentParagraphIndex.value = 0
        activeParagraphOffset = 0
        lastPausedCharOffset = 0
    }

    fun seekToParagraph(index: Int) {
        if (!isInitialized) return
        if (index < 0 || index >= paragraphsList.size) return
        activeParagraphOffset = 0
        lastPausedCharOffset = 0
        speakParagraph(index)
    }

    private fun speakParagraph(index: Int) {
        if (index < 0 || index >= paragraphsList.size) return
        _currentParagraphIndex.value = index
        _isPlaying.value = true
        
        val fullText = paragraphsList[index]
        val textToSpeak = if (activeParagraphOffset > 0 && activeParagraphOffset < fullText.length) {
            fullText.substring(activeParagraphOffset)
        } else {
            fullText
        }
        
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
            if (_isPlaying.value) {
                // Instantly re-trigger playback to apply the speed rate changes
                pause()
                resume()
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
