package com.example.sermonindex.data.mapper

import com.example.sermonindex.data.model.RandomShardResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomSermonMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun randomShardJson_deserializesAndMapsToDomainCorrectly() {
        val rawJson = """
            {
                "shard": 1,
                "count": 1,
                "sermons": [
                    {
                        "i": "tphPJyH4SC3Gl1WO",
                        "t": "Suffering - Part 2",
                        "u": "https://www.sermonindex.net/speakers/norman-grubb/suffering-part-2/",
                        "s": "Norman Grubb",
                        "g": "norman-grubb",
                        "m": "https://www.sermonindex.net/images/speakers/n/normangrubb.png",
                        "d": "36:15",
                        "a": "https://archive.org/download/SERMONINDEX_SID17354/SID17354.mp3",
                        "c": "https://sermonindex1.b-cdn.net/17/SID17354.mp3",
                        "y": "The sermon explores the transformative power of suffering.",
                        "p": ["Suffering", "suffering"],
                        "v": ["Romans 1:20", "ROM/1/20"],
                        "x": 1
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<RandomShardResponse>(rawJson)
        assertEquals(1, response.shard)
        assertEquals(1, response.count)
        assertEquals(1, response.sermons.size)

        val dto = response.sermons.first()
        val domain = dto.toDomain()

        assertEquals("tphPJyH4SC3Gl1WO", domain.id)
        assertEquals("Suffering - Part 2", domain.title)
        assertEquals("Norman Grubb", domain.speaker)
        assertEquals("https://www.sermonindex.net/images/speakers/n/normangrubb.png", domain.speakerImage)
        assertEquals("36:15", domain.duration)
        assertEquals("https://archive.org/download/SERMONINDEX_SID17354/SID17354.mp3", domain.mp3Url)
        assertEquals("https://sermonindex1.b-cdn.net/17/SID17354.mp3", domain.cdnMp3Url)
        assertEquals("The sermon explores the transformative power of suffering.", domain.summary)
        assertTrue(domain.hasAudio)
        assertFalse(domain.isTextOnly)

        assertEquals(1, domain.topics.size)
        assertEquals("Suffering", domain.topics.first().name)
        assertEquals("suffering", domain.topics.first().slug)
    }

    @Test
    fun randomSermon_textOnly_isTextOnlyIsTrue() {
        val rawJson = """
            {
                "shard": 2,
                "count": 1,
                "sermons": [
                    {
                        "i": "spurgeon_manuscript",
                        "t": "Morning by Morning",
                        "s": "Charles Spurgeon",
                        "a": "",
                        "c": "",
                        "x": 0
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<RandomShardResponse>(rawJson)
        val domain = response.sermons.first().toDomain()

        assertFalse(domain.hasAudio)
        assertTrue(domain.isTextOnly)
    }
}
