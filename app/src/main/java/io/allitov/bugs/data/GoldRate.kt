package io.allitov.bugs.data

/**
 * Учётный курс золота Банка России.
 *
 * @property pricePerGram цена одного грамма золота, рублей
 * @property date         дата котировки в формате ГГГГММДД (или null, если неизвестна)
 */
data class GoldRate(
    val pricePerGram: Double,
    val date: String?
)
