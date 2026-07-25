package uk.sume.streamfolio.data.network

import android.content.Context
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.google.mlkit.genai.summarization.SummarizerOptions.InputType
import com.google.mlkit.genai.summarization.SummarizerOptions.OutputType
import com.google.mlkit.genai.summarization.SummarizerOptions.Language
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.SummarizationResult
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.GenAiException
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AiSummaryHelperImpl(private val context: Context) : IAiSummaryHelper {

    private val options = SummarizerOptions.builder(context)
        .setInputType(InputType.ARTICLE)
        .setOutputType(OutputType.THREE_BULLETS)
        .setLanguage(Language.ENGLISH)
        .setLongInputAutoTruncationEnabled(true)
        .build()

    private val client by lazy {
        Summarization.getClient(options)
    }

    /**
     * Checks if the model is ready.
     */
    override suspend fun checkFeatureStatus(): Int {
        return client.checkFeatureStatus().await()
    }

    /**
     * Downloads the required model feature using the ML Kit download callback.
     */
    override suspend fun downloadFeature() = suspendCancellableCoroutine<Unit> { cont ->
        client.downloadFeature(object : DownloadCallback {
            override fun onDownloadStarted(bytesToDownload: Long) {}
            override fun onDownloadProgress(totalBytesDownloaded: Long) {}
            override fun onDownloadCompleted() {
                cont.resume(Unit)
            }
            override fun onDownloadFailed(e: GenAiException) {
                cont.resumeWithException(e)
            }
        })
    }

    /**
     * Runs on-device AI summarization with smart preprocessing.
     */
    override suspend fun summarizeText(text: String): String {
        return try {
            val preparedText = TextSummarizerPreprocessor.preprocessArticleText(text)
            val request = SummarizationRequest.builder(preparedText).build()
            val result = client.runInference(request).await()
            result.summary
        } catch (e: Throwable) {
            throw AiSummaryException.map(e, text.length)
        }
    }

    // Pure Kotlin extension to convert ListenableFuture to a suspend function
    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
        val executor = Executors.newSingleThreadExecutor()
        this.addListener({
            try {
                val value = this.get()
                cont.resume(value)
            } catch (e: java.util.concurrent.ExecutionException) {
                cont.resumeWithException(e.cause ?: e)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, executor)
    }
}

object TextSummarizerPreprocessor {

    /**
     * Cleans up noise (HTML tags, extra whitespace, boilerplate text) and intelligently
     * structures text so ML Kit Gemini Nano produces maximum quality key takeaways.
     */
    fun preprocessArticleText(rawText: String, maxChars: Int = 6500): String {
        if (rawText.isBlank()) return ""

        // 1. Remove HTML tags if present
        var cleaned = if (rawText.contains("<") && rawText.contains(">")) {
            try {
                org.jsoup.Jsoup.parse(rawText).text()
            } catch (e: Exception) {
                rawText.replace(Regex("<[^>]*>"), " ")
            }
        } else {
            rawText
        }

        // 2. Remove common web noise & boilerplate lines
        cleaned = cleaned
            .replace(Regex("(?i)subscribe to read full article.*"), "")
            .replace(Regex("(?i)all rights reserved.*"), "")
            .replace(Regex("(?i)follow us on (twitter|facebook|instagram|linkedin).*"), "")
            .replace(Regex("(?i)cookie (policy|preferences|settings).*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.length <= maxChars) {
            return cleaned
        }

        // 3. Smart Chunking / Intelligent Truncation for long text (> maxChars)
        // Extract opening lead section (headline facts) and conclusion section
        val splitParagraphs = cleaned.split(Regex("\n\\s*\n|(?<=[.!?])\\s{2,}"))
            .map { it.trim() }
            .filter { it.length > 20 }

        if (splitParagraphs.size <= 3) {
            return truncateOnWordBoundary(cleaned, maxChars)
        }

        val leadBudget = (maxChars * 0.70).toInt()
        val tailBudget = maxChars - leadBudget - 100

        val leadText = StringBuilder()
        var paragraphIdx = 0
        while (paragraphIdx < splitParagraphs.size && (leadText.length + splitParagraphs[paragraphIdx].length + 1) <= leadBudget) {
            if (leadText.isNotEmpty()) leadText.append("\n\n")
            leadText.append(splitParagraphs[paragraphIdx])
            paragraphIdx++
        }

        val tailText = StringBuilder()
        var tailIdx = splitParagraphs.size - 1
        while (tailIdx > paragraphIdx && (tailText.length + splitParagraphs[tailIdx].length + 1) <= tailBudget) {
            if (tailText.isNotEmpty()) tailText.insert(0, "\n\n")
            tailText.insert(0, splitParagraphs[tailIdx])
            tailIdx--
        }

        return if (tailText.isNotEmpty()) {
            "${leadText.toString()}\n\n[...]\n\n${tailText.toString()}"
        } else {
            truncateOnWordBoundary(leadText.toString(), maxChars)
        }
    }

    private fun truncateOnWordBoundary(text: String, limit: Int): String {
        if (text.length <= limit) return text
        val cut = text.substring(0, limit)
        val lastSpace = cut.lastIndexOf(' ')
        return if (lastSpace > limit / 2) {
            cut.substring(0, lastSpace) + "..."
        } else {
            cut + "..."
        }
    }
}

