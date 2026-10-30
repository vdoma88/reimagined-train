package com.animate.companion.model

import kotlin.random.Random

/** Short, gender-neutral details for an original forest-town character. */
object CharacterNoteGenerator {
    private val hobbies = listOf(
        "Коллекционирует странные карты", "Рисует комиксы о соседях",
        "Записывает городские легенды", "Чинит старые радиоприёмники",
        "Собирает значки с фестивалей", "Фотографирует необычные облака",
        "Ищет забавные опечатки на вывесках", "Изучает следы лесных животных",
        "Придумывает настольные игры", "Собирает камни смешной формы",
        "Рисует карты любимых прогулок", "Выращивает растения на подоконнике",
        "Сочиняет истории о заброшенной станции", "Мастерит бумажные фонарики",
        "Собирает открытки из маленьких городков", "Разгадывает шифры в старых журналах",
    )
    private val habits = listOf(
        "носит запасной карандаш", "даёт смешные имена своим вещам",
        "всегда оставляет другу кусочек пирога", "проверяет догадки по блокноту",
        "любит какао в дождливые дни", "разговаривает с неработающей техникой",
        "хранит билеты как сувениры", "прячет закладки в самых неожиданных местах",
        "придумывает названия обычным прогулкам", "запоминает местные байки",
        "рисует звёздочки на полях", "берёт фонарик даже на дневную прогулку",
        "ищет во всём повод для доброй шутки", "сравнивает новые находки со старыми записями",
        "любит скрип старых половиц", "ведёт список маленьких открытий",
    )
    private val dreams = listOf(
        "мечтает увидеть море", "хочет открыть клуб загадок",
        "мечтает сделать выставку своих находок", "хочет нарисовать комикс о летних каникулах",
        "мечтает найти потерянную капсулу времени", "хочет восстановить старую карту городка",
        "мечтает устроить фестиваль курьёзов", "хочет записать собственную радиопередачу",
        "мечтает построить уютный домик на дереве", "хочет собрать альбом городских историй",
        "мечтает увидеть метеорный дождь", "хочет придумать самый смешной сувенир",
        "мечтает пройти безопасный маршрут по заповеднику", "хочет научиться печь идеальный пирог",
        "мечтает найти друзей для настольных приключений", "хочет разгадать загадку старого дневника",
    )

    // Narrative perks are part of the editable character description and reach the prompt as data.
    val perks = listOf(
        "запоминает необычные звуки", "быстро замечает повторяющиеся детали",
        "умеет успокаивать друзей шуткой", "находит неожиданные применения обычным вещам",
        "терпеливо разбирается в запутанных записях", "рисует понятные схемы и карты",
        "замечает, когда кому-то нужна поддержка", "придумывает смешные прозвища предметам",
        "слышит фальшивые ноты", "умеет объяснить сложное на простом примере",
        "помнит интересные городские истории", "видит смешное в мелких неудачах",
        "подбирает музыку под настроение", "находит общее между непохожими идеями",
        "запоминает узоры и символы", "умеет составить запасной план",
    )

    /** Replace only the generated feature; preserve the rest of the creator's description. */
    fun randomizePerk(note: String, random: Random = Random.Default): String {
        val marker = "Особенность: "
        val start = note.indexOf(marker)
        val current = if (start >= 0) note.substring(start + marker.length).substringBefore('.').trim() else ""
        val choices = perks.filter { it != current }
        val feature = marker + choices.random(random) + "."
        val prefix = if (start >= 0) note.substring(0, start).trimEnd() else note.trimEnd()
        val suffix = if (start >= 0) note.substring(start + marker.length).substringAfter('.', "").trimStart() else ""
        val separator = if (prefix.isNotEmpty() && !prefix.endsWith(" ")) " " else ""
        val ending = if (suffix.isNotEmpty()) " $suffix" else ""
        val result = prefix + separator + feature + ending
        // Never discard user-written text just to fit an automatically generated feature.
        return if (result.length <= 400) result else note
    }

    /** Avoid repeating the current complete description when the dice is tapped. */
    fun generate(previous: String = "", random: Random = Random.Default): String {
        val hobby = hobbies.random(random)
        val habit = habits.random(random)
        val dream = dreams.random(random)
        val prefix = "$hobby, $habit, $dream. Особенность: "
        val available = perks.filter { prefix + it + "." != previous }
        return prefix + available.random(random) + "."
    }
}
