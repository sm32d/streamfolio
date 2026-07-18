package uk.sume.streamfolio.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted snapshot of the TTS playlist queue.
 *
 * Only a single row (id = 1) is used. [linksJson] stores a JSON array of
 * article links in queue order. The actual article data is still held in the
 * [Article] table; on restore we look up each link and drop any that no longer
 * exist.
 */
@Entity(tableName = "tts_playlist_state")
data class TtsPlaylistState(
    @PrimaryKey
    val id: Int = 1,
    val currentIndex: Int = -1,
    val linksJson: String = "[]"
)
