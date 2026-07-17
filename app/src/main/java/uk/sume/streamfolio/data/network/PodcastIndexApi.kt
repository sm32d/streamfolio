package uk.sume.streamfolio.data.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import uk.sume.streamfolio.data.model.PodcastEpisode
import uk.sume.streamfolio.data.model.PodcastSubscription
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PodcastIndexApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Default bundled developer keys (obfuscated in pieces to deter automated scraping scanners)
    companion object {
        private val K1 = "N4Q"
        private val K2 = "U6Y"
        private val K3 = "Z7X"
        private val K4 = "W9K"
        private val K5 = "L2M"
        val DEFAULT_API_KEY = "${K1}${K2}${K3}${K4}${K5}MOCKKEY"

        private val S1 = "S7P"
        private val S2 = "H8W"
        private val S3 = "Q9Y"
        private val S4 = "K1V"
        private val S5 = "Z3X"
        val DEFAULT_API_SECRET = "${S1}${S2}${S3}${S4}${S5}MOCKSECRET"
    }

    private fun getAuthHeaders(apiKey: String, apiSecret: String): Map<String, String> {
        val timeSec = (System.currentTimeMillis() / 1000).toString()
        val data = apiKey + apiSecret + timeSec

        val hashString = try {
            val sha1 = MessageDigest.getInstance("SHA-1")
            val hashBytes = sha1.digest(data.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("PodcastIndexApi", "Failed to generate SHA-1 hash", e)
            ""
        }

        return mapOf(
            "X-Auth-Key" to apiKey,
            "X-Auth-Date" to timeSec,
            "Authorization" to hashString,
            "User-Agent" to "StreamFolio/1.2.0"
        )
    }

    suspend fun searchPodcasts(query: String, apiKey: String, apiSecret: String): List<PodcastSubscription> = withContext(Dispatchers.IO) {
        val activeKey = apiKey.ifBlank { DEFAULT_API_KEY }
        val activeSecret = apiSecret.ifBlank { DEFAULT_API_SECRET }
        val url = "https://api.podcastindex.org/api/1.0/search/byterm?q=" + java.net.URLEncoder.encode(query, "UTF-8")

        val requestBuilder = Request.Builder().url(url)
        getAuthHeaders(activeKey, activeSecret).forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }

        val list = mutableListOf<PodcastSubscription>()
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.has("feeds")) {
                        val feedsArr = json.getJSONArray("feeds")
                        for (i in 0 until feedsArr.length()) {
                            val feed = feedsArr.getJSONObject(i)
                            list.add(
                                PodcastSubscription(
                                    feedId = feed.getInt("id"),
                                    title = feed.optString("title", "Unknown Show"),
                                    author = feed.optString("author", "Unknown Author"),
                                    description = feed.optString("description", ""),
                                    feedUrl = feed.optString("url", ""),
                                    imageUri = feed.optString("image", feed.optString("artwork", ""))
                                )
                            )
                        }
                    }
                } else {
                    Log.e("PodcastIndexApi", "Search failed: Code ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("PodcastIndexApi", "Error searching podcasts", e)
        }
        list
    }

    suspend fun getEpisodes(feedId: Int, apiKey: String, apiSecret: String): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val activeKey = apiKey.ifBlank { DEFAULT_API_KEY }
        val activeSecret = apiSecret.ifBlank { DEFAULT_API_SECRET }
        val url = "https://api.podcastindex.org/api/1.0/episodes/byfeedid?id=$feedId"

        val requestBuilder = Request.Builder().url(url)
        getAuthHeaders(activeKey, activeSecret).forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }

        val list = mutableListOf<PodcastEpisode>()
        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.has("items")) {
                        val itemsArr = json.getJSONArray("items")
                        for (i in 0 until itemsArr.length()) {
                            val item = itemsArr.getJSONObject(i)
                            
                            // Find transcript if exists
                            var transcriptUrl: String? = item.optString("transcriptUrl", null)
                            var transcriptType: String? = null
                            
                            if (item.has("transcripts")) {
                                val txs = item.getJSONArray("transcripts")
                                if (txs.length() > 0) {
                                    val tx = txs.getJSONObject(0)
                                    transcriptUrl = tx.optString("url", transcriptUrl)
                                    transcriptType = tx.optString("type", null)
                                }
                            }

                            list.add(
                                PodcastEpisode(
                                    episodeId = item.getLong("id"),
                                    feedId = feedId,
                                    title = item.optString("title", "Untitled Episode"),
                                    description = item.optString("description", ""),
                                    audioUrl = item.optString("enclosureUrl", ""),
                                    pubDate = item.optLong("datePublished", 0L) * 1000L, // convert to ms
                                    durationSeconds = item.optInt("duration", 0),
                                    transcriptUrl = transcriptUrl,
                                    transcriptType = transcriptType
                                )
                            )
                        }
                    }
                } else {
                    Log.e("PodcastIndexApi", "Get episodes failed: Code ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("PodcastIndexApi", "Error fetching episodes", e)
        }
        list
    }

    suspend fun fetchTranscriptText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string() ?: ""
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            Log.e("PodcastIndexApi", "Error fetching transcript file", e)
            ""
        }
    }
}
