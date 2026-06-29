package uk.sume.streamfolio.data.network

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import uk.sume.streamfolio.data.model.Article
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RssParser {

    /**
     * Checks if the XML document is an OPML list
     */
    fun isOpml(xmlContent: String): Boolean {
        val trimmed = xmlContent.trimStart()
        val limit = 500.coerceAtMost(trimmed.length)
        val prefix = trimmed.substring(0, limit).lowercase()
        return prefix.contains("<opml") && !prefix.contains("<rss") && !prefix.contains("<feed")
    }

    /**
     * Parses OPML xml content and returns a list of nested feed xmlUrls
     */
    fun parseOpmlUrls(xmlContent: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                    if (!xmlUrl.isNullOrBlank()) {
                        urls.add(xmlUrl)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return urls
    }

    /**
     * Unified parser supporting standard RSS (<channel>/<item>) and Atom (<feed>/<entry>) formats.
     */
    fun parse(xmlContent: String, category: String, customFeedId: Int? = null): List<Article> {
        val articles = mutableListOf<Article>()
        var channelTitle = "Unknown Source"
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            var currentArticleBuilder: ArticleBuilder? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        // Detect start of RSS item or Atom entry
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            currentArticleBuilder = ArticleBuilder()
                        } else if (currentArticleBuilder != null) {
                            when {
                                name.equals("title", ignoreCase = true) -> {
                                    currentArticleBuilder.title = parser.nextText()
                                }
                                name.equals("link", ignoreCase = true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        currentArticleBuilder.link = href
                                    } else {
                                        currentArticleBuilder.link = parser.nextText()
                                    }
                                }
                                name.equals("description", ignoreCase = true) || 
                                name.equals("summary", ignoreCase = true) -> {
                                    currentArticleBuilder.description = parser.nextText()
                                }
                                name.equals("content", ignoreCase = true) || 
                                name.equals("content:encoded", ignoreCase = true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (url != null) {
                                        // Media content (image)
                                        currentArticleBuilder.thumbnailUrl = url
                                    } else {
                                        // Atom content tag (HTML body)
                                        currentArticleBuilder.description = parser.nextText()
                                    }
                                }
                                name.equals("pubDate", ignoreCase = true) || 
                                name.equals("published", ignoreCase = true) || 
                                name.equals("updated", ignoreCase = true) -> {
                                    currentArticleBuilder.pubDate = parser.nextText()
                                }
                                name.equals("source", ignoreCase = true) -> {
                                    currentArticleBuilder.sourceUrl = parser.getAttributeValue(null, "url") ?: ""
                                    currentArticleBuilder.sourceName = parser.nextText()
                                }
                                name.equals("enclosure", ignoreCase = true) -> {
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    if (type.startsWith("image/") || type.contains("image")) {
                                        currentArticleBuilder.thumbnailUrl = parser.getAttributeValue(null, "url")
                                    }
                                }
                                name.equals("media:content", ignoreCase = true) -> {
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    val url = parser.getAttributeValue(null, "url")
                                    if (url != null && (type.isEmpty() || type.startsWith("image/") || type.contains("image"))) {
                                        currentArticleBuilder.thumbnailUrl = url
                                    }
                                }
                                name.equals("media:thumbnail", ignoreCase = true) -> {
                                    currentArticleBuilder.thumbnailUrl = parser.getAttributeValue(null, "url")
                                }
                            }
                        } else if (name.equals("title", ignoreCase = true)) {
                            // Main channel/feed title
                            channelTitle = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) && 
                            currentArticleBuilder != null) {
                            val article = currentArticleBuilder.build(category, customFeedId, channelTitle)
                            if (article != null) {
                                articles.add(article)
                            }
                            currentArticleBuilder = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return articles
    }

    private class ArticleBuilder {
        var title: String? = null
        var link: String? = null
        var description: String? = null
        var pubDate: String? = null
        var sourceName: String? = null
        var sourceUrl: String? = null
        var thumbnailUrl: String? = null

        fun build(category: String, customFeedId: Int? = null, channelFallback: String = "Unknown Source"): Article? {
            val articleLink = link ?: return null
            val rawTitle = title ?: "No Title"
            
            var cleanTitle = rawTitle
            var cleanSource = sourceName ?: channelFallback
            if (cleanSource.isBlank()) {
                cleanSource = channelFallback
            }
            
            val lastDashIndex = rawTitle.lastIndexOf(" - ")
            if (lastDashIndex != -1 && lastDashIndex > rawTitle.length - 40) {
                val suffix = rawTitle.substring(lastDashIndex + 3).trim()
                // Only split if the suffix is capitalized (likely a publisher name)
                if (suffix.isNotEmpty() && suffix[0].isUpperCase()) {
                    cleanTitle = rawTitle.substring(0, lastDashIndex).trim()
                    if (sourceName == null || sourceName!!.isEmpty()) {
                        cleanSource = suffix
                    }
                }
            }

            val cleanDesc = description?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""

            return Article(
                link = articleLink,
                title = cleanTitle,
                description = cleanDesc,
                pubDate = normalizePubDate(pubDate),
                sourceName = cleanSource,
                sourceUrl = sourceUrl ?: "",
                category = category,
                thumbnailUrl = thumbnailUrl,
                isBookmarked = false,
                customFeedId = customFeedId
            )
        }

        private fun normalizePubDate(rawDate: String?): String {
            if (rawDate.isNullOrBlank()) return ""
            val formats = listOf(
                "EEE, dd MMM yyyy HH:mm:ss z",
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "EEE, dd MMM yyyy HH:mm:ss"
            )
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    val date = sdf.parse(rawDate.trim())
                    if (date != null) {
                        return outputFormat.format(date)
                    }
                } catch (e: Exception) {
                    // try next format
                }
            }
            return rawDate
        }
    }
}
