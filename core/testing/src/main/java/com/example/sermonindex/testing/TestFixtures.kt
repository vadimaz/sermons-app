package com.example.sermonindex.testing

import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.BibleChapter
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.model.TranscriptCue

fun createTestSermon(
    id: String = "test-sermon-1",
    title: String = "The Crucified Life",
    speaker: String = "A.W. Tozer",
    speakerImage: String = "https://example.com/tozer.jpg",
    mp3Url: String = "https://example.com/audio.mp3",
    cdnMp3Url: String = "https://cdn.example.com/audio.mp3",
    vttUrl: String = "https://example.com/transcript.vtt",
    hasAudio: Boolean = true,
    duration: String = "45:00",
    thumbnailUrl: String = "",
    summary: String = "A call to authentic discipleship and holiness.",
    topics: List<Topic> = emptyList(),
    videoUrl: String = "",
    mp4Url: String = "",
    cdnMp4Url: String = "",
    mediaType: String = "AUDIO"
): Sermon = Sermon(
    id = id,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    mp3Url = mp3Url,
    cdnMp3Url = cdnMp3Url,
    vttUrl = vttUrl,
    hasAudio = hasAudio,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    summary = summary,
    topics = topics,
    videoUrl = videoUrl,
    mp4Url = mp4Url,
    cdnMp4Url = cdnMp4Url,
    mediaType = mediaType
)

fun createTestTextSermon(
    id: String = "text-sermon-1",
    title: String = "Morning by Morning",
    speaker: String = "Charles Spurgeon",
    summary: String = "Daily devotionals for meditation."
): Sermon = Sermon(
    id = id,
    title = title,
    speaker = speaker,
    hasAudio = false,
    duration = "Text",
    summary = summary
)

fun createTestSermonDetail(
    id: String = "test-sermon-1",
    title: String = "The Crucified Life",
    speaker: String = "A.W. Tozer",
    speakerSlug: String = "tozer",
    mp3Url: String = "https://example.com/audio.mp3",
    transcript: String = "Full text transcript of the sermon.",
    hasAudio: Boolean = true
): SermonDetail = SermonDetail(
    id = id,
    title = title,
    speaker = speaker,
    speakerSlug = speakerSlug,
    mp3Url = mp3Url,
    transcript = transcript,
    hasAudio = hasAudio
)

fun createTestSpeaker(
    id: String = "spk-1",
    name: String = "A.W. Tozer",
    slug: String = "tozer",
    image: String = "https://example.com/tozer.jpg",
    sermonCount: Int = 120,
    audioCount: Int = 110,
    bio: String = "American Christian pastor and author."
): Speaker = Speaker(
    id = id,
    name = name,
    slug = slug,
    image = image,
    sermonCount = sermonCount,
    audioCount = audioCount,
    bio = bio
)

fun createTestTopic(
    id: String = "top-1",
    name: String = "Prayer",
    slug: String = "prayer",
    count: Int = 350
): Topic = Topic(
    id = id,
    name = name,
    slug = slug,
    count = count
)

fun createTestTopicDetail(
    id: String = "top-1",
    name: String = "Prayer",
    slug: String = "prayer",
    description: String = "Sermons on intercession and private prayer.",
    sermons: List<Sermon> = emptyList()
): TopicDetail = TopicDetail(
    id = id,
    name = name,
    slug = slug,
    description = description,
    sermons = sermons
)

fun createTestFavorite(
    id: String = "test-sermon-1",
    title: String = "The Crucified Life",
    speaker: String = "A.W. Tozer",
    addedAt: Long = System.currentTimeMillis()
): FavoriteSermon = FavoriteSermon(
    id = id,
    title = title,
    speaker = speaker,
    addedAt = addedAt
)

fun createTestHistory(
    sermonId: String = "test-sermon-1",
    title: String = "The Crucified Life",
    speaker: String = "A.W. Tozer",
    speakerImage: String = "",
    lastPositionMs: Long = 30000L,
    durationMs: Long = 120000L,
    updatedAt: Long = System.currentTimeMillis(),
    completed: Boolean = false
): ListeningHistory = ListeningHistory(
    sermonId = sermonId,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    lastPositionMs = lastPositionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
    completed = completed
)

fun createTestDownload(
    id: String = "test-sermon-1",
    title: String = "The Crucified Life",
    speaker: String = "A.W. Tozer",
    localPath: String = "/data/user/0/audio_downloads/test-sermon-1.mp3",
    fileSizeBytes: Long = 15_000_000L,
    downloadedAt: Long = System.currentTimeMillis()
): DownloadedSermon = DownloadedSermon(
    id = id,
    title = title,
    speaker = speaker,
    localPath = localPath,
    fileSizeBytes = fileSizeBytes,
    downloadedAt = downloadedAt
)

fun createTestBibleBook(
    id: String = "GEN",
    name: String = "Genesis",
    testament: String = "OT",
    chapterCount: Int = 50,
    order: Int = 1
): BibleBook = BibleBook(
    id = id,
    name = name,
    testament = testament,
    chapterCount = chapterCount,
    order = order
)

fun createTestBibleTranslation(
    id: String = "BSB",
    name: String = "Berean Standard Bible",
    englishName: String = "Berean Standard Bible",
    shortName: String = "BSB",
    language: String = "en"
): BibleTranslation = BibleTranslation(
    id = id,
    name = name,
    englishName = englishName,
    shortName = shortName,
    language = language
)

fun createTestBibleChapter(
    translationId: String = "BSB",
    bookId: String = "GEN",
    bookName: String = "Genesis",
    chapterNumber: Int = 1,
    verses: List<Pair<Int, String>> = listOf(
        1 to "In the beginning God created the heavens and the earth.",
        2 to "Now the earth was formless and void, and darkness was over the surface of the deep."
    )
): BibleChapter = BibleChapter(
    translationId = translationId,
    bookId = bookId,
    bookName = bookName,
    chapterNumber = chapterNumber,
    numberOfVerses = verses.size,
    items = verses.map { (num, text) -> BibleChapterItem.Verse(number = num, text = text) },
    footnotes = emptyList()
)

fun createTestBibleBookmark(
    id: String = "GEN_1_1",
    translationId: String = "BSB",
    bookId: String = "GEN",
    bookName: String = "Genesis",
    chapter: Int = 1,
    verse: Int = 1,
    text: String = "In the beginning God created the heavens and the earth.",
    color: HighlightColor = HighlightColor.YELLOW,
    createdAt: Long = System.currentTimeMillis()
): BibleBookmark = BibleBookmark(
    id = id,
    translationId = translationId,
    bookId = bookId,
    bookName = bookName,
    chapter = chapter,
    verse = verse,
    text = text,
    color = color,
    createdAt = createdAt
)

fun createTestTranscriptCue(
    index: Int = 0,
    startMs: Long = 0L,
    endMs: Long = 5000L,
    text: String = "Let us turn our Bibles to Genesis."
): TranscriptCue = TranscriptCue(
    index = index,
    startMs = startMs,
    endMs = endMs,
    text = text
)
