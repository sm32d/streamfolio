package uk.sume.news

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URL

class ExampleUnitTest {
    @Test
    fun testAllArticlesResolution() {
        val rssUrl = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        
        try {
            println("Fetching RSS Feed: $rssUrl")
            val doc = Jsoup.connect(rssUrl).get()
            val items = doc.select("item").take(10)
            println("Found ${items.size} articles in feed.")
            
            for ((index, item) in items.withIndex()) {
                val title = item.select("title").text()
                val link = item.select("link").text()
                println("\n--- ARTICLE #${index + 1}: $title ---")
                println("Link: $link")
                
                // 1. Resolve URL
                val resolvedUrl = resolveGoogleNewsUrl(link)
                println("Resolved URL: $resolvedUrl")
                
                if (resolvedUrl == link) {
                    println("WARNING: URL could not be resolved (same as original)")
                    continue
                }
                
                // 2. Scraping thumbnail
                try {
                    val targetDoc = Jsoup.connect(resolvedUrl)
                        .userAgent(userAgent)
                        .referrer("https://www.google.com")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Cache-Control", "max-age=0")
                        .header("Connection", "keep-alive")
                        .ignoreHttpErrors(true)
                        .timeout(8000)
                        .get()
                        
                    var imageUrl = targetDoc.select("meta[property=og:image]").attr("content")
                    if (imageUrl.isBlank()) {
                        imageUrl = targetDoc.select("meta[name=twitter:image]").attr("content")
                    }
                    if (imageUrl.isBlank()) {
                        imageUrl = targetDoc.select("article img, main img").firstOrNull()?.attr("abs:src") ?: ""
                    }
                    println("Scraped image URL: $imageUrl")
                    
                    if (imageUrl.contains("googleusercontent.com") || 
                        imageUrl.contains("gstatic.com") || 
                        imageUrl.contains("google.com")) {
                        println("FILTERED: Image URL contains google branding!")
                    }
                } catch (e: Exception) {
                    println("Scrape thumbnail failed: ${e.message}")
                }
                
                // 3. Scraping body
                try {
                    val targetDoc = Jsoup.connect(resolvedUrl)
                        .userAgent(userAgent)
                        .referrer("https://www.google.com")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Cache-Control", "max-age=0")
                        .header("Connection", "keep-alive")
                        .ignoreHttpErrors(true)
                        .timeout(8000)
                        .get()
                        
                    val paragraphs = targetDoc.select("article p, main p, .post-content p, .article-content p, .story-body p")
                    val rawText = if (paragraphs.isNotEmpty()) {
                        paragraphs.map { it.text().trim() }
                            .filter { it.isNotBlank() }
                            .joinToString("\n\n")
                    } else {
                        targetDoc.select("p").map { it.text().trim() }
                            .filter { it.isNotBlank() }
                            .joinToString("\n\n")
                    }
                    
                    println("Scraped body length: ${rawText.length}")
                    if (rawText.length > 200) {
                        println("Snippet: ${rawText.take(150)}...")
                    } else {
                        println("Snippet: $rawText")
                    }
                } catch (e: Exception) {
                    println("Scrape body failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun resolveGoogleNewsUrl(googleUrl: String): String {
        try {
            if (!googleUrl.startsWith("https://news.google.com/")) {
                return googleUrl
            }
            val uri = java.net.URI(googleUrl)
            val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
            if (pathParts.size < 2) return googleUrl
            val base64Str = pathParts.last()
            
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            val redirectDoc = Jsoup.connect(googleUrl)
                .userAgent(userAgent)
                .referrer("https://www.google.com")
                .timeout(6000)
                .get()
            
            val element = redirectDoc.select("[data-n-a-sg]").firstOrNull() ?: return googleUrl
            val signature = element.attr("data-n-a-sg")
            val timestamp = element.attr("data-n-a-ts")
            
            val payload = """[[["Fbv4je","[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$base64Str\",$timestamp,\"$signature\"]",null,"generic"]]]"""
            
            val postResponse = Jsoup.connect("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .userAgent(userAgent)
                .data("f.req", payload)
                .ignoreContentType(true)
                .timeout(6000)
                .execute()
            
            val responseBody = postResponse.body()
            val match = Regex("""https://[^"\\ ]+""").find(responseBody)
            return match?.value ?: googleUrl
        } catch (e: Exception) {
            return googleUrl
        }
    }
}