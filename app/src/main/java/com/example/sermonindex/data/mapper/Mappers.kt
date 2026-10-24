package com.example.sermonindex.data.mapper

import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.FtsSermonRecord
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.local.entities.SermonFtsEntity
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
import com.example.sermonindex.domain.model.OutlineItem
import com.example.sermonindex.domain.model.QuoteItem
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.data.model.AvailableTranslationsResponse
import com.example.sermonindex.data.model.BibleChapterResponse
import com.example.sermonindex.data.model.BibleTranslationDto
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.TopicScripture
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

fun SermonDto.toDomain(): Sermon = Sermon(
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
    topics = topics.map { Topic(id = it.slug, name = it.name, slug = it.slug, count = 0) },
    videoUrl = videoUrl,
    mp4Url = mp4Url,
    cdnMp4Url = cdnMp4Url,
    mediaType = mediaType
)

fun SermonDetailDto.toDomain(): SermonDetail = SermonDetail(
    id = id,
    title = title,
    speaker = speaker,
    speakerImage = speakerImage,
    speakerSlug = speakerSlug,
    mp3Url = mp3Url,
    cdnMp3Url = cdnMp3Url,
    vttUrl = vttUrl,
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
    transcriptHtml = transcriptHtml,
    videoUrl = videoUrl,
    mp4Url = mp4Url,
    cdnMp4Url = cdnMp4Url,
    mediaType = mediaType
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

fun SermonFtsEntity.toDomain(): Sermon = Sermon(
    id = sermonId,
    title = title,
    speaker = speaker,
    summary = summary,
    duration = duration,
    speakerImage = speakerImage,
    hasAudio = hasAudio == 1,
    mp3Url = mp3Url,
    cdnMp3Url = cdnMp3Url
)

fun FtsSermonRecord.toEntity(): SermonFtsEntity = SermonFtsEntity(
    sermonId = id,
    title = title,
    speaker = speaker,
    summary = summary,
    duration = duration,
    speakerImage = speakerImage,
    hasAudio = if (hasAudio) 1 else 0,
    mp3Url = mp3Url,
    cdnMp3Url = cdnMp3Url
)

fun BibleTranslationDto.toDomain(): BibleTranslation = BibleTranslation(
    id = id,
    name = name.ifBlank { englishName },
    englishName = englishName,
    shortName = shortName.ifBlank { id },
    language = language,
    languageName = languageName,
    languageEnglishName = languageEnglishName,
    numberOfBooks = numberOfBooks
)

fun com.example.sermonindex.data.model.BibleTranslationBookDto.toDomain(): BibleBook = BibleBook(
    id = id,
    name = name.ifBlank { commonName.ifBlank { title } },
    testament = if (order <= 39) "OT" else "NT",
    chapterCount = numberOfChapters,
    order = order
)

fun com.example.sermonindex.data.model.BibleFootnoteDto.toDomain(): com.example.sermonindex.domain.model.BibleFootnote =
    com.example.sermonindex.domain.model.BibleFootnote(
        noteId = noteId,
        caller = caller,
        text = text,
        chapter = reference?.chapter ?: 0,
        verse = reference?.verse ?: 0
    )

fun BibleChapterResponse.toDomain(
    translationId: String,
    bookId: String,
    bookName: String,
    chapterNumber: Int
): com.example.sermonindex.domain.model.BibleChapter {
    val items = mutableListOf<com.example.sermonindex.domain.model.BibleChapterItem>()
    val contentArray = chapter?.content ?: emptyList()
    for (item in contentArray) {
        val obj = item as? JsonObject ?: continue
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
        when (type) {
            "heading" -> {
                val h = obj["content"]?.jsonArray?.joinToString(" ") {
                    if (it is JsonPrimitive) it.content else it.toString()
                } ?: ""
                if (h.isNotBlank()) items.add(com.example.sermonindex.domain.model.BibleChapterItem.Heading(h))
            }
            "hebrew_subtitle" -> {
                val s = obj["content"]?.jsonArray?.joinToString(" ") {
                    if (it is JsonPrimitive) it.content else it.toString()
                } ?: ""
                if (s.isNotBlank()) items.add(com.example.sermonindex.domain.model.BibleChapterItem.HebrewSubtitle(s))
            }
            "line_break" -> {
                items.add(com.example.sermonindex.domain.model.BibleChapterItem.LineBreak)
            }
            "verse" -> {
                val num = obj["number"]?.jsonPrimitive?.intOrNull ?: 0
                val verseContent = obj["content"]?.jsonArray
                val textParts = mutableListOf<String>()
                val noteIds = mutableListOf<Int>()
                if (verseContent != null) {
                    for (part in verseContent) {
                        if (part is JsonPrimitive && part.isString) {
                            val text = part.content.trim()
                            if (text.isNotEmpty()) textParts.add(text)
                        } else if (part is JsonObject) {
                            val noteId = part["noteId"]?.jsonPrimitive?.intOrNull
                            if (noteId != null) {
                                noteIds.add(noteId)
                            }
                            val text = part["text"]?.jsonPrimitive?.contentOrNull?.trim()
                            if (!text.isNullOrEmpty()) {
                                textParts.add(text)
                            }
                        }
                    }
                }
                val fullText = textParts.joinToString(" ").replace("\\s+".toRegex(), " ")
                items.add(com.example.sermonindex.domain.model.BibleChapterItem.Verse(number = num, text = fullText, footnoteIds = noteIds))
            }
        }
    }

    val allFootnotes = footnotes.ifEmpty { chapter?.footnotes ?: emptyList() }
    val mappedFootnotes = allFootnotes.map { it.toDomain() }
    val resolvedBookName = book?.name?.ifBlank { bookName } ?: bookName

    return com.example.sermonindex.domain.model.BibleChapter(
        translationId = translationId,
        bookId = bookId,
        bookName = resolvedBookName,
        chapterNumber = chapterNumber,
        numberOfVerses = numberOfVerses,
        items = items,
        footnotes = mappedFootnotes,
        audioLinks = thisChapterAudioLinks,
        hasNextChapter = nextChapterApiLink != null,
        hasPreviousChapter = previousChapterApiLink != null
    )
}

fun BibleChapterResponse.extractVerseMap(): Map<Int, String> {
    val items = chapter?.content ?: return emptyMap()
    val verseMap = mutableMapOf<Int, String>()
    for (item in items) {
        val obj = item as? JsonObject ?: continue
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
        val number = obj["number"]?.jsonPrimitive?.intOrNull
        if (type == "verse" && number != null) {
            val contentArray = obj["content"]?.jsonArray ?: continue
            val parts = mutableListOf<String>()
            for (part in contentArray) {
                if (part is JsonPrimitive && part.isString) {
                    val s = part.content.trim()
                    if (s.isNotEmpty()) parts.add(s)
                } else if (part is JsonObject) {
                    val text = part["text"]?.jsonPrimitive?.contentOrNull?.trim()
                    if (!text.isNullOrEmpty()) parts.add(text)
                }
            }
            if (parts.isNotEmpty()) {
                verseMap[number] = parts.joinToString(" ").replace("\\s+".toRegex(), " ")
            }
        }
    }
    return verseMap
}

fun com.example.sermonindex.data.model.ApiStatsDto.toDomain(): com.example.sermonindex.domain.model.ApiStats =
    com.example.sermonindex.domain.model.ApiStats(
        sermonsCount = sermons,
        speakersCount = speakers,
        topicsCount = topics,
        generatedAt = generated
    )


