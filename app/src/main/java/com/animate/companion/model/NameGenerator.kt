package com.animate.companion.model

import kotlin.random.Random

/** Original names for an eccentric forest town, written in Cyrillic. */
object NameGenerator {
    private val legacyFemale = listOf(
        "Сакура", "Хината", "Юи", "Рин", "Мику", "Аой", "Хана", "Мио", "Асуна", "Куруми",
        "Нанами", "Мэй", "Харука", "Акари", "Каэдэ", "Судзу", "Момо", "Шиори", "Тика", "Юдзуки",
        "Рури", "Хикари", "Кохару", "Цубаки", "Эми", "Нодзоми", "Канна", "Саяка", "Микото", "Аямэ",
        "Котори", "Мэгуми", "Саки", "Юкино", "Эрика", "Ран", "Химари", "Мизуки", "Ринко", "Ая",
        "Минори", "Тсумуги", "Ику", "Сора", "Нэнэ", "Шизуку", "Ханако", "Мисаки", "Ёсино", "Фуюка",
    )
    private val legacyMale = listOf(
        "Харуто", "Рэн", "Кайто", "Юто", "Рику", "Такэру", "Акира", "Хаято", "Дайчи", "Кэндзи",
        "Рю", "Сота", "Ямато", "Шо", "Тоя", "Кё", "Тайга", "Райдэн", "Джин", "Кэй",
        "Сэйя", "Изуми", "Минато", "Тацуя", "Кёске", "Сэцуна", "Ёсуке", "Шинья", "Хироши", "Котаро",
        "Рёта", "Сюн", "Гин", "Арата", "Юкихиро", "Мадока", "Кадзума", "Тэцуя", "Рэйдзи", "Акио",
    )
    private val legacyUnisex = listOf(
        "Рэй", "Ицуки", "Макото", "Хикару", "Каору", "Хару", "Аой", "Нагиса", "Синобу", "Тихиро",
        "Сора", "Юки", "Мидори", "Акира", "Рин", "Наоми", "Кадзуми", "Мицуки", "Хотару", "Юу",
    )
    private val legacySurnames = listOf(
        "Хошино", "Цукиширо", "Сакураба", "Амамия", "Куросава", "Широгане", "Таканаши", "Ханадзава",
        "Кагами", "Минадзуки", "Юкиширо", "Акацуки", "Кирю", "Хошизора", "Камия", "Курогане",
        "Татибана", "Шинономэ", "Айдзава", "Киришима", "Хиномия", "Фудзисаки", "Мидзухара", "Асахина",
        "Куроба", "Итиносэ", "Сумэраги", "Химура", "Цукисаки", "Аманэ", "Кудзё", "Сайондзи",
        "Тэндо", "Мицурэ", "Казэхая", "Хаякава", "Морино", "Сирасаги", "Ёимия", "Курэнай",
    )

    private val female = listOf("Нора", "Тэсс", "Джун", "Пенни", "Руби", "Хейзел", "Айви", "Одри", "Клара", "Элли", "Мэйси", "Вайолет", "Бонни", "Лила", "Фэй", "Дотти", "Эйприл", "Пайпер", "Дейзи", "Сэди")
    private val male = listOf("Финн", "Майлз", "Оуэн", "Тоби", "Эллиот", "Джаспер", "Феликс", "Арчи", "Хэнк", "Уолтер", "Эммет", "Луи", "Гас", "Клайд", "Нейт", "Купер", "Тедди", "Рори", "Оскар", "Лео")
    private val unisex = listOf("Райли", "Кейси", "Роуэн", "Джейми", "Куинн", "Эйвери", "Скай", "Тейлор", "Морган", "Чарли", "Риз", "Ривер", "Дакота", "Сэм", "Алекс", "Паркер")
    private val surnames = listOf("Брукс", "Блэквуд", "Фокс", "Крик", "Холлоу", "Мосс", "Кедар", "Харт", "Финч", "Рид", "Торн", "Бакстер", "Уайлдер", "Эшвуд", "Локвуд", "Стоун", "Хоббс", "Мэйпл", "Берч", "Бринк", "Белл", "Грейвс", "Кроу", "Хилл", "Фэрроу", "Бун", "Оукс", "Уитлок", "Хоторн", "Фрост")

    /** Recognize complete old generated pairs; leave free-form creator names unchanged.
     * A fixed seed keeps the replacement stable across restarts and conversation updates.
     */
    fun refreshLegacyName(name: String, gender: Gender, seed: Int): String {
        val parts = name.split(" ")
        if (parts.size != 2 || parts[1] !in legacySurnames ||
            parts[0] !in legacyFemale + legacyMale + legacyUnisex) return name
        return generate(gender, Random(seed xor name.hashCode()))
    }

    fun generate(gender: Gender, random: Random = Random.Default): String {
        val given = when (gender) {
            Gender.FEMALE -> female
            Gender.MALE -> male
            Gender.NEUTRAL -> unisex
        }.random(random)
        return "$given ${surnames.random(random)}"
    }

    /** Retained for creator compatibility; town names use no honorific. */
    @Suppress("UNUSED_PARAMETER")
    fun suffix(gender: Gender): String = ""
}
