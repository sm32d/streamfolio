package uk.sume.streamfolio.data.network

object DefaultFeedsConfig {
    private val FEEDS = mapOf(
        "US" to mapOf(
            "Top Stories" to listOf(
                "https://feedx.net/rss/ap.xml",
                "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
                "https://feeds.bbci.co.uk/news/world/rss.xml"
            ),
            "World" to listOf(
                "https://www.theguardian.com/world/rss",
                "http://feeds.bbci.co.uk/news/world/rss.xml"
            ),
            "Business" to listOf(
                "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10001147",
                "http://feeds.marketwatch.com/marketwatch/topstories/"
            ),
            "Technology" to listOf(
                "https://techcrunch.com/feed/",
                "https://www.theverge.com/rss/index.xml"
            ),
            "Science" to listOf(
                "https://www.sciencedaily.com/rss/all.xml",
                "https://www.nasa.gov/news-release/feed/"
            ),
            "Sports" to listOf(
                "https://www.espn.com/espn/rss/news",
                "https://sports.yahoo.com/rss/"
            ),
            "Health" to listOf(
                "https://www.sciencedaily.com/rss/health_medicine.xml",
                "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml"
            ),
            "Entertainment" to listOf(
                "https://www.hollywoodreporter.com/feed/",
                "https://variety.com/feed/"
            )
        ),
        "GB" to mapOf(
            "Top Stories" to listOf(
                "https://feeds.bbci.co.uk/news/rss.xml",
                "https://www.theguardian.com/world/rss"
            ),
            "World" to listOf(
                "https://feeds.bbci.co.uk/news/world/rss.xml",
                "https://www.theguardian.com/world/rss"
            ),
            "Business" to listOf(
                "https://feeds.bbci.co.uk/news/business/rss.xml"
            ),
            "Technology" to listOf(
                "https://feeds.bbci.co.uk/news/technology/rss.xml",
                "https://www.techradar.com/rss"
            ),
            "Sports" to listOf(
                "https://feeds.bbci.co.uk/sport/rss.xml"
            ),
            "Entertainment" to listOf(
                "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml"
            )
        ),
        "SG" to mapOf(
            "Top Stories" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=10416",
                "https://www.straitstimes.com/news/singapore/rss.xml"
            ),
            "World" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=6311",
                "https://www.straitstimes.com/news/world/rss.xml"
            ),
            "Business" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=6936",
                "https://www.straitstimes.com/news/business/rss.xml"
            ),
            "Technology" to listOf(
                "https://www.techinasia.com/feed",
                "https://www.straitstimes.com/news/life/rss.xml"
            ),
            "Sports" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=10296",
                "https://www.straitstimes.com/news/sport/rss.xml"
            ),
            "Health" to listOf(
                "https://www.straitstimes.com/news/life/rss.xml",
                "https://www.sciencedaily.com/rss/health_medicine.xml"
            ),
            "Entertainment" to listOf(
                "https://www.straitstimes.com/news/life/rss.xml"
            )
        ),
        "IN" to mapOf(
            "Top Stories" to listOf(
                "https://timesofindia.indiatimes.com/rssfeeds/296589292.cms",
                "https://www.hindustantimes.com/feeds/rss/news/rssfeed.xml"
            ),
            "World" to listOf(
                "https://www.ndtv.com/rss/world-news",
                "https://www.hindustantimes.com/feeds/rss/news/rssfeed.xml"
            ),
            "Business" to listOf(
                "https://www.moneycontrol.com/rss/latestnews.xml"
            ),
            "Technology" to listOf(
                "https://feeds.feedburner.com/gadgets360-latest"
            ),
            "Sports" to listOf(
                "https://www.ndtv.com/rss/sports",
                "https://www.hindustantimes.com/feeds/rss/news/rssfeed.xml"
            )
        ),
        "CA" to mapOf(
            "Top Stories" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-topstories"
            ),
            "World" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-world"
            ),
            "Business" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-business"
            ),
            "Technology" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-technology"
            ),
            "Sports" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-sports"
            )
        ),
        "AU" to mapOf(
            "Top Stories" to listOf(
                "https://www.abc.net.au/news/feed/51120/rss.xml",
                "https://www.smh.com.au/rss/feed.xml"
            ),
            "Business" to listOf(
                "https://www.abc.net.au/news/feed/51892/rss.xml"
            ),
            "Sports" to listOf(
                "https://www.smh.com.au/rss/sport.xml"
            )
        ),
        "FR" to mapOf(
            "Top Stories" to listOf(
                "https://www.france24.com/fr/rss",
                "https://www.lefigaro.fr/rss/figaro_actualites.xml"
            ),
            "World" to listOf(
                "https://www.france24.com/fr/monde/rss"
            ),
            "Business" to listOf(
                "https://www.lefigaro.fr/rss/figaro_economie.xml"
            )
        ),
        "DE" to mapOf(
            "Top Stories" to listOf(
                "https://www.tagesschau.de/infoservices/alle-meldungen-100~rdf.xml",
                "https://rss.dw.com/rdf/rss-de-top"
            ),
            "World" to listOf(
                "https://rss.dw.com/rdf/rss-de-all"
            ),
            "Business" to listOf(
                "https://www.tagesschau.de/wirtschaft/index~rdf.xml"
            )
        )
    )

    fun getFeedsFor(region: String, category: String): List<String> {
        val regionUpper = region.uppercase()
        val regionMap = FEEDS[regionUpper] ?: FEEDS["US"]!!
        val feeds = regionMap[category]
        if (feeds.isNullOrEmpty()) {
            return FEEDS["US"]?.get(category) ?: emptyList()
        }
        return feeds
    }
}
