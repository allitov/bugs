package io.allitov.bugs.network

import android.content.Context
import io.allitov.bugs.R
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Retrofit-API справочника котировок драгоценных металлов Банка России.
 *
 * Сертификат cbr.ru подписан корневым УЦ HARICA TLS RSA Root CA 2021, которого
 * нет в системном хранилище многих версий Android, поэтому HTTP-клиент доверяет
 * и системным УЦ, и корню HARICA, встроенному в приложение
 * (res/raw/harica_tls_rsa_root_ca_2021.cer).
 *
 * Запрос [getMetalsXml] возвращает XML с учётными ценами металлов за период:
 * записи «Record» с датой, кодом металла и ценой покупки/продажи за грамм
 * (код «1» — золото).
 */
interface CbrMetalsApi {

    /**
     * Возвращает XML с учётными ценами драгоценных металлов ЦБ РФ за период.
     *
     * Параметры дат обязательны: без них сервер отвечает страницей 404 в HTML
     * вместо XML, поэтому запрос выполняется всегда за период (см.
     * [io.allitov.bugs.data.GoldRateRepository]).
     *
     * @param dateFrom начало периода, формат дд/ММ/гггг
     * @param dateTo   конец периода, формат дд/ММ/гггг
     */
    // Браузерный User-Agent: часть защит ЦБ может блокировать нестандартные клиенты
    @Headers("User-Agent: Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
    @GET("scripts/xml_metall.asp")
    suspend fun getMetalsXml(
        @Query("date_req1") dateFrom: String,
        @Query("date_req2") dateTo: String
    ): String

    companion object {

        /** Базовый адрес сервера Банка России. */
        const val BASE_URL = "https://www.cbr.ru/"

        /** Псевдоним встроенного корневого сертификата HARICA в хранилище доверия. */
        private const val HARICA_ROOT_ALIAS = "harica_tls_rsa_root_ca_2021"

        @Volatile
        private var cached: CbrMetalsApi? = null

        /** Готовый к работе экземпляр API (создаётся лениво и один раз). */
        fun instance(context: Context): CbrMetalsApi =
            cached ?: synchronized(this) {
                cached ?: create(context.applicationContext).also { cached = it }
            }

        private fun create(context: Context): CbrMetalsApi {
            val trustManager = CompositeTrustManager(
                listOf(systemTrustManager(), bundledTrustManager(context))
            )
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<X509TrustManager>(trustManager), SecureRandom())
            }
            val client = okhttp3.OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(CbrMetalsApi::class.java)
        }

        /** Стандартный доверенный менеджер с системными корневыми УЦ Android. */
        private fun systemTrustManager(): X509TrustManager =
            trustManagerOf(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(null as KeyStore?)
            })

        /** Доверенный менеджер с корневым УЦ HARICA, встроенным в приложение. */
        private fun bundledTrustManager(context: Context): X509TrustManager {
            val certificate = context.resources.openRawResource(R.raw.harica_tls_rsa_root_ca_2021).use {
                CertificateFactory.getInstance("X.509").generateCertificate(it)
            }
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry(HARICA_ROOT_ALIAS, certificate)
            }
            return trustManagerOf(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            })
        }

        private fun trustManagerOf(factory: TrustManagerFactory): X509TrustManager =
            factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
                ?: error("Не найден X509TrustManager в ${factory.algorithm}")
    }
}

/**
 * Доверенный менеджер, принимающий сертификаты, которые прошла проверка хотя бы
 * одного из базовых менеджеров: системного (обычные публичные УЦ) и встроенного
 * в приложение (корневой УЦ HARICA для cbr.ru).
 */
private class CompositeTrustManager(
    private val delegates: List<X509TrustManager>
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        delegates.first().checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        var lastError: CertificateException? = null
        for (delegate in delegates) {
            try {
                delegate.checkServerTrusted(chain, authType)
                return
            } catch (e: CertificateException) {
                lastError = e
            }
        }
        throw lastError ?: CertificateException("Цепочка сертификатов не признана ни одним доверенным УЦ")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        delegates.flatMap { it.acceptedIssuers.toList() }.toTypedArray()
}
