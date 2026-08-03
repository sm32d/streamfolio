package uk.sume.streamfolio.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmark_lists",
    indices = [Index(value = ["name"], unique = true)]
)
data class BookmarkList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "article_bookmark_lists",
    primaryKeys = ["articleLink", "listId"],
    indices = [
        Index(value = ["articleLink"]),
        Index(value = ["listId"])
    ]
)
data class ArticleBookmarkListCrossRef(
    val articleLink: String,
    val listId: Long
)

data class BookmarkListWithArticles(
    val list: BookmarkList,
    val articleCount: Int,
    val previewThumbnail: String? = null
)
