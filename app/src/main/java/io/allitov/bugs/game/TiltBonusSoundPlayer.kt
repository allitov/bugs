package io.allitov.bugs.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.allitov.bugs.R

/**
 * Звуковое сопровождение бонуса наклона на базе [SoundPool].
 *
 * [playPickup] звучит в момент нажатия на бонус, [startLoop] запускает
 * зацикленный «ветер» на всё время действия режима наклона.
 *
 * Пул создаётся лениво, поэтому после [release] класс можно использовать снова:
 * при следующем вызове пул пересоздастся и звуки загрузятся заново.
 */
class TiltBonusSoundPlayer(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var pickupSoundId = 0
    private var loopSoundId = 0
    private var loopStreamId = 0
    private val loadedSoundIds = mutableSetOf<Int>()

    private fun pool(): SoundPool {
        soundPool?.let { return it }
        val pool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool.setOnLoadCompleteListener { source, sampleId, status ->
            // Пул мог быть пересоздан, пока шли загрузки прежнего экземпляра
            if (source === soundPool && status == 0) {
                loadedSoundIds += sampleId
            }
        }
        soundPool = pool
        loadedSoundIds.clear()
        pickupSoundId = pool.load(context, R.raw.tilt_bonus_pickup, 1)
        loopSoundId = pool.load(context, R.raw.tilt_bonus_loop, 1)
        return pool
    }

    /** Играет короткий сигнал подбора бонуса. */
    fun playPickup() {
        val pool = pool()
        if (pickupSoundId in loadedSoundIds) {
            pool.play(pickupSoundId, PICKUP_VOLUME, PICKUP_VOLUME, PICKUP_PRIORITY, 0, 1f)
        }
    }

    /** Запускает зацикленный звук действия бонуса. Повторный вызов ничего не меняет. */
    fun startLoop() {
        if (loopStreamId != 0) return
        val pool = pool()
        if (loopSoundId in loadedSoundIds) {
            loopStreamId = pool.play(loopSoundId, LOOP_VOLUME, LOOP_VOLUME, LOOP_PRIORITY, -1, 1f)
        }
    }

    /** Останавливает зацикленный звук действия бонуса. */
    fun stopLoop() {
        if (loopStreamId != 0) {
            soundPool?.stop(loopStreamId)
            loopStreamId = 0
        }
    }

    /** Полностью освобождает пул звуков. */
    fun release() {
        stopLoop()
        soundPool?.release()
        soundPool = null
        loadedSoundIds.clear()
    }

    private companion object {
        const val MAX_STREAMS = 2
        const val PICKUP_VOLUME = 0.9f
        const val LOOP_VOLUME = 0.6f
        const val PICKUP_PRIORITY = 1
        const val LOOP_PRIORITY = 0
    }
}
