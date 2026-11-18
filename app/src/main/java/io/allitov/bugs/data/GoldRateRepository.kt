package io.allitov.bugs.data

import android.content.Context
import io.allitov.bugs.network.CbrMetalsApi
import io.allitov.bugs.network.GoldRateParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Источник курса золота ЦБ для игры и виджета: загружает курс через Retrofit
 * ([CbrMetalsApi]) и сохраняет его в [GoldRateStore]; при ошибке сети
 * возвращает последний сохранённый курс.
 */
class GoldRateRepository(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Загружает свежий курс золота ЦБ (рублей за грамм) и обновляет кеш.
     * Если загрузка не удалась, возвращает последний сохранённый курс или null.
     */
    suspend fun loadGoldPricePerGram(): Double? = withContext(Dispatchers.IO) {
        try {
            val xml = CbrMetalsApi.instance.getMetalsXml()
            val rate = GoldRateParser.parse(xml)
                ?: error("В ответе ЦБ нет данных о цене золота")
            GoldRateStore.save(appContext, rate)
            rate.pricePerGram
        } catch (e: Exception) {
            GoldRateStore.pricePerGram(appContext)
        }
    }

    companion object {

        /** Курс из локального кеша без обращения к сети или null. */
        fun cachedPricePerGram(context: Context): Double? =
            GoldRateStore.pricePerGram(context.applicationContext)
    }
}
