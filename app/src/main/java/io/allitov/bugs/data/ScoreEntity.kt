package io.allitov.bugs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Результат завершённого раунда, привязанный к игроку. */
@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId")]
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val score: Int,
    val hits: Int,
    val misses: Int,
    val difficulty: String,
    val roundDurationSeconds: Int = 120,
    val playedAt: Long = System.currentTimeMillis()
)
