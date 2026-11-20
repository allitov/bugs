package io.allitov.bugs.data

import android.content.Context
import android.util.Log
import io.allitov.bugs.network.CbrMetalsApi
import io.allitov.bugs.network.GoldRateParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Источник курса золота ЦБ для игры и виджета: загружает курс через Retrofit
 * ([CbrMetalsApi]) и сохраняет его в [GoldRateStore]; при ошибке сети
 * возвращает последний сохранённый курс.
 */
class GoldRateRepository(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Загружает свежий курс золота ЦБ (рублей за грамм) и обновляет кеш.
     * Запрос выполняется за последние [LOOKBACK_DAYS] дней: параметры дат для
     * xml_metall.asp обязательны. Если загрузка не удалась, возвращает
     * последний сохранённый курс или null.
     */
    suspend fun loadGoldPricePerGram(): Double? = withContext(Dispatchers.IO) {
        try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val xml = CbrMetalsApi.instance(appContext).getMetalsXml(
                dateFrom = format.format(Date(System.currentTimeMillis() - LOOKBACK_DAYS * MILLIS_PER_DAY)),
                dateTo = format.format(Date())
            )
            val rate = GoldRateParser.parse(xml)
            if (rate == null) {
                // Обычно это HTML-страница вместо XML (защита сервера ЦБ или 404)
                Log.w(TAG, "В ответе ЦБ нет данных о золоте (получено ${xml.length} символов)")
                error("В ответе ЦБ нет данных о цене золота")
            }
            GoldRateStore.save(appContext, rate)
            rate.pricePerGram
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить курс золота ЦБ", e)
            GoldRateStore.pricePerGram(appContext)
        }
    }

    companion object {

        private const val TAG = "GoldRateRepository"

        /** Глубина периода запроса котировок, дней. */
        private const val LOOKBACK_DAYS = 30

        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /** Курс из локального кеша без обращения к сети или null. */
        fun cachedPricePerGram(context: Context): Double? =
            GoldRateStore.pricePerGram(context.applicationContext)
    }
}
