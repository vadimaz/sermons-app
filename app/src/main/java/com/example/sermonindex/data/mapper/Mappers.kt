package com.example.sermonindex.data.mapper

import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.FtsSermonResult
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.model.FaqItemDto
import com.example.sermonindex.data.model.OutlineItemDto
import com.example.sermonindex.data.model.ScriptureBookDto
import com.example.sermonindex.data.model.ScriptureChapterDto
import com.example.sermonindex.data.model.SermonDetailDto
import com.example.sermonindex.data.model.SermonDto
import com.example.sermonindex.data.model.SpeakerDto
import com.example.sermonindex.data.model.SpeakerPagedDto
import com.example.sermonindex.data.model.TopicDetailDto
import com.example.sermonindex.data.model.TopicDto
import com.example.sermonindex.data.model.TopicScriptureDto
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.FaqItem
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.OfflineSearchResult
import com.example.sermonindex.domain.model.OutlineItem
import com.example.sermonindex.domain.model.QuoteItem
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.model.TopicScripture

fun SermonDto.toDomain(): Sermon = Sermon(
    id = id,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    mp3Url = mp3Url,
    cdnMp3Url = cdnMp3Url,
    hasAudio = hasAudio,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    summary = summary,
    topics = topics.map { Topic(id = it.slug, name = it.name, slug = it.slug, count = 0) }
)

fun SermonDetailDto.toDomain(): SermonDetail = SermonDetail(
    id = id,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    speakerSlug = speakerSlug,
    mp3Url = mp3Url,
    cdnMp3Url = cdnMp3Url,
    hasAudio = hasAudio,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    summary = summary,
    description = description,
    scriptureCitations = scripture.mapNotNull { it.text.takeIf { t -> t.isNotBlank() } },
    outline = outline.map { it.toDomain() },
    quotes = quotes.map { QuoteItem(quote = it, topic = "") },
    applicationPoints = applicationPoints,
    faq = faq.map { it.toDomain() },
    transcript = transcript,
    transcriptHtml = transcriptHtml
)

fun OutlineItemDto.toDomain(): OutlineItem = OutlineItem(
    timestamp = "",
    timeSeconds = 0L,
    title = heading
)

fun FaqItemDto.toDomain(): FaqItem = FaqItem(
    question = q,
    answer = a
)

fun SpeakerDto.toDomain(): Speaker = Speaker(
    id = slug,
    name = name,
    slug = slug,
    image = image,
    sermonCount = sermonCount,
    audioCount = audioCount,
    bio = bio,
    totalPages = totalPages.coerceAtLeast(1),
    topTopics = topTopics.map { Topic(id = it.slug, name = it.name, slug = it.slug, count = it.count) }
)

fun SpeakerPagedDto.toDomain(): SpeakerPagedSermons = SpeakerPagedSermons(
    sermons = sermons.map { it.toDomain() },
    currentPage = page,
    totalPages = totalPages.coerceAtLeast(1),
    totalSermons = total
)

fun TopicDto.toDomain(): Topic = Topic(
    id = slug,
    name = name,
    slug = slug,
    count = sermonCount
)

fun TopicScriptureDto.toDomain(): TopicScripture = TopicScripture(
    text = text,
    bookId = bookId,
    chapter = chapter,
    verse = verse,
    count = count,
    bsb = bsb
)

fun TopicDetailDto.toDomain(): TopicDetail = TopicDetail(
    id = slug,
    name = name,
    slug = slug,
    description = description,
    topScriptures = topScriptures.map { it.toDomain() },
    sermons = sermons.map { it.toDomain() }
)

fun ScriptureBookDto.toDomain(): BibleBook = BibleBook(
    id = bookId,
    name = name,
    testament = testament,
    chapterCount = chapters
)

fun ScriptureChapterDto.toDomain(): ChapterDetails {
    val heatMap = verses.associate { it.verse to it.sermonCount }
    return ChapterDetails(
        bookId = bookId,
        bookName = book,
        chapter = chapter,
        verseCount = verses.size,
        verseHeatMap = heatMap
    )
}

fun FavoriteSermonEntity.toDomain(): FavoriteSermon = FavoriteSermon(
    id = id,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    duration = duration,
    addedAt = addedAt
)

fun HistoryEntity.toDomain(): ListeningHistory = ListeningHistory(
    sermonId = sermonId,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    lastPositionMs = lastPositionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
    completed = completed
)

fun FtsSermonResult.toDomain(): OfflineSearchResult = OfflineSearchResult(
    sermonId = id,
    title = title,
    speaker = speaker,
    summary = summary
)

fun Sermon.toFavoriteEntity(): FavoriteSermonEntity = FavoriteSermonEntity(
    id = id,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    duration = duration,
    addedAt = System.currentTimeMillis()
)

fun com.example.sermonindex.data.local.entities.DownloadedSermonEntity.toDomain(): com.example.sermonindex.domain.model.DownloadedSermon =
    com.example.sermonindex.domain.model.DownloadedSermon(
        id = id,
        title = title,
        speaker = speaker,
        speakerImage = speakerImage,
        duration = duration,
        localPath = localPath,
        fileSizeBytes = fileSizeBytes,
        downloadedAt = downloadedAt
    )

fun com.example.sermonindex.domain.model.DownloadedSermon.toEntity(): com.example.sermonindex.data.local.entities.DownloadedSermonEntity =
    com.example.sermonindex.data.local.entities.DownloadedSermonEntity(
        id = id,
        title = title,
        speaker = speaker,
        speakerImage = speakerImage,
        duration = duration,
        localPath = localPath,
        fileSizeBytes = fileSizeBytes,
        downloadedAt = downloadedAt
    )

