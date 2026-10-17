package com.wardclick.app.util

import kotlin.random.Random

/** Публичный профиль автора отзыва (демо-данные). */
data class ReviewerProfile(
    val name: String,
    val initials: String,
    val avatarColor: Long,
    val city: String,
    val memberSince: String,
    val trips: Int,
    val reviews: Int,
    val avgRating: Double,
    val isSuperGuest: Boolean,
    val about: String,
    val languages: List<String>,
    val interests: List<String>,
    val verified: List<String>,
    val favoriteType: String,
    val responseTime: String
)

/**
 * Генератор профилей авторов отзывов. Информация «случайная», но у каждого человека своя
 * и не меняется между запусками: генератор инициализируется от имени и id автора.
 */
object ReviewerProfiles {

    private val cities = listOf(
        "Москва", "Санкт-Петербург", "Казань", "Екатеринбург", "Новосибирск", "Сочи",
        "Краснодар", "Нижний Новгород", "Самара", "Калининград", "Ростов-на-Дону", "Пермь"
    )
    private val months = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )
    private val abouts = listOf(
        "Путешествую с семьёй, люблю большие дома за городом и долгие ужины на террасе.",
        "Организую выездные встречи для команды — ценю просторные гостиные и хорошую кухню.",
        "Отмечаю праздники только в особняках: бассейн, баня и много места для гостей.",
        "Фотограф. Ищу дома с красивыми видами и необычной архитектурой.",
        "Люблю тишину, лес и камин. Зимой выбираю шале, летом — виллу у моря.",
        "Езжу с друзьями на выходные и всегда оставляю честные отзывы.",
        "Работаю удалённо и пару раз в год арендую дом на месяц у моря.",
        "Собираю большую семью на юбилеи — нужен дом минимум на 12 человек.",
        "Ценю сервис: персонал, чистоту и быстрые ответы владельцев.",
        "Открываю для себя города России и выбираю особняки рядом с природой."
    )
    private val extraLanguages = listOf("Английский", "Немецкий", "Французский", "Испанский", "Итальянский", "Китайский", "Татарский")
    private val interestPool = listOf(
        "Баня", "Бассейн", "Горы", "Море", "Рыбалка", "Гастрономия",
        "Фотография", "Лыжи", "Йога", "Активный отдых", "Архитектура", "Конные прогулки"
    )
    private val types = listOf("Особняк", "Вилла", "Усадьба", "Шале")
    private val responseTimes = listOf("в течение часа", "в течение нескольких часов", "в течение дня")
    private val colors = listOf(0xFF0B6E99, 0xFFFF6B4A, 0xFF2E7D32, 0xFF6A1B9A, 0xFFC62828, 0xFF00838F, 0xFFEF6C00, 0xFF37474F)

    fun forAuthor(name: String, userId: String = ""): ReviewerProfile {
        val cleanName = name.ifBlank { "Гость" }
        val rnd = Random((cleanName.lowercase() + "|" + userId).hashCode())

        val trips = rnd.nextInt(3, 49)
        val reviews = rnd.nextInt(1, trips + 1)
        val avg = (43 + rnd.nextInt(0, 8)) / 10.0 // 4.3 … 5.0
        val year = rnd.nextInt(2019, 2026)
        val month = months[rnd.nextInt(months.size)]

        val languages = listOf("Русский") + extraLanguages.shuffled(rnd).take(rnd.nextInt(0, 3))
        val verified = buildList {
            add("Электронная почта")
            if (rnd.nextInt(100) < 85) add("Номер телефона")
            if (rnd.nextInt(100) < 60) add("Паспорт")
        }
        val initials = cleanName.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }

        return ReviewerProfile(
            name = cleanName,
            initials = initials,
            avatarColor = colors[rnd.nextInt(colors.size)],
            city = cities[rnd.nextInt(cities.size)],
            memberSince = "с $month $year",
            trips = trips,
            reviews = reviews,
            avgRating = avg,
            isSuperGuest = trips >= 20 && avg >= 4.7,
            about = abouts[rnd.nextInt(abouts.size)],
            languages = languages,
            interests = interestPool.shuffled(rnd).take(3),
            verified = verified,
            favoriteType = types[rnd.nextInt(types.size)],
            responseTime = responseTimes[rnd.nextInt(responseTimes.size)]
        )
    }
}
