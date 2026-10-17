package com.example.sermonindex.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VttParserTest {

    @Test
    fun parse_emptyOrBlank_returnsEmptyList() {
        assertTrue(VttParser.parse("").isEmpty())
        assertTrue(VttParser.parse("   \n\n  ").isEmpty())
    }

    @Test
    fun parse_standardWebVtt_parsesCuesCorrectly() {
        val sampleVtt = """
            WEBVTT

            1
            00:00:00.840 --> 00:00:03.520
            I want to thank you for the privilege

            2
            00:00:03.520 --> 00:00:05.600
            of sharing your fellowship this week along with

            3
            00:00:05.600 --> 00:00:06.120
            my wife.
        """.trimIndent()

        val cues = VttParser.parse(sampleVtt)
        assertEquals(3, cues.size)

        assertEquals(0, cues[0].index)
        assertEquals(840L, cues[0].startMs)
        assertEquals(3520L, cues[0].endMs)
        assertEquals("I want to thank you for the privilege", cues[0].text)

        assertEquals(1, cues[1].index)
        assertEquals(3520L, cues[1].startMs)
        assertEquals(5600L, cues[1].endMs)
        assertEquals("of sharing your fellowship this week along with", cues[1].text)

        assertEquals(2, cues[2].index)
        assertEquals(5600L, cues[2].startMs)
        assertEquals(6120L, cues[2].endMs)
        assertEquals("my wife.", cues[2].text)
    }

    @Test
    fun parse_hoursAndMinutesTimestamps_parsesCorrectly() {
        val sampleVtt = """
            WEBVTT

            01:15:30.500 --> 01:15:35.000
            Looking unto Jesus the author and finisher of our faith.
        """.trimIndent()

        val cues = VttParser.parse(sampleVtt)
        assertEquals(1, cues.size)
        // 1 hr = 3600s, 15 min = 900s, 30.5s = 30.5s -> 4530.5s = 4530500 ms
        assertEquals(4530500L, cues[0].startMs)
        assertEquals(4535000L, cues[0].endMs)
        assertEquals("Looking unto Jesus the author and finisher of our faith.", cues[0].text)
    }

    @Test
    fun parse_htmlTagsAndEntities_cleanedProperly() {
        val sampleVtt = """
            WEBVTT

            00:00:01.000 --> 00:00:04.000
            <v Preacher>Grace &amp; peace to you <b>all</b>.</v>
        """.trimIndent()

        val cues = VttParser.parse(sampleVtt)
        assertEquals(1, cues.size)
        assertEquals("Grace & peace to you all.", cues[0].text)
    }
}
