package uk.sume.news

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.Test
import java.net.URL
import java.util.Base64

class ExampleUnitTest {
    @Test
    fun testBatchExecute() {
        val articleLink = "https://news.google.com/rss/articles/CBMilgFBVV95cUxNS283YlpmSzViRkVaWTd0cFlTRldKcGg1N01vODl2b3BmNG1RYVlZVjB4QXI1X3BiVGotYXBGZXJ5bkppckp2ZXVsUHRGUE1OZ0trSGxwTjRxNXBPVDJIbGw3aWVUclQzblgzTUV3ZEdXY2EtR28wdXdFVGFfVFkzTlhiRU5lTWQ0M093YWd1MmNkZHM2SVE?oc=5"
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        
        try {
            // Extract base64
            val urlObj = URL(articleLink)
            val pathParts = urlObj.path.split("/").filter { it.isNotEmpty() }
            val base64Str = pathParts.last()
            println("BASE64 STR: $base64Str")
            
            // Connect to redirect page to get parameters
            val redirectDoc = Jsoup.connect(articleLink)
                .userAgent(userAgent)
                .referrer("https://www.google.com")
                .timeout(10000)
                .get()
            
            val element = redirectDoc.select("[data-n-a-sg]").firstOrNull()
            if (element == null) {
                println("ERROR: Could not find element with data-n-a-sg")
                return
            }
            
            val signature = element.attr("data-n-a-sg")
            val timestamp = element.attr("data-n-a-ts")
            println("SIGNATURE: $signature")
            println("TIMESTAMP: $timestamp")
            
            // Construct payload
            val payload = """[[["Fbv4je","[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$base64Str\",$timestamp,\"$signature\"]",null,"generic"]]]"""
            
            println("Sending batchexecute POST request...")
            val postResponse = Jsoup.connect("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .userAgent(userAgent)
                .data("f.req", payload)
                .ignoreContentType(true)
                .timeout(10000)
                .execute()
            
            val responseBody = postResponse.body()
            println("RESPONSE BODY LENGTH: ${responseBody.length}")
            println("RESPONSE BODY:")
            println(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}