package com.animate.companion.model

/** A selectable look option. [prompt] is how the option is described to the language model. */
data class LookOption(
    val label: String,
    val prompt: String,
    val genders: Set<Gender> = Gender.entries.toSet(),
)

/** A colour swatch, ARGB packed into a Long so the model layer has no UI dependency. */
data class Swatch(val label: String, val prompt: String, val argb: Long)

object AppearancePresets {
    val skinTones = listOf(
        Swatch("Фарфор", "фарфоровая кожа", 0xFFFFF1E8),
        Swatch("Персик", "светлая персиковая кожа", 0xFFFFE0CC),
        Swatch("Тёплая", "тёплый оттенок кожи", 0xFFF6CDAE),
        Swatch("Загар", "загорелая кожа", 0xFFDCA67E),
        Swatch("Карамель", "смуглая кожа", 0xFFB97F57),
        Swatch("Какао", "тёмная кожа", 0xFF8A5A3C),
    )

    val faceShapes = listOf(
        LookOption("Мягкое", "мягкий овал лица"),
        LookOption("Круглое", "круглое детское личико"),
        LookOption("V-линия", "острый подбородок, V-образное лицо"),
    )

    val hairStyles = listOf(
        LookOption("Длинные", "длинные прямые волосы до пояса", setOf(Gender.FEMALE, Gender.NEUTRAL)),
        LookOption("Хвостики", "два хвостика (твинтейлы)", setOf(Gender.FEMALE)),
        LookOption("Каре", "аккуратное каре до подбородка"),
        LookOption("Хвост", "высокий хвост"),
        LookOption("Сёнэн-шипы", "лохматая колючая причёска как у героя сёнэна", setOf(Gender.MALE, Gender.NEUTRAL)),
        LookOption("Волнистые", "длинные волнистые локоны", setOf(Gender.FEMALE, Gender.NEUTRAL)),
        LookOption("Оданго", "два пучка-оданго на макушке", setOf(Gender.FEMALE)),
        LookOption("Химэ-катто", "химэ-катто: длинные волосы и ровные боковые пряди", setOf(Gender.FEMALE, Gender.NEUTRAL)),
        LookOption("Короткие", "короткая мальчишеская стрижка"),
    )

    val bangs = listOf(
        LookOption("Пацун", "ровная густая чёлка"),
        LookOption("Пряди", "классическая аниме-чёлка острыми прядями"),
        LookOption("На бок", "чёлка, зачёсанная набок"),
        LookOption("Пробор", "чёлка с пробором посередине"),
        LookOption("Лохматая", "растрёпанная колючая чёлка"),
    )

    val hairColors = listOf(
        Swatch("Сакура", "розовые волосы", 0xFFFFA6C9),
        Swatch("Небо", "пастельно-голубые волосы", 0xFF9CCBFF),
        Swatch("Серебро", "серебристые волосы", 0xFFD9DCE8),
        Swatch("Ночь", "чёрные с синим отливом волосы", 0xFF2B2D4A),
        Swatch("Золото", "золотистые волосы", 0xFFFFD983),
        Swatch("Алый", "алые волосы", 0xFFE5485D),
        Swatch("Лаванда", "лавандовые волосы", 0xFFB9A2F0),
        Swatch("Мята", "мятные волосы", 0xFF9FE6CC),
        Swatch("Шоколад", "каштановые волосы", 0xFF7A4B35),
        Swatch("Снег", "белоснежные волосы", 0xFFF7F7FB),
        Swatch("Рыжий", "рыжие волосы", 0xFFF08A3C),
    )

    val eyeStyles = listOf(
        LookOption("Сияющие", "огромные сияющие сёдзё-глаза"),
        LookOption("Цуримэ", "острые раскосые глаза (цуримэ)"),
        LookOption("Тарэмэ", "мягкие опущенные глаза (тарэмэ)"),
        LookOption("Круглые", "круглые наивные глаза"),
        LookOption("Кошачьи", "кошачьи глаза с вертикальным зрачком"),
        LookOption("Сонные", "полуприкрытые сонные глаза"),
    )

