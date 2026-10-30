package com.animate.companion.model

/** Short, opt-in fictional scenes. The player's choice is sent through the normal chat. */
data class StoryScene(val id: String, val title: String, val setup: String, val choices: List<String>)

object StoryScenes {
    val all = listOf(
        StoryScene("archive", "Записка в архиве", "В старом архиве между страницами карты нашлась записка с непонятными знаками.", listOf("Рассмотреть записку и поискать подсказки", "Расспросить хранителя архива")),
        StoryScene("radio", "Сигнал на радио", "На местной радиостанции среди музыки прозвучал странный короткий сигнал.", listOf("Записать сигнал и внимательно послушать", "Спросить ведущего, откуда идёт передача")),
        StoryScene("festival", "Путаница на фестивале", "Перед праздником городка неожиданно пропала коробка с украшениями.", listOf("Посмотреть, где её видели в последний раз", "Предложить вместе сделать новые украшения")),
    )

    /** Only a recognized feature in the editable feature field unlocks the special choice. */
    fun perk(note: String): String? {
        val marker = "Особенность: "
        val value = note.substringAfter(marker, "").substringBefore('.').trim()
        return value.takeIf { it in CharacterNoteGenerator.perks }
    }

    fun choices(scene: StoryScene, note: String): List<String> =
        scene.choices + listOfNotNull(perk(note)?.let { "Применить особенность героя: $it" })

    fun message(scene: StoryScene, choice: String): String =
        "Давай сыграем короткую вымышленную сцену. ${scene.setup} Мой выбор: $choice. " +
            "Продолжи историю, учти характер и особенность героя, затем предложи, что можно сделать дальше."
}
