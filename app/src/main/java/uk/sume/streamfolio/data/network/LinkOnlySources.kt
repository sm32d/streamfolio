package uk.sume.streamfolio.data.network

import uk.sume.streamfolio.data.model.Article
import java.net.URI

/**
 * A publisher whose feed may only be used as a headline-and-link source.
 *
 * Agreed scope (publisher: Joho no Todai): only publication name, headline, publication date,
 * direct article link and the feed's short text `description` are used, cached on the reader's
 * device. No images, no article bodies, no `content:encoded`, no translation, summaries, TTS,
 * offline body caching, redistribution or training. See docs/14-decisions-and-known-limitations.md.
 */
data class LinkOnlySource(
    val id: String,
    val displayName: String,
    /** Host names (and their subdomains) whose feeds and article links are covered. */
    val hosts: List<String>
)

object LinkOnlySources {
    const val JOHO_TODAI_EN_ID = "joho-todai-en"
    const val JOHO_TODAI_EN_FEED = "https://joho-todai.com/en/rss/"

    /** Longest `description` kept for a link-only article (a short text description only). */
    const val MAX_DESCRIPTION_CHARS = 500

    val all: List<LinkOnlySource> = listOf(
        LinkOnlySource(
            id = JOHO_TODAI_EN_ID,
            displayName = "Joho no Todai",
            hosts = listOf("joho-todai.com")
        )
    )

    fun byId(id: String): LinkOnlySource? = all.firstOrNull { it.id == id }

    /** Returns the link-only source that owns [url] (a feed URL or an article link), if any. */
    fun forUrl(url: String?): LinkOnlySource? {
        if (url.isNullOrBlank()) return null
        val host = try {
            URI(url.trim()).host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: return null
        return all.firstOrNull { source ->
            source.hosts.any { h -> host == h || host.endsWith(".$h") }
        }
    }

    fun isLinkOnlyUrl(url: String?): Boolean = forUrl(url) != null

    fun isLinkOnlyArticle(article: Article): Boolean =
        forUrl(article.link) != null || forUrl(article.sourceUrl) != null

    /** SQL `LIKE` patterns matching every stored link / feed URL of [source]. */
    fun likePatterns(source: LinkOnlySource): List<String> = source.hosts.map { "%$it%" }
}
