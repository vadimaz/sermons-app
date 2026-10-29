package com.example.sermonindex.ui.components

import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.OutlineItem
import com.example.sermonindex.domain.model.QuoteItem
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic

/**
 * Shared preview mock models used across Compose @Preview functions.
 */
object PreviewFixtures {
    val previewAudioSermon = Sermon(
        id = "sermon_1",
        title = "Why Revival Tarries",
        speaker = "Leonard Ravenhill",
        speakerImage = "https://example.com/ravenhill.jpg",
        mp3Url = "https://example.com/revival.mp3",
        hasAudio = true,
        duration = "42:15",
        summary = "A passionate call to prayer, brokenness, and spiritual urgency in the church.",
        topics = listOf(
            Topic(id = "1", name = "Revival", slug = "revival", count = 120),
            Topic(id = "2", name = "Prayer", slug = "prayer", count = 95)
        )
    )

    val previewTextSermon = Sermon(
        id = "sermon_text_1",
        title = "Faith and Works: Justification Explained",
        speaker = "Charles H. Spurgeon",
        speakerImage = "https://example.com/spurgeon.jpg",
        mp3Url = "",
        cdnMp3Url = "",
        hasAudio = false,
        duration = "",
        summary = "Spurgeon's classic exposition on James 2 and Romans 4 exploring true saving faith.",
        topics = listOf(
            Topic(id = "3", name = "Faith", slug = "faith", count = 80)
        )
    )

    val previewSpeaker = Speaker(
        id = "spk_1",
        name = "A.W. Tozer",
        slug = "aw-tozer",
        image = "https://example.com/tozer.jpg",
        sermonCount = 142,
        audioCount = 138,
        bio = "Aidan Wilson Tozer was an American Christian pastor, author, magazine editor, and spiritual mentor."
    )

    val previewTopic = Topic(
        id = "top_1",
        name = "Prayer & Fasting",
        slug = "prayer-and-fasting",
        count = 68
    )

    val previewListeningHistory = ListeningHistory(
        sermonId = "sermon_hist_1",
        title = "The Pursuit of God",
        speaker = "A.W. Tozer",
        speakerImage = "https://example.com/tozer.jpg",
        lastPositionMs = 18 * 60 * 1000L,
        durationMs = 35 * 60 * 1000L,
        updatedAt = System.currentTimeMillis() - 3600_000L,
        completed = false
    )

    val previewDownloadedSermon = DownloadedSermon(
        id = "sermon_dl_1",
        title = "The Crucified Life",
        speaker = "A.W. Tozer",
        speakerImage = "https://example.com/tozer.jpg",
        duration = "38:40",
        localPath = "/data/user/0/com.example.sermonindex/files/audio_downloads/sermon_dl_1.mp3",
        fileSizeBytes = 28_400_000L,
        downloadedAt = System.currentTimeMillis() - 86400_000L
    )

    val previewSermonDetail = SermonDetail(
        id = "sermon_1",
        title = "Why Revival Tarries",
        speaker = "Leonard Ravenhill",
        speakerImage = "https://example.com/ravenhill.jpg",
        speakerSlug = "leonard-ravenhill",
        mp3Url = "https://example.com/revival.mp3",
        hasAudio = true,
        duration = "42:15",
        summary = "A passionate call to prayer, brokenness, and spiritual urgency in the church.",
        description = "Delivered in 1968, this message remains one of Ravenhill's most burning exhortations to the modern church regarding spiritual lethargy and the desperate need for holy prayer meetings.",
        scriptureCitations = listOf("2 Chronicles 7:14", "Isaiah 64:1", "James 5:16"),
        outline = listOf(
            OutlineItem(timestamp = "00:00", timeSeconds = 0, title = "The tragedy of unanswered prayer"),
            OutlineItem(timestamp = "12:30", timeSeconds = 750, title = "Why God holds back revival"),
            OutlineItem(timestamp = "28:15", timeSeconds = 1695, title = "The price of genuine awakening")
        ),
        quotes = listOf(
            QuoteItem(quote = "No man is greater than his prayer life.", topic = "Prayer"),
            QuoteItem(quote = "A popular church loves plenty of people and plenty of noise; a spiritual church loves God and hates sin.", topic = "Revival")
        ),
        applicationPoints = listOf(
            "Examine your secret prayer closet before seeking public ministry.",
            "Plead with God for revival with persistent intercession."
        )
    )

    val previewBibleBookmark = BibleBookmark(
        id = "bm_1",
        translationId = "rus_syn",
        bookId = "PSA",
        bookName = "Псалтирь",
        chapter = 23,
        verse = 1,
        text = "Господь — Пастырь мой; я ни в чем не буду нуждаться.",
        color = HighlightColor.YELLOW
    )
}