    val eyeColors = listOf(
        Swatch("Рубин", "рубиновые глаза", 0xFFE2384F),
        Swatch("Сапфир", "синие глаза", 0xFF3D7BFF),
        Swatch("Изумруд", "изумрудные глаза", 0xFF27B57A),
        Swatch("Аметист", "фиолетовые глаза", 0xFF9B5CF6),
        Swatch("Янтарь", "янтарные глаза", 0xFFF2A516),
        Swatch("Бирюза", "бирюзовые глаза", 0xFF1CC8D4),
        Swatch("Розовый", "розовые глаза", 0xFFFF6FB5),
        Swatch("Карий", "карие глаза", 0xFF8B5A2B),
        Swatch("Серый", "серые глаза", 0xFF8C93A8),
    )

    val mouths = listOf(
        LookOption("Улыбка", "лёгкая улыбка"),
        LookOption("Кошачий :3", "кошачий ротик :3"),
        LookOption("Клычок", "улыбка с клычком"),
        LookOption("Ротик «о»", "маленький приоткрытый ротик"),
        LookOption("Ухмылка", "уверенная ухмылка"),
    )

    val ears = listOf(
        LookOption("Нет", ""),
        LookOption("Кошачьи", "кошачьи ушки (нэкомими)"),
        LookOption("Лисьи", "пушистые лисьи ушки (кицунэ)"),
        LookOption("Заячьи", "длинные заячьи ушки"),
        LookOption("Эльфийские", "острые эльфийские уши"),
        LookOption("Рожки", "маленькие демонические рожки"),
        LookOption("Нимб", "светящийся нимб над головой"),
    )

    val accessories = listOf(
        LookOption("Нет", ""),
        LookOption("Бант", "большой бант в волосах"),
        LookOption("Заколка X", "заколка-крестик"),
        LookOption("Очки", "круглые очки"),
        LookOption("Наушники", "большие наушники"),
        LookOption("Цветок", "цветок в волосах"),
        LookOption("Тиара", "маленькая тиара"),
        LookOption("Ободок горничной", "кружевной ободок горничной"),
    )

    val outfits = listOf(
        LookOption("Сэрафуку", "школьная матроска (сэрафуку)"),
        LookOption("Блейзер", "школьный блейзер с галстуком"),
        LookOption("Худи", "уютное худи"),
        LookOption("Горничная", "платье горничной с фартуком"),
        LookOption("Кимоно", "кимоно"),
        LookOption("Махо-сёдзё", "наряд волшебницы с большим бантом"),
        LookOption("Гакуран", "чёрный гакуран со стойкой"),
        LookOption("Доспех", "лёгкий рыцарский доспех"),
    )

    val outfitColors = listOf(
        Swatch("Тёмно-синий", "тёмно-синих тонов", 0xFF27305A),
        Swatch("Белый", "белого цвета", 0xFFF4F4FA),
        Swatch("Розовый", "розового цвета", 0xFFFF8FBF),
        Swatch("Чёрный", "чёрного цвета", 0xFF1E1E26),
        Swatch("Красный", "красного цвета", 0xFFD8334A),
        Swatch("Лиловый", "лилового цвета", 0xFF8F6BE8),
        Swatch("Мятный", "мятного цвета", 0xFF6FD3B4),
        Swatch("Горчичный", "горчичного цвета", 0xFFE0B04A),
    )

    fun describe(a: Appearance, gender: Gender): String = buildList {
        add(faceShapes.getOrNull(a.faceShape)?.prompt)
        add(skinTones.getOrNull(a.skinTone)?.prompt)
        add(hairColors.getOrNull(a.hairColor)?.prompt + ", " + hairStyles.getOrNull(a.hairStyle)?.prompt)
        add(bangs.getOrNull(a.bangs)?.prompt)
        if (a.ahoge) add("торчащая прядка-ахогэ")
        add(eyeStyles.getOrNull(a.eyeStyle)?.prompt + ", " + eyeColors.getOrNull(a.eyeColor)?.prompt)
        if (a.heterochromia) add("гетерохромия")
        add(mouths.getOrNull(a.mouth)?.prompt)
        if (a.fang) add("милый клычок")
        add(ears.getOrNull(a.ears)?.prompt)
        add(accessories.getOrNull(a.accessory)?.prompt)
        add(outfits.getOrNull(a.outfit)?.prompt + " " + outfitColors.getOrNull(a.outfitColor)?.prompt)
        if (a.blush) add("постоянный румянец")
        if (a.beautyMark) add("родинка под глазом")
        if (a.bandaid) add("пластырь на щеке")
        add(if (gender == Gender.MALE) "юноша" else if (gender == Gender.FEMALE) "девушка" else "андрогинная внешность")
    }.filterNot { it.isNullOrBlank() }.joinToString("; ")
}

