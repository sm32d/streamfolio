package uk.sume.streamfolio.data.network

import android.content.Context

interface IAiSummaryHelper {
    suspend fun checkFeatureStatus(): Int
    suspend fun downloadFeature()
    suspend fun summarizeText(text: String): String
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
