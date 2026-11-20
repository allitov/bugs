package io.allitov.bugs.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import io.allitov.bugs.R
import io.allitov.bugs.data.GoldRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Виджет «шар» для рабочего стола: показывает учётный курс золота ЦБ РФ.
 *
 * Фон виджета — золотая сфера ([R.drawable.widget_ball]); данные загружаются
 * через Retrofit ([GoldRateRepository]) при добавлении виджета на рабочий стол
 * и при обновлении системой (раз в [WIDGET_UPDATE_PERIOD_MILLIS]).
 * Пока данные не загружены или сеть недоступна, показывается последний
 * сохранённый курс или заглушка «нет данных».
 */
class GoldRateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Сразу показываем последний известный курс, пока идёт загрузка
        val cachedPrice = GoldRateRepository.cachedPricePerGram(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, cachedPrice))
        }

        // Загружаем свежий курс в фоне; трансляция завершается после ответа
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val price = withTimeoutOrNull(LOAD_TIMEOUT_MILLIS) {
                    GoldRateRepository(context).loadGoldPricePerGram()
                } ?: GoldRateRepository.cachedPricePerGram(context)
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, price))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        /** Период автоматического обновления виджета системой, миллисекунды
         *  (значение также задано в res/xml/gold_rate_widget_info.xml). */
        const val WIDGET_UPDATE_PERIOD_MILLIS = 1_800_000L

        /** Таймаут загрузки курса из виджета, миллисекунды. */
        private const val LOAD_TIMEOUT_MILLIS = 8_000L

        /** Форматирует курс для показа: «8 465». */
        private fun formatPrice(price: Double): String =
            NumberFormat.getIntegerInstance(Locale.getDefault()).format(price.roundToInt())

        /** Готовит представление виджета с курсом [price] или заглушкой, если данных нет. */
        private fun buildViews(context: Context, price: Double?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)
            views.setTextViewText(
                R.id.tvGoldPrice,
                if (price != null) {
                    context.getString(R.string.widget_gold_price, formatPrice(price))
                } else {
                    context.getString(R.string.widget_gold_no_data)
                }
            )
            return views
        }

        /** Обновляет все виджеты приложения последним сохранённым курсом. */
        fun refreshAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(appContext, GoldRateWidgetProvider::class.java)
            )
            val price = GoldRateRepository.cachedPricePerGram(appContext)
            ids.forEach { id -> manager.updateAppWidget(id, buildViews(appContext, price)) }
        }
    }
}
