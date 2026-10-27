package io.allitov.bugs.data

import android.content.Context

/** Текущий выбранный игрок: хранится в памяти и запоминается между запусками приложения. */
object PlayerSession {

    private const val PREFS_NAME = "player_session"
    private const val KEY_LAST_PLAYER_ID = "last_player_id"

    var current: PlayerEntity? = null

    /** Запоминает id выбранного игрока (или сбрасывает его, если передан null). */
    fun persist(context: Context, player: PlayerEntity?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (player == null) {
            prefs.edit().remove(KEY_LAST_PLAYER_ID).apply()
        } else {
            prefs.edit().putLong(KEY_LAST_PLAYER_ID, player.id).apply()
        }
    }

    /** Возвращает id последнего выбранного игрока или null, если его ещё не выбирали. */
    fun loadLastPlayerId(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAST_PLAYER_ID)) return null
        return prefs.getLong(KEY_LAST_PLAYER_ID, 0L)
    }
}
