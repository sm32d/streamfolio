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
        
        val urls = listOf(
            "https://sports.yahoo.com/rss/",
            "https://www.hollywoodreporter.com/feed/",
            "https://variety.com/feed/",
            "https://www.espn.com/espn/rss/news"
        )
        
        for (url in urls) {
            println("\n=== TESTING FEED: $url ===")
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    println("HTTP Status: ${response.code} ${response.message}")
                    val body = response.body?.string() ?: ""
                    println("Body Length: ${body.length} characters")
                    if (response.isSuccessful) {
                        val articles = parser.parse(body, "Sports", feedUrl = url)
                        println("Successfully parsed ${articles.size} articles.")
                        if (articles.isNotEmpty()) {
                            println("First Article Title: ${articles[0].title}")
                            println("First Article SourceName: ${articles[0].sourceName}")
                            println("First Article SourceUrl: ${articles[0].sourceUrl}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Exception for $url: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}