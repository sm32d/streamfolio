package uk.sume.news.data.network

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import uk.sume.news.data.model.Article
import java.io.StringReader

class RssParser {
    fun parse(xmlContent: String, category: String, customFeedId: Int? = null): List<Article> {
        val articles = mutableListOf<Article>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false // keep namespace check false for simple parsing
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            var currentArticleBuilder: ArticleBuilder? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
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
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) && currentArticleBuilder != null) {
                            val article = currentArticleBuilder.build(category, customFeedId)
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

        fun build(category: String, customFeedId: Int? = null): Article? {
            val articleLink = link ?: return null
            val rawTitle = title ?: "No Title"
            
            // Try to split the title and source if the feed is from Google News (usually "Title - Source")
            var cleanTitle = rawTitle
            var cleanSource = sourceName ?: "Unknown Source"
            
            val lastDashIndex = rawTitle.lastIndexOf(" - ")
            if (lastDashIndex != -1 && lastDashIndex > rawTitle.length - 40) { // source names are usually short
                cleanTitle = rawTitle.substring(0, lastDashIndex).trim()
                if (sourceName == null || sourceName!!.isEmpty()) {
                    cleanSource = rawTitle.substring(lastDashIndex + 3).trim()
                }
            }

            // Stripping HTML from description
            val cleanDesc = description?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""

            return Article(
                link = articleLink,
                title = cleanTitle,
                description = cleanDesc,
                pubDate = pubDate ?: "",
                sourceName = cleanSource,
                sourceUrl = sourceUrl ?: "",
                category = category,
                thumbnailUrl = null, // Will be scraped asynchronously
                isBookmarked = false,
                customFeedId = customFeedId
            )
        }
    }
}
