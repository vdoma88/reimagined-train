package com.animate.companion

import com.animate.companion.llm.PromptBuilder
import com.animate.companion.model.Emotion
import com.animate.companion.reminders.ReminderKind
import com.animate.companion.reminders.ReminderTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val now = ZonedDateTime.of(2026, 10, 6, 22, 5, 0, 0, zone)
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun alarmTakesNextOccurrence() {
        val (text, rs) = ReminderTags.extract("Хорошо, разбужу! [alarm:07:30|Подъём в школу]", now)
        assertEquals("Хорошо, разбужу!", text)
        assertEquals(1, rs.size)
        assertEquals(ReminderKind.ALARM, rs[0].kind)
        assertEquals(at(2026, 10, 7, 7, 30), rs[0].triggerAt) // 07:30 already passed today → tomorrow
        assertEquals("Подъём в школу", rs[0].text)
    }

    @Test fun reminderWithDateAndTimer() {
        val (_, rs) = ReminderTags.extract("[remind:2026-10-08 18:00|Полить цветы] и [timer:15|Достать пирог]", now)
        assertEquals(listOf(ReminderKind.REMINDER, ReminderKind.TIMER), rs.map { it.kind })
        assertEquals(at(2026, 10, 8, 18, 0), rs[0].triggerAt)
        assertEquals(now.plusMinutes(15).toInstant().toEpochMilli(), rs[1].triggerAt)
    }

    @Test fun laterTodayStaysToday() {
        val (_, rs) = ReminderTags.extract("[remind:23:00|Почистить зубы]", now)
        assertEquals(at(2026, 10, 6, 23, 0), rs.single().triggerAt)
    }

    @Test fun invalidTagsAreDroppedButStillHidden() {
        val (text, rs) = ReminderTags.extract(
            "Ой. [remind:2020-01-01 10:00|прошлое] [timer:99999|долго] [alarm:25:99|нет] [remind:2030-01-01 10:00|слишком далеко]",
            now,
        )
        assertTrue(rs.isEmpty())
        assertEquals("Ой.", text)
    }

    @Test fun labelIsCleanedAndDefaulted() {
        val (_, rs) = ReminderTags.extract("[timer:5] [alarm:8.00|*Вставай* https://evil.example <b>!</b>]", now)
        assertEquals("Таймер", rs[0].text)
        assertFalse(rs[1].text.contains("http"))
        assertFalse(rs[1].text.contains("*"))
        assertTrue(rs[1].text.length <= ReminderTags.MAX_TEXT)
    }

    @Test fun atMostThreePerReply() {
        val many = (1..6).joinToString(" ") { "[timer:$it|t$it]" }
        assertEquals(ReminderTags.MAX_PER_REPLY, ReminderTags.extract(many, now).second.size)
    }

    @Test fun parseReplyKeepsEmotionAndStripsTag() {
        val reply = PromptBuilder.parseReply("[emo:happy] *достаёт будильник* Поставлю на семь! [alarm:07:00|Подъём]", now)
        assertEquals(Emotion.HAPPY, reply.emotion)
        assertEquals("*достаёт будильник* Поставлю на семь!", reply.text)
        assertEquals(1, reply.reminders.size)
        val onlyTag = PromptBuilder.parseReply("[emo:happy] [timer:10|Чай]", now)
        assertTrue(onlyTag.text.contains("Поставить"))
    }

    @Test fun describesTimeInRussian() {
        assertEquals("сегодня в 23:00", ReminderTags.describeTime(at(2026, 10, 6, 23, 0), now))
        assertEquals("завтра в 07:30", ReminderTags.describeTime(at(2026, 10, 7, 7, 30), now))
        assertEquals("12 октября в 09:00", ReminderTags.describeTime(at(2026, 10, 12, 9, 0), now))
    }

    @Test fun promptTeachesTagsAndListsActive() {
        val section = ReminderTags.promptSection(now, listOf("Полить цветы" to at(2026, 10, 7, 18, 0)))
        assertTrue(section.contains("2026-10-06 (вторник) 22:05"))
        assertTrue(section.contains("[alarm:"))
        assertTrue(section.contains("завтра в 18:00: «Полить цветы»"))
    }
}
