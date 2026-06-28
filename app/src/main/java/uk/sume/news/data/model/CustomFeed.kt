package uk.sume.news.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_feeds")
data class CustomFeed(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val url: String,
    val category: String
)
