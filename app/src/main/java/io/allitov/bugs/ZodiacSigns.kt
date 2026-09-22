package io.allitov.bugs

import androidx.annotation.DrawableRes

enum class ZodiacSign(
    val title: String,
    @DrawableRes val imageRes: Int
) {
    ARIES("Овен", R.drawable.zodiac_aries),
    TAURUS("Телец", R.drawable.zodiac_taurus),
    GEMINI("Близнецы", R.drawable.zodiac_gemini),
    CANCER("Рак", R.drawable.zodiac_cancer),
    LEO("Лев", R.drawable.zodiac_leo),
    VIRGO("Дева", R.drawable.zodiac_virgo),
    LIBRA("Весы", R.drawable.zodiac_libra),
    SCORPIO("Скорпион", R.drawable.zodiac_scorpio),
    SAGITTARIUS("Стрелец", R.drawable.zodiac_sagittarius),
    CAPRICORN("Козерог", R.drawable.zodiac_capricorn),
    AQUARIUS("Водолей", R.drawable.zodiac_aquarius),
    PISCES("Рыбы", R.drawable.zodiac_pisces);

    companion object {
        fun from(day: Int, month: Int): ZodiacSign = when (month) {
            1  -> if (day <= 19) CAPRICORN else AQUARIUS
            2  -> if (day <= 18) AQUARIUS else PISCES
            3  -> if (day <= 20) PISCES else ARIES
            4  -> if (day <= 19) ARIES else TAURUS
            5  -> if (day <= 20) TAURUS else GEMINI
            6  -> if (day <= 20) GEMINI else CANCER
            7  -> if (day <= 22) CANCER else LEO
            8  -> if (day <= 22) LEO else VIRGO
            9  -> if (day <= 22) VIRGO else LIBRA
            10 -> if (day <= 22) LIBRA else SCORPIO
            11 -> if (day <= 21) SCORPIO else SAGITTARIUS
            12 -> if (day <= 21) SAGITTARIUS else CAPRICORN
            else -> throw IllegalArgumentException("Недопустимый месяц: $month")
        }
    }
}