package io.allitov.bugs

data class Player(
    val fullName: String,
    val gender: String,
    val course: Int,
    val difficulty: String,
    val birthDate: String,
    val zodiacSign: String
) {
    override fun toString(): String = """
        ФИО: $fullName
        Пол: $gender
        Курс: $course
        Уровень сложности: $difficulty
        Дата рождения: $birthDate
        Знак зодиака: $zodiacSign
    """.trimIndent()
}