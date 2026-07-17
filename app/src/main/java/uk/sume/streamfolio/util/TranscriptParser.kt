package uk.sume.streamfolio.util

import java.lang.StringBuilder

data class TranscriptSegment(val startTimeMs: Long, val endTimeMs: Long, val text: String)

object TranscriptParser {
    fun parse(content: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val lines = content.split("\n").map { it.trim() }
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.contains("-->")) {
                val times = line.split("-->").map { it.trim() }
                if (times.size == 2) {
                    val start = parseTime(times[0])
                    val end = parseTime(times[1])
                    
                    val textBuilder = StringBuilder()
                    i++
                    while (i < lines.size && lines[i].isNotEmpty() && !lines[i].contains("-->")) {
                        // Skip SRT sequence numbers if they happen to end up here
                        if (lines[i].toIntOrNull() != null && textBuilder.isEmpty()) {
                            i++
                            continue
                        }
                        if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                        textBuilder.append(lines[i])
                        i++
                    }
                    val text = textBuilder.toString().replace(Regex("<[^>]*>"), "")
                    if (start >= 0 && end >= 0 && text.isNotBlank()) {
                        segments.add(TranscriptSegment(start, end, text))
                    }
                    continue
                }
            }
            i++
        }
        return segments
    }

    private fun parseTime(timeStr: String): Long {
        val clean = timeStr.replace(',', '.')
        val parts = clean.split(":")
        return try {
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secondsParts = parts[2].split(".")
                val seconds = secondsParts[0].toLong()
                val millis = if (secondsParts.size > 1) {
                    secondsParts[1].padEnd(3, '0').substring(0, 3).toLong()
                } else 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
            } else if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val secondsParts = parts[1].split(".")
                val seconds = secondsParts[0].toLong()
                val millis = if (secondsParts.size > 1) {
                    secondsParts[1].padEnd(3, '0').substring(0, 3).toLong()
                } else 0L
                (minutes * 60 + seconds) * 1000 + millis
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
