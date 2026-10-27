package io.allitov.bugs.data

import androidx.room.Embedded
import androidx.room.Relation

/** Результат раунда вместе с данными игрока — для вкладки «Рекорды». */
data class ScoreWithPlayer(
    @Embedded val score: ScoreEntity,
    @Relation(parentColumn = "playerId", entityColumn = "id")
    val player: PlayerEntity
)
