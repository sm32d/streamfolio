package uk.sume.news.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

    private var paragraphsList: List<String> = emptyList()

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                utteranceId?.toIntOrNull()?.let { index ->
                    _currentParagraphIndex.value = index
                }
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.toIntOrNull()?.let { index ->
                    val nextIndex = index + 1
                    if (nextIndex < paragraphsList.size) {
                        speakParagraph(nextIndex)
                    } else {
                        _isPlaying.value = false
                        _currentParagraphIndex.value = 0
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                super.onError(utteranceId, errorCode)
                _isPlaying.value = false
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
        } else {
            Log.e("TtsHelper", "Initialization failed")
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
        tts?.stop()
        _isPlaying.value = false
    }

    fun stop() {
        if (!isInitialized) return
        tts?.stop()
        _isPlaying.value = false
        _currentParagraphIndex.value = 0
    }

    private fun speakParagraph(index: Int) {
        if (index < 0 || index >= paragraphsList.size) return
        _currentParagraphIndex.value = index
        _isPlaying.value = true
        
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        
        tts?.speak(paragraphsList[index], TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
