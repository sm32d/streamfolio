package uk.sume.streamfolio.data.network

import android.content.Context

interface IAiSummaryHelper {
    suspend fun checkFeatureStatus(): Int
    suspend fun downloadFeature()
    suspend fun summarizeText(text: String): String
}

/**
 * Strongly-typed domain exceptions for AI summarization.
 * Eliminates fragile string matching on third-party SDK exception messages.
 */
sealed class AiSummaryException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InputTooLongException(val inputLength: Int = 0, cause: Throwable? = null) :
        AiSummaryException("Input text exceeds ML Kit GenAI model capacity", cause)
    
    class SafetyPolicyViolationException(cause: Throwable? = null) :
        AiSummaryException("Content blocked by local AI safety policy check", cause)
    
    class ModelNotAvailableException(val status: Int = 0, cause: Throwable? = null) :
        AiSummaryException("Gemini Nano (AICore) is unavailable or requires update", cause)
    
    class UnknownAiException(cause: Throwable? = null) :
        AiSummaryException(cause?.message ?: "Unknown AI summarization error", cause)

    companion object {
        fun map(e: Throwable, inputLength: Int = 0): AiSummaryException {
            if (e is AiSummaryException) return e
            if (e is java.util.concurrent.CancellationException || e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            val msg = e.message ?: ""
            return when {
                msg.contains("character limit", ignoreCase = true) || 
                msg.contains("exceeds", ignoreCase = true) || 
                msg.contains("7860") || 
                e is IllegalArgumentException -> {
                    InputTooLongException(inputLength, e)
                }
                msg.contains("safety", ignoreCase = true) || 
                msg.contains("policy check", ignoreCase = true) -> {
                    SafetyPolicyViolationException(e)
                }
                msg.contains("AICore", ignoreCase = true) || 
                msg.contains("unavailable", ignoreCase = true) -> {
                    ModelNotAvailableException(cause = e)
                }
                else -> UnknownAiException(e)
            }
        }
    }
}

interface IAiTranslateHelper {
    suspend fun identifyLanguage(text: String): String
    suspend fun translateText(text: String, sourceLangCode: String, targetLangCode: String): String
}

object AiHelperFactory {
    fun createSummaryHelper(context: Context): IAiSummaryHelper? {
        return try {
            Class.forName("uk.sume.streamfolio.data.network.AiSummaryHelperImpl")
            AiSummaryHelperImpl(context)
        } catch (e: Throwable) {
            android.util.Log.e("AiHelperFactory", "Failed to load AiSummaryHelperImpl: ${e.message}")
            null
        }
    }

    fun createTranslateHelper(context: Context): IAiTranslateHelper? {
        return try {
            Class.forName("uk.sume.streamfolio.data.network.AiTranslateHelperImpl")
            AiTranslateHelperImpl(context)
        } catch (e: Throwable) {
            android.util.Log.e("AiHelperFactory", "Failed to load AiTranslateHelperImpl: ${e.message}")
            null
        }
    }
}
