package uk.sume.news

import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.sume.streamfolio.data.network.RssParser
import uk.sume.streamfolio.data.network.DefaultFeedsConfig

class ExampleUnitTest {
    @Test
    fun testFeeds() {
        val client = OkHttpClient()
        val parser = RssParser()
        
        val failedFeeds = mutableListOf<String>()
        val emptyFeeds = mutableListOf<String>()
        val successfulFeeds = mutableListOf<String>()

        val providers = DefaultFeedsConfig.getAllCuratedProviders()
        for (provider in providers) {
            val region = provider.region
            val category = provider.category
            val url = provider.url
            println("\n=== TESTING FEED [$region][$category]: $url ===")
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val articles = parser.parse(body, category, feedUrl = url)
                        if (articles.isNotEmpty()) {
                            println("SUCCESS: Parsed ${articles.size} articles.")
                            successfulFeeds.add("[$region][$category] $url - ${articles.size} articles")
                        } else {
                            println("EMPTY: 0 articles parsed.")
                            emptyFeeds.add("[$region][$category] $url")
                        }
                    } else {
                        println("FAILED: HTTP ${response.code}")
                        failedFeeds.add("[$region][$category] $url - HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                println("EXCEPTION: ${e.message}")
                failedFeeds.add("[$region][$category] $url - Exception: ${e.message}")
            }
        }
        
        println("\n=======================================")
        println("SUMMARY:")
        println("Successful: ${successfulFeeds.size}/${successfulFeeds.size + failedFeeds.size + emptyFeeds.size}")
        println("Empty: ${emptyFeeds.size}")
        println("Failed: ${failedFeeds.size}")
        
        if (emptyFeeds.isNotEmpty()) {
            println("\nEMPTY FEEDS:")
            emptyFeeds.forEach { println(" - $it") }
        }
        if (failedFeeds.isNotEmpty()) {
            println("\nFAILED FEEDS:")
            failedFeeds.forEach { println(" - $it") }
        }
        println("=======================================")
    }
}