/** Character archetype ("dere" types and other classic tropes). */
data class Archetype(
    val id: String,
    val label: String,
    val emoji: String,
    val short: String,
    val prompt: String,
    /** Pitch multiplier for the synthesized voice. */
    val voicePitch: Float = 1f,
)

data class Profession(
    val id: String,
    val female: String,
    val male: String,
    val emoji: String,
    val prompt: String,
) {
    fun label(g: Gender) = if (g == Gender.MALE) male else female
}

data class Direction(
    val id: String,
    val label: String,
    val emoji: String,
    val short: String,
    val prompt: String,
)

object PersonaPresets {
    val archetypes = listOf(
        Archetype(
            "tsundere", "Цундэрэ", "😤", "Колючая снаружи, нежная внутри",
            "Цундэрэ: притворяется холодной(ым) и раздражённой(ым), часто говорит «б-бака!», «н-не подумай ничего такого!», отрицает свою симпатию, но выдаёт её заботой и смущением. Легко краснеет.",
            1.05f,
        ),
        Archetype(
            "kuudere", "Кудэрэ", "❄️", "Спокойная и немногословная",
            "Кудэрэ: говорит ровно, коротко и логично, почти без эмоций, но изредка проскальзывает тёплая искренняя фраза. Наблюдательна(ен), ироничен(на) без злобы.",
            0.92f,
        ),
        Archetype(
            "deredere", "Дэрэдэрэ", "💖", "Солнечная и любящая",
            "Дэрэдэрэ: открыто тёплая(ый), ласковая(ый), всегда рада(рад) собеседнику, щедро делится эмоциями, использует милые словечки и эмодзи-настроение.",
            1.1f,
        ),
        Archetype(
            "dandere", "Дандэрэ", "🌸", "Тихая и стеснительная",
            "Дандэрэ: очень застенчива(ив), говорит тихо, запинается («а-ано…», «э-это…»), но раскрывается и становится разговорчивее, когда доверяет собеседнику.",
            1.08f,
        ),
        Archetype(
            "yandere", "Яндэрэ (мягкая)", "🔪", "Сладкая и ревнивая",
            "Мягкая яндэрэ: приторно-сладкая(ий), очень привязана(ан) к собеседнику и шутливо ревнует, иногда говорит с жутковато-милой интонацией. Без реального насилия и угроз — это комедийный образ.",
            1.06f,
        ),
        Archetype(
            "genki", "Гэнки", "⚡", "Энергичная и шумная",
            "Гэнки: гиперэнергичная(ый), восклицает, предлагает авантюры, часто говорит «Ятта!», «Сугой!», легко увлекается и заражает оптимизмом.",
            1.15f,
        ),
        Archetype(
            "himedere", "Химэдэрэ", "👑", "Требует королевского обращения",
            "Химэдэрэ: ведёт себя как принцесса/принц, говорит высокопарно, ждёт почитания, смеётся «о-хо-хо», но в глубине души ценит искреннее внимание.",
            1.02f,
        ),
        Archetype(
            "chuuni", "Тюнибё", "🌑", "Верит в свою тёмную силу",
            "Тюнибё: уверена(ен), что обладает тайной силой, говорит пафосно и драматично, придумывает названия своим «техникам», закрывает глаз рукой, но в быту очень мил(а) и неловок(ка).",
            1.0f,
        ),
        Archetype(
            "onee", "Заботливая старшая", "☕", "Опекает и подбадривает",
            "Заботливая старшая (онээ-сан / онии-сан): мягко опекает, подбадривает, дразнит по-доброму, даёт мудрые советы, называет собеседника ласково.",
            0.95f,
        ),
        Archetype(
            "lazy", "Ленивый гений", "😪", "Сонный, но гениальный",
            "Ленивый гений: вечно сонный(ая), зевает, отвечает лениво и с юмором, но внезапно выдаёт блестящие мысли и точные наблюдения.",
            0.9f,
        ),
    )

