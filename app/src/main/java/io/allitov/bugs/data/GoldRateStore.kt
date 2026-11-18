package io.allitov.bugs.data

import android.content.Context

/**
 * Локальный кеш курса золота ЦБ: последний успешно загруженный курс.
 * Используется виджетом и игрой, когда сеть недоступна.
 */
object GoldRateStore {

    private const val PREFS_NAME = "gold_rate_prefs"
    private const val KEY_PRICE_PER_GRAM = "price_per_gram"
    private const val KEY_RATE_DATE = "rate_date"
    private const val KEY_UPDATED_AT = "updated_at_millis"

    /** Сохраняет курс как последний успешно загруженный. */
    fun save(context: Context, rate: GoldRate) {
        preferences(context).edit()
            .putString(KEY_PRICE_PER_GRAM, rate.pricePerGram.toString())
            .putString(KEY_RATE_DATE, rate.date)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    /** Последний сохранённый курс за грамм или null, если курс ещё не загружался. */
    fun pricePerGram(context: Context): Double? =
        preferences(context).getString(KEY_PRICE_PER_GRAM, null)?.toDoubleOrNull()

    /** Дата последнего сохранённого курса или null. */
    fun rateDate(context: Context): String? =
        preferences(context).getString(KEY_RATE_DATE, null)

    /** Время последнего обновления курса (миллисекунды) или 0, если курса ещё нет. */
    fun updatedAt(context: Context): Long =
        preferences(context).getLong(KEY_UPDATED_AT, 0L)

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
