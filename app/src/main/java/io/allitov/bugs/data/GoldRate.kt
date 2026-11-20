package io.allitov.bugs.data

/**
 * Учётный курс золота Банка России.
 *
 * @property pricePerGram цена одного грамма золота, рублей
 * @property date         дата котировки в формате дд.ММ.гггг как в ответе ЦБ (или null)
 */
data class GoldRate(
    val pricePerGram: Double,
    val date: String?
)
