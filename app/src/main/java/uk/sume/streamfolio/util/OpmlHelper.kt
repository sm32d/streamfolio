package uk.sume.streamfolio.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import uk.sume.streamfolio.data.model.CustomFeed
import java.io.InputStream
import java.util.Stack

data class OpmlFeed(
    val title: String,
    val xmlUrl: String,
    val category: String
)

object OpmlHelper {

    fun parseOpml(inputStream: InputStream): List<OpmlFeed> {
        val feeds = mutableListOf<OpmlFeed>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        val folderStack = Stack<String>()
        val outlineStack = Stack<Boolean>() // true for category/folder outline, false for feed outline

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("outline", ignoreCase = true)) {
                        val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                            ?: parser.getAttributeValue(null, "xmlurl")
                        val text = parser.getAttributeValue(null, "text")
                            ?: parser.getAttributeValue(null, "title")
                            ?: "Untitled Feed"

                        if (xmlUrl != null) {
                            // This outline represents a feed item
                            outlineStack.push(false)
                            val currentCategory = if (folderStack.isNotEmpty()) folderStack.peek() else "Imported"
                            feeds.add(OpmlFeed(text.trim(), xmlUrl.trim(), currentCategory.trim()))
                        } else {
                            // This outline represents a folder/category
                            outlineStack.push(true)
                            folderStack.push(text.trim())
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("outline", ignoreCase = true)) {
                        if (outlineStack.isNotEmpty()) {
                            val isFolder = outlineStack.pop()
                            if (isFolder && folderStack.isNotEmpty()) {
                                folderStack.pop()
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return feeds
    }

    fun exportFeedsToOpml(feeds: List<CustomFeed>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head>\n")
        sb.append("    <title>StreamFolio Subscriptions</title>\n")
        sb.append("  </head>\n")
        sb.append("  <body>\n")

        val grouped = feeds.groupBy { it.category }
        for ((category, categoryFeeds) in grouped) {
            val escapedCat = escapeXml(category)
            sb.append("    <outline text=\"$escapedCat\" title=\"$escapedCat\">\n")
            for (feed in categoryFeeds) {
                val escapedTitle = escapeXml(feed.title)
                val escapedUrl = escapeXml(feed.url)
                sb.append("      <outline type=\"rss\" text=\"$escapedTitle\" title=\"$escapedTitle\" xmlUrl=\"$escapedUrl\"/>\n")
            }
            sb.append("    </outline>\n")
        }

        sb.append("  </body>\n")
        sb.append("</opml>\n")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
