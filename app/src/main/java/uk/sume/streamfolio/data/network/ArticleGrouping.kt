package uk.sume.streamfolio.data.network

import uk.sume.streamfolio.data.model.Article

/**
 * Pure title-similarity clustering used to link articles covering the same story across
 * different publishers. Extracted from [NewsRepository] so the Jaccard-similarity math can be
 * unit-tested without a Room database/Context (the DB lookup for cross-refresh continuity stays
 * in [NewsRepository.assignGroupIds], which fetches `existingArticles` and delegates here).
 */
object ArticleGrouping {

    private val punctuationRegex = Regex("[^\\p{L}\\p{N}\\s]")
    private val whitespaceRegex = Regex("\\s+")
    private val stopwordsSet = setOf(
        "a", "an", "the", "in", "on", "at", "by", "of", "to", "for", "with",
        "and", "but", "or", "is", "are", "was", "were", "has", "have", "had",
        "that", "this", "these", "those", "will", "would", "could", "should",
        "after", "before", "about", "from", "into", "over", "under", "been",
        "says", "said", "reporting", "reports", "latest", "update", "live", "news"
    )
    private val sourceSuffixesList = listOf(
        "bbc news", "bbc", "reuters", "cnn", "ap", "ap news", "the guardian",
        "new york times", "nytimes", "washington post", "wapo", "fox news",
        "cna", "the straits times", "straits times", "bloomberg"
    )

    /** Similarity threshold above which two article titles are considered the same story. */
    const val SIMILARITY_THRESHOLD = 0.35f

    fun cleanTitle(title: String): String {
        var cleaned = title.lowercase().trim()
        cleaned = punctuationRegex.replace(cleaned, " ")
        for (suffix in sourceSuffixesList) {
            if (cleaned.endsWith(suffix)) cleaned = cleaned.removeSuffix(suffix).trim()
            if (cleaned.startsWith(suffix)) cleaned = cleaned.removePrefix(suffix).trim()
        }
        val words = whitespaceRegex.split(cleaned)
            .map { it.trim() }
            .filter { it.length > 2 && !stopwordsSet.contains(it) }
        return words.joinToString(" ")
    }

    fun jaccardSimilarity(words1: Set<String>, words2: Set<String>): Float {
        if (words1.isEmpty() && words2.isEmpty()) return 1.0f
        if (words1.isEmpty() || words2.isEmpty()) return 0.0f
        val intersectionSize = words1.intersect(words2).size
        val unionSize = words1.union(words2).size
        return intersectionSize.toFloat() / unionSize.toFloat()
    }

    private fun wordSetOf(title: String): Set<String> =
        cleanTitle(title).split(" ").filter { it.isNotBlank() }.toSet()

    /**
     * Assigns a stable `groupId` to every article in [newArticles], merging into an existing
     * story cluster (from [existingArticles], e.g. previously stored articles in the same
     * category) or another article within [newArticles] when title similarity is
     * >= [SIMILARITY_THRESHOLD]. Articles that already carry a `groupId` keep it.
     */
    fun assignGroupIds(existingArticles: List<Article>, newArticles: List<Article>): List<Article> {
        if (newArticles.isEmpty()) return newArticles

        val existingGroups = mutableListOf<MutableList<Article>>()
        val existingWordSets = mutableListOf<Set<String>>()

        for (existing in existingArticles) {
            val wordSet = wordSetOf(existing.title)
            var placed = false
            for (i in existingGroups.indices) {
                if (jaccardSimilarity(existingWordSets[i], wordSet) >= SIMILARITY_THRESHOLD) {
                    existingGroups[i].add(existing)
                    placed = true
                    break
                }
            }
            if (!placed) {
                existingGroups.add(mutableListOf(existing))
                existingWordSets.add(wordSet)
            }
        }

        return newArticles.map { article ->
            val wordSet = wordSetOf(article.title)
            var matchedGroupId: String? = article.groupId

            if (matchedGroupId == null) {
                for (i in existingGroups.indices) {
                    if (jaccardSimilarity(existingWordSets[i], wordSet) >= SIMILARITY_THRESHOLD) {
                        val groupLeader = existingGroups[i][0]
                        matchedGroupId = groupLeader.groupId ?: groupLeader.link
                        existingGroups[i].add(article)
                        break
                    }
                }
            }

            if (matchedGroupId == null) {
                matchedGroupId = article.link
                existingGroups.add(mutableListOf(article))
                existingWordSets.add(wordSet)
            }

            article.copy(groupId = matchedGroupId)
        }
    }
}
