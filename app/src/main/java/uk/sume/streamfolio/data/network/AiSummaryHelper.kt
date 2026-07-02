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
     * Runs on-device AI summarization.
     */
    override suspend fun summarizeText(text: String): String {
        val request = SummarizationRequest.builder(text).build()
        val result = client.runInference(request).await()
        return result.summary
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
