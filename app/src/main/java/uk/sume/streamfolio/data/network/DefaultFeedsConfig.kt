package uk.sume.streamfolio.data.network

data class CuratedProvider(
    val region: String,
    val category: String,
    val publisherName: String,
    val url: String
)

object DefaultFeedsConfig {
    private val FEEDS = mapOf(
        "US" to mapOf(
            "Top Stories" to listOf(
                "https://feeds.npr.org/1001/rss.xml",
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
                "https://www.cbssports.com/rss/headlines/",
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
                "https://www.techinasia.com/feed"
            ),
            "Sports" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=10296",
                "https://www.straitstimes.com/news/sport/rss.xml"
            ),
            "Health" to listOf(
                "https://www.sciencedaily.com/rss/health_medicine.xml"
            )
        ),
        "IN" to mapOf(
            "Top Stories" to listOf(
                "https://timesofindia.indiatimes.com/rssfeedstopstories.cms",
                "https://www.hindustantimes.com/feeds/rss/india-news/rssfeed.xml"
            ),
            "World" to listOf(
                "https://timesofindia.indiatimes.com/rssfeeds/296589292.cms",
                "https://www.hindustantimes.com/feeds/rss/world-news/rssfeed.xml"
            ),
            "Business" to listOf(
                "https://www.livemint.com/rss/markets"
            ),
            "Technology" to listOf(
                "https://feeds.feedburner.com/gadgets360-latest"
            ),
            "Sports" to listOf(
                "https://www.hindustantimes.com/feeds/rss/sports/rssfeed.xml",
                "https://www.livemint.com/rss/sports"
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

    fun getFeedsFor(
        region: String,
        category: String,
        disabledFeedUrls: Set<String> = emptySet(),
        enabledCrossRegionFeeds: Set<String> = emptySet()
    ): List<String> {
        val regionUpper = region.uppercase()
        val regionMap = FEEDS[regionUpper] ?: FEEDS["US"]!!
        val standardFeeds = regionMap[category] ?: emptyList()
        val activeFeeds = standardFeeds.filter { !disabledFeedUrls.contains("$category|$it") }
        
        val manuallyEnabledCrossFeeds = mutableListOf<String>()
        for ((r, categoryMap) in FEEDS) {
            if (r != regionUpper) {
                val urls = categoryMap[category] ?: emptyList()
                for (url in urls) {
                    if (enabledCrossRegionFeeds.contains("$category|$url")) {
                        manuallyEnabledCrossFeeds.add(url)
                    }
                }
            }
        }
        return (activeFeeds + manuallyEnabledCrossFeeds).distinct()
    }

    fun getPublisherName(url: String): String {
        return when {
            url.contains("npr.org") -> "NPR"
            url.contains("apnews.com") || url.contains("ap.xml") -> "Associated Press (AP)"
            url.contains("nytimes.com") -> "The New York Times"
            url.contains("bbc.co.uk") || url.contains("bbc.com") || url.contains("bbci.co.uk") -> "BBC News"
            url.contains("theguardian.com") -> "The Guardian"
            url.contains("cnbc.com") -> "CNBC"
            url.contains("marketwatch.com") -> "MarketWatch"
            url.contains("techcrunch.com") -> "TechCrunch"
            url.contains("theverge.com") -> "The Verge"
            url.contains("sciencedaily.com") -> "ScienceDaily"
            url.contains("nasa.gov") -> "NASA"
            url.contains("espn.com") -> "ESPN"
            url.contains("yahoo.com") -> "Yahoo Sports"
            url.contains("cbssports.com") -> "CBS Sports"
            url.contains("hollywoodreporter.com") -> "The Hollywood Reporter"
            url.contains("variety.com") -> "Variety"
            url.contains("channelnewsasia.com") -> "CNA"
            url.contains("straitstimes.com") -> "The Straits Times"
            url.contains("techinasia.com") -> "Tech in Asia"
            url.contains("timesofindia") -> "Times of India"
            url.contains("hindustantimes.com") -> "Hindustan Times"
            url.contains("moneycontrol.com") -> "Moneycontrol"
            url.contains("livemint.com") -> "Livemint"
            url.contains("gadgets360") -> "Gadgets360"
            url.contains("ndtv.com") -> "NDTV"
            url.contains("cbc.ca") -> "CBC News"
            url.contains("thestar.com") -> "Toronto Star"
            url.contains("abc.net.au") -> "ABC News"
            url.contains("smh.com.au") -> "Sydney Morning Herald"
            url.contains("france24.com") -> "France 24"
            url.contains("lefigaro.fr") -> "Le Figaro"
            url.contains("tagesschau.de") -> "Tagesschau"
            url.contains("dw.com") -> "Deutsche Welle (DW)"
            else -> "Unknown Source"
        }
    }

    fun getAllCuratedProviders(): List<CuratedProvider> {
        val providers = mutableListOf<CuratedProvider>()
        for ((region, categoriesMap) in FEEDS) {
            for ((category, urls) in categoriesMap) {
                for (url in urls) {
                    providers.add(
                        CuratedProvider(
                            region = region,
                            category = category,
                            publisherName = getPublisherName(url),
                            url = url
                        )
                    )
                }
            }
        }
        return providers
    }
}
