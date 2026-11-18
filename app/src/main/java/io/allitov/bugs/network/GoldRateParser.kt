package io.allitov.bugs.network

import android.util.Xml
import io.allitov.bugs.data.GoldRate
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.StringReader

/** Разбор XML-ответа ЦБ РФ: ищет последнюю запись о цене золота (код «1»). */
object GoldRateParser {

    /** Код золота в справочнике металлов ЦБ. */
    const val GOLD_METAL_CODE = "1"

    private const val RECORD_TAG = "Record"
    private const val BUY_TAG = "Buy"
    private const val DATE_ATTRIBUTE = "Date"
    private const val CODE_ATTRIBUTE = "Code"

    /**
     * Разбирает XML с котировками металлов и возвращает курс золота на последнюю
     * дату в документе. Если записей о золоте нет или документ повреждён
     * (например, сервер вернул HTML-страницу вместо XML), возвращает null.
     */
    fun parse(xml: String): GoldRate? {
        val parser = Xml.newPullParser()
        return try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))
            var latest: GoldRate? = null
            var code: String? = null
            var date: String? = null
            var buyPrice: Double? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        RECORD_TAG -> {
                            code = parser.getAttributeValue(null, CODE_ATTRIBUTE)
                            date = parser.getAttributeValue(null, DATE_ATTRIBUTE)
                            buyPrice = null
                        }

                        BUY_TAG -> buyPrice = parser.nextText().parsePrice()
                    }

                    XmlPullParser.END_TAG -> if (parser.name == RECORD_TAG) {
                        // Даты в ответе ЦБ в формате дд.ММ.гггг — для выбора последней
                        // записи приводим их к сравнимому виду ггггММдд
                        if (code == GOLD_METAL_CODE && buyPrice != null &&
                            (latest == null || dateKey(date) >= dateKey(latest.date))
                        ) {
                            latest = GoldRate(pricePerGram = buyPrice, date = date)
                        }
                        code = null
                        date = null
                        buyPrice = null
                    }
                }
                event = parser.next()
            }
            latest
        } catch (e: XmlPullParserException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    /** Ключ сравнения дат ЦБ: «20.10.2026» превращает в «20261020», неожиданный формат — в «». */
    private fun dateKey(date: String?): String {
        if (date == null) return ""
        val parts = date.split('.')
        return if (parts.size == 3) {
            parts[2] + parts[1].padStart(2, '0') + parts[0].padStart(2, '0')
        } else {
            ""
        }
    }

    /** Преобразует цену ЦБ («8465,42») в число, игнорируя пробелы и другие разделители. */
    private fun String.parsePrice(): Double? =
        replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
}
