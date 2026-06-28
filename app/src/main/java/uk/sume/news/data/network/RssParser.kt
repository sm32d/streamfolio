package uk.sume.news.data.network

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import uk.sume.news.data.model.Article
import java.io.StringReader

class RssParser {
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
            var insideChannel = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("channel", ignoreCase = true)) {
                            insideChannel = true
                        } else if (name.equals("item", ignoreCase = true)) {
                            currentArticleBuilder = ArticleBuilder()
                        } else if (currentArticleBuilder != null) {
                            when {
                                name.equals("title", ignoreCase = true) -> {
                                    currentArticleBuilder.title = parser.nextText()
                                }
                                name.equals("link", ignoreCase = true) -> {
                                    currentArticleBuilder.link = parser.nextText()
                                }
                                name.equals("description", ignoreCase = true) -> {
                                    currentArticleBuilder.description = parser.nextText()
                                }
                                name.equals("pubDate", ignoreCase = true) -> {
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
                                name.equals("media:content", ignoreCase = true) || name.equals("content", ignoreCase = true) -> {
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
                        } else if (insideChannel && name.equals("title", ignoreCase = true)) {
                            channelTitle = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("channel", ignoreCase = true)) {
                            insideChannel = false
                        } else if (name.equals("item", ignoreCase = true) && currentArticleBuilder != null) {
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
                cleanTitle = rawTitle.substring(0, lastDashIndex).trim()
                if (sourceName == null || sourceName!!.isEmpty()) {
                    cleanSource = rawTitle.substring(lastDashIndex + 3).trim()
                }
            }

            val cleanDesc = description?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""

            return Article(
                link = articleLink,
                title = cleanTitle,
                description = cleanDesc,
                pubDate = pubDate ?: "",
                sourceName = cleanSource,
                sourceUrl = sourceUrl ?: "",
                category = category,
                thumbnailUrl = thumbnailUrl, // Use XML parsed thumbnail if available!
                isBookmarked = false,
                customFeedId = customFeedId
            )
        }
    }
}
