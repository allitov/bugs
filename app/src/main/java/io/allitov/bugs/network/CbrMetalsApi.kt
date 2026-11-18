package io.allitov.bugs.network

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

/**
 * Retrofit-API справочника котировок драгоценных металлов Банка России.
 *
 * Запрос [getMetalsXml] без параметров возвращает XML с учётными ценами
 * металлов за предыдущий месяц: записи «Record» с датой, кодом металла
 * и ценой покупки/продажи за грамм (код «1» — золото).
 */
interface CbrMetalsApi {

    /** Возвращает XML с учётными ценами драгоценных металлов ЦБ РФ. */
    @GET("scripts/xml_metall.asp")
    suspend fun getMetalsXml(): String

    companion object {

        /** Базовый адрес сервера Банка России. */
        const val BASE_URL = "https://www.cbr.ru/"

        /** Готовый к работе экземпляр API (создаётся лениво и один раз). */
        val instance: CbrMetalsApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(CbrMetalsApi::class.java)
        }
    }
}
