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
                "https://feeds.bbci.co.uk/news/world/rss.xml"
            ),
            "Business" to listOf(
                "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10001147",
                "https://feeds.npr.org/1007/rss.xml"
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
            "Science" to listOf(
                "https://www.newscientist.com/feed/",
                "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"
            ),
            "Sports" to listOf(
                "https://feeds.bbci.co.uk/sport/rss.xml"
            ),
            "Health" to listOf(
                "https://www.theguardian.com/lifeandstyle/health-and-wellbeing/rss",
                "https://feeds.bbci.co.uk/news/health/rss.xml"
            ),
            "Entertainment" to listOf(
                "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml"
            )
        ),
        "SG" to mapOf(
            "Top Stories" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=10416",
                "https://www.straitstimes.com/news/singapore/rss.xml",
                "https://www.independent.sg/feed/",
                "https://mothership.sg/feed/"
            ),
            "World" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=6311",
                "https://www.straitstimes.com/news/world/rss.xml"
            ),
            "Business" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=6936",
                "https://www.straitstimes.com/news/business/rss.xml",
                "https://www.businesstimes.com.sg/rss.xml"
            ),
            "Technology" to listOf(
                "https://www.techinasia.com/feed"
            ),
            "Science" to listOf(
                "https://www.sciencedaily.com/rss/all.xml"
            ),
            "Sports" to listOf(
                "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=10296",
                "https://www.straitstimes.com/news/sport/rss.xml"
            ),
            "Health" to listOf(
                "https://www.sciencedaily.com/rss/health_medicine.xml"
            ),
            "Entertainment" to listOf(
                "https://www.8days.sg/api/v1/rss-outbound-feed?_format=xml"
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
                "https://www.livemint.com/rss/markets",
                "https://www.thehindu.com/business/feeder/default.rss",
                "https://economictimes.indiatimes.com/rssfeedstopstories.cms"
            ),
            "Technology" to listOf(
                "https://feeds.feedburner.com/gadgets360-latest",
                "https://feeds.feedburner.com/ndtvnews-latest"
            ),
            "Science" to listOf(
                "https://www.thehindu.com/sci-tech/feeder/default.rss"
            ),
            "Sports" to listOf(
                "https://www.hindustantimes.com/feeds/rss/sports/rssfeed.xml",
                "https://www.livemint.com/rss/sports"
            )
        ),
        "CA" to mapOf(
            "Top Stories" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-topstories",
                "https://www.cbc.ca/webfeed/rss/rss-canada"
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
            "Science" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-discovery"
            ),
            "Sports" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-sports"
            ),
            "Health" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-health"
            ),
            "Entertainment" to listOf(
                "https://www.cbc.ca/webfeed/rss/rss-arts"
            )
        ),
        "AU" to mapOf(
            "Top Stories" to listOf(
                "https://www.abc.net.au/news/feed/51120/rss.xml",
                "https://www.smh.com.au/rss/feed.xml",
                "https://www.theguardian.com/australia-news/rss"
            ),
            "World" to listOf(
                "https://www.smh.com.au/rss/world.xml"
            ),
            "Business" to listOf(
                "https://www.abc.net.au/news/feed/51892/rss.xml",
                "https://www.smh.com.au/rss/business.xml"
            ),
            "Technology" to listOf(
                "https://www.smh.com.au/rss/technology.xml"
            ),
            "Science" to listOf(
                "https://www.abc.net.au/news/feed/51120/rss.xml"
            ),
            "Sports" to listOf(
                "https://www.smh.com.au/rss/sport.xml"
            ),
            "Health" to listOf(
                "https://www.abc.net.au/news/feed/51120/rss.xml"
            ),
            "Entertainment" to listOf(
                "https://www.smh.com.au/rss/entertainment.xml"
            )
        ),
        "FR" to mapOf(
            "Top Stories" to listOf(
                "https://www.france24.com/fr/rss",
                "https://www.lefigaro.fr/rss/figaro_actualites.xml",
                "https://www.lemonde.fr/rss/une.xml"
            ),
            "World" to listOf(
                "https://www.france24.com/fr/monde/rss"
            ),
            "Business" to listOf(
                "https://www.lefigaro.fr/rss/figaro_economie.xml",
                "https://www.lemonde.fr/economie/rss_full.xml"
            ),
            "Science" to listOf(
                "https://www.lemonde.fr/sciences/rss_full.xml",
                "https://www.futura-sciences.com/rss/actualites.xml"
            ),
            "Sports" to listOf(
                "https://www.lemonde.fr/sport/rss_full.xml"
            ),
            "Entertainment" to listOf(
                "https://www.lemonde.fr/culture/rss_full.xml"
            )
        ),
        "DE" to mapOf(
            "Top Stories" to listOf(
                "https://www.tagesschau.de/infoservices/alle-meldungen-100~rdf.xml",
                "https://rss.dw.com/rdf/rss-de-top",
                "https://www.spiegel.de/schlagzeilen/tops/index.rss"
            ),
            "World" to listOf(
                "https://rss.dw.com/rdf/rss-de-all",
                "https://www.faz.net/rss/aktuell/"
            ),
            "Business" to listOf(
                "https://www.tagesschau.de/wirtschaft/index~rdf.xml"
            ),
            "Technology" to listOf(
                "https://www.heise.de/rss/heise.rdf"
            ),
            "Science" to listOf(
                "https://www.dw.com/de/rss"
            ),
            "Sports" to listOf(
                "https://www.dw.com/de/rss"
            ),
            "Health" to listOf(
                "https://www.dw.com/de/rss"
            ),
            "Entertainment" to listOf(
                "https://www.dw.com/de/rss"
            )
        ),
        "HK" to mapOf(
            "Top Stories" to listOf(
                "https://hongkongfp.com/feed/",
                "https://www.scmp.com/rss/91/feed",
                "https://rthk.hk/rthk/news/rss/e_expressnews_elocal.xml",
                "https://www.news.gov.hk/rss/news/topstories_en.xml"
            ),
            "World" to listOf(
                "https://www.scmp.com/rss/5/feed",
                "https://rthk.hk/rthk/news/rss/e_expressnews_einternational.xml"
            ),
            "Business" to listOf(
                "https://www.scmp.com/rss/92/feed",
                "https://rthk.hk/rthk/news/rss/e_expressnews_efinance.xml"
            ),
            "Technology" to listOf(
                "https://www.scmp.com/rss/36/feed",
                "https://fintechnews.hk/feed/"
            ),
            "Sports" to listOf(
                "https://www.scmp.com/rss/95/feed",
                "https://rthk.hk/rthk/news/rss/e_expressnews_esport.xml"
            ),
            "Health" to listOf(
                "https://www.scmp.com/rss/32/feed"
            ),
            "Science" to listOf(
                "https://www.scmp.com/rss/4/feed"
            ),
            "Entertainment" to listOf(
                "https://www.scmp.com/rss/94/feed",
                "https://www.orientalsunday.hk/feed/",
                "https://eastweek.stheadline.com/rss"
            )
        ),
        "KR" to mapOf(
            "Top Stories" to listOf(
                "https://en.yna.co.kr/RSS/news.xml"
            ),
            "Sports" to listOf(
                "https://en.yna.co.kr/RSS/sports.xml"
            )
        ),
        "JP" to mapOf(
            "Top Stories" to listOf(
                "https://www.nhk.or.jp/rss/news/cat0.xml",
                "https://www.japantimes.co.jp/feed/"
            )
        ),
        "BR" to mapOf(
            "Top Stories" to listOf(
                "https://feeds.folha.uol.com.br/emcimadahora/rss091.xml"
            )
        ),
        "ZA" to mapOf(
            "Top Stories" to listOf(
                "https://www.dailymaverick.co.za/rss/",
                "https://www.iol.co.za/rss"
            )
        ),
        "AE" to mapOf(
            "Top Stories" to listOf(
                "https://www.arabnews.com/rss.xml"
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
        val activeFeeds = standardFeeds.filter { !disabledFeedUrls.contains("$regionUpper|$category|$it") }
        
        val manuallyEnabledCrossFeeds = mutableListOf<String>()
        for ((r, categoryMap) in FEEDS) {
            if (r != regionUpper) {
                val urls = categoryMap[category] ?: emptyList()
                for (url in urls) {
                    if (enabledCrossRegionFeeds.contains("$r|$category|$url")) {
                        manuallyEnabledCrossFeeds.add(url)
                    }
                }
            }
        }
        return (activeFeeds + manuallyEnabledCrossFeeds).distinct()
    }

    fun getPublisherName(url: String): String {
        return when {
            url.contains("hongkongfp.com") -> "Hong Kong Free Press"
            url.contains("scmp.com") -> "South China Morning Post"
            url.contains("rthk.hk") -> "RTHK News"
            url.contains("news.gov.hk") -> "GovHK"
            url.contains("fintechnews.hk") -> "Fintech News HK"
            url.contains("orientalsunday.hk") -> "Oriental Sunday"
            url.contains("stheadline.com") -> "East Week"
            url.contains("npr.org") -> "NPR"
            url.contains("apnews.com") || url.contains("ap.xml") -> "Associated Press (AP)"
            url.contains("nytimes.com") -> "The New York Times"
            url.contains("bbc.co.uk") || url.contains("bbc.com") || url.contains("bbci.co.uk") -> "BBC News"
            url.contains("theguardian.com") -> "The Guardian"
            url.contains("cnbc.com") -> "CNBC"
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
            url.contains("8days.sg") -> "8 Days"
            url.contains("independent.sg") -> "The Independent SG"
            url.contains("mothership.sg") -> "Mothership"
            url.contains("businesstimes.com.sg") -> "The Business Times"
            url.contains("straitstimes.com") -> "The Straits Times"
            url.contains("techinasia.com") -> "Tech in Asia"
            url.contains("timesofindia") -> "Times of India"
            url.contains("hindustantimes.com") -> "Hindustan Times"
            url.contains("moneycontrol.com") -> "Moneycontrol"
            url.contains("livemint.com") -> "Livemint"
            url.contains("gadgets360") -> "Gadgets360"
            url.contains("ndtv.com") || url.contains("ndtvnews") -> "NDTV"
            url.contains("thehindu.com") -> "The Hindu"
            url.contains("economictimes.indiatimes.com") -> "The Economic Times"
            url.contains("cbc.ca") -> "CBC News"
            url.contains("thestar.com") -> "Toronto Star"
            url.contains("abc.net.au") -> "ABC News (Australia)"
            url.contains("smh.com.au") -> "Sydney Morning Herald"
            url.contains("france24.com") -> "France 24"
            url.contains("lefigaro.fr") -> "Le Figaro"
            url.contains("lemonde.fr") -> "Le Monde"
            url.contains("futura-sciences.com") -> "Futura Sciences"
            url.contains("tagesschau.de") -> "Tagesschau"
            url.contains("dw.com") -> "Deutsche Welle (DW)"
            url.contains("spiegel.de") -> "Der Spiegel"
            url.contains("faz.net") -> "Frankfurter Allgemeine Zeitung"
            url.contains("heise.de") -> "Heise Online"
            url.contains("newscientist.com") -> "New Scientist"
            url.contains("nature.com") -> "Nature"
            url.contains("yna.co.kr") -> "Yonhap News Agency"
            url.contains("nhk.or.jp") -> "NHK World"
            url.contains("japantimes.co.jp") -> "Japan Times"
            url.contains("folha.uol.com.br") -> "Folha de S.Paulo"
            url.contains("dailymaverick.co.za") -> "Daily Maverick"
            url.contains("iol.co.za") -> "IOL News"
            url.contains("arabnews.com") -> "Arab News"
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
