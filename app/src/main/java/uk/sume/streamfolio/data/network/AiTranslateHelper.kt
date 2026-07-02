package uk.sume.streamfolio.data.network

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AiTranslateHelper(private val context: Context) {

    /**
     * Identifies the language of the provided text.
     * Returns BCP 47 language code (e.g. "fr", "de"), or "en" if undetermined.
     */
    suspend fun identifyLanguage(text: String): String {
        val languageIdentifier = LanguageIdentification.getClient()
        return try {
            val languageCode = languageIdentifier.identifyLanguage(text).await()
            if (languageCode == "und") "en" else languageCode
        } catch (e: Exception) {
            "und"
        }
    }

    /**
     * Translates a string from source language to target language.
     * Automatically downloads the required translation models if not already present on-device.
     */
    suspend fun translateText(text: String, sourceLangCode: String, targetLangCode: String): String {
        if (text.isBlank() || sourceLangCode == targetLangCode) return text

        val sourceLanguage = TranslateLanguage.fromLanguageTag(sourceLangCode) ?: return text
        val targetLanguage = TranslateLanguage.fromLanguageTag(targetLangCode) ?: return text

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()

        val translator = Translation.getClient(options)
        return try {
            // Download the models on-device (runs offline afterwards)
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            translator.translate(text).await()
        } finally {
            translator.close()
        }
    }

    // Generic Kotlin extension helper to convert ML Kit Tasks into suspend functions
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("ML Kit Task failed"))
            }
        }
    }
}
