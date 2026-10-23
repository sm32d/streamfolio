package uk.sume.streamfolio.util

import android.net.Uri

/**
 * Builds and parses StreamFolio share links.
 *
 * A shared article looks like `https://streamfolio.sume.uk/open?url=<encoded article url>`.
 * Devices with the app installed open it in StreamFolio (Android App Links); everyone else
 * lands on `landing/open.html`, which offers "Open in StreamFolio" / "Open in browser".
 */
object ShareLinks {
    const val HOST = "streamfolio.sume.uk"
    const val OPEN_PATH = "/open"
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=uk.sume.streamfolio"

    fun buildShareText(title: String, articleLink: String): String {
        val articleUrl = UrlSecurityValidator.normalizeToHttps(articleLink) ?: articleLink
        val shareLink = Uri.Builder()
            .scheme("https")
            .authority(HOST)
            .path(OPEN_PATH)
            .appendQueryParameter("url", articleUrl)
            .build()
        return "$title\n\nRead more: $shareLink\n\nGet StreamFolio: $PLAY_STORE_URL"
    }

    /** Returns the safe https article URL carried by a share link, or null if [data] isn't one. */
    fun parseArticleUrl(data: Uri?): String? {
        if (data == null) return null
        if (data.scheme != "https" || !HOST.equals(data.host, ignoreCase = true)) return null
        if (data.path?.startsWith(OPEN_PATH) != true) return null
        val target = data.getQueryParameter("url") ?: return null
        return UrlSecurityValidator.normalizeToHttps(target)
    }
}