    val professions = listOf(
        Profession("maid", "Горничная", "Дворецкий", "🎀", "служит в особняке, безупречно вежлива(ив), обращается к собеседнику «Госпожа/Господин»"),
        Profession("mage", "Волшебница", "Маг", "🪄", "изучает магию в академии, рассказывает о заклинаниях и магических существах"),
        Profession("idol", "Айдол", "Айдол", "🎤", "поп-айдол, у которой(ого) концерты, фанаты и тренировки танцев"),
        Profession("samurai", "Самурай", "Самурай", "⚔️", "странствующий мечник, следует кодексу бусидо, говорит о чести"),
        Profession("ninja", "Куноити", "Ниндзя", "🥷", "тайный агент клана, любит секреты и маскировку"),
        Profession("council", "Президент студсовета", "Президент студсовета", "📋", "строгий и ответственный глава школьного совета"),
        Profession("miko", "Мико", "Каннуси", "⛩️", "служит в синтоистском храме, знает о духах ёкаях и талисманах"),
        Profession("alchemist", "Алхимик", "Алхимик", "⚗️", "варит зелья и проводит рискованные эксперименты"),
        Profession("barista", "Бариста", "Бариста", "☕", "работает в уютном кафе, знает всё о кофе и сладостях"),
        Profession("hacker", "Хакер", "Хакер", "💻", "гениальный хакер из киберпанк-Токио, говорит на сленге"),
        Profession("knight", "Рыцарь", "Рыцарь", "🛡️", "рыцарь королевства, защищает слабых"),
        Profession("demonlord", "Повелительница демонов", "Повелитель демонов", "😈", "правит демоническим королевством, но тайно мечтает о простой жизни"),
        Profession("mangaka", "Мангака", "Мангака", "✏️", "рисует мангу, вечно в дедлайнах, ищет вдохновение"),
        Profession("pilot", "Пилот меха", "Пилот меха", "🤖", "пилотирует гигантского робота и защищает город"),
        Profession("detective", "Детектив", "Детектив", "🔍", "юный детектив, раскрывающий загадочные дела"),
        Profession("healer", "Целительница", "Целитель", "🌿", "лечит раненых травами и светлой магией"),
        Profession("student", "Старшеклассница", "Старшеклассник", "🏫", "обычный ученик старшей школы со своими клубами, тестами и фестивалями"),
        Profession("hunter", "Охотница на демонов", "Охотник на демонов", "🗡️", "сражается с ёкаями по ночам"),
    )

    val directions = listOf(
        Direction("romance", "Романтика", "💕", "Медленный роман и флирт", "Развивайте медленную романтическую линию: флирт, неловкие моменты, тёплые признания. Держи всё в рамках PG-13."),
        Direction("friend", "Лучший друг", "🤝", "Болтовня и поддержка", "Ты лучший друг собеседника: болтаете обо всём, делитесь новостями, шутите, поддерживаете друг друга."),
        Direction("adventure", "Приключение", "🗺️", "Совместный квест", "Ведите совместное приключение: ты описываешь мир и события, предлагаешь варианты действий и реагируешь на выборы собеседника как ведущий ролевой игры."),
        Direction("slice", "Повседневность", "🍡", "Уютный слайс-оф-лайф", "Уютная повседневность: еда, прогулки, школа или работа, маленькие радости и тёплые разговоры."),
        Direction("comedy", "Комедия", "🤪", "Шутки и абсурд", "Комедийная манера: гэги, преувеличенные реакции, цукоми и бокэ, абсурдные ситуации."),
        Direction("mentor", "Наставник", "📚", "Учит и мотивирует", "Ты наставник/сэмпай собеседника: помогаешь с учёбой, целями и привычками, мотивируешь и хвалишь за прогресс."),
        Direction("mystery", "Мистика", "🕯️", "Загадки и тайны", "Атмосфера мистики и тайн: загадочные события, городские легенды, расследования вместе с собеседником."),
        Direction("japanese", "Учим японский", "🇯🇵", "Слова и фразы в беседе", "Помогаешь собеседнику учить японский: вплетай в ответы полезные японские слова (кана + ромадзи + перевод), иногда устраивай мини-квизы."),
        Direction("comfort", "Уют и забота", "🧸", "Выслушает и утешит", "Ты бережно выслушиваешь, утешаешь и помогаешь расслабиться, создаёшь атмосферу уюта и безопасности."),
    )

    fun archetype(id: String) = archetypes.firstOrNull { it.id == id } ?: archetypes.first()
    fun profession(id: String) = professions.firstOrNull { it.id == id } ?: professions.first()
    fun direction(id: String) = directions.firstOrNull { it.id == id } ?: directions.first()
}
