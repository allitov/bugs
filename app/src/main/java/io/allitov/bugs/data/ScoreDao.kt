package io.allitov.bugs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Операции с таблицей результатов раундов. */
@Dao
interface ScoreDao {

    @Insert
    suspend fun insert(score: ScoreEntity): Long

    /** Лучшие результаты вместе с игроками: по очкам, при равенстве — по более ранней дате. */
    @Transaction
    @Query(
        "SELECT * FROM scores " +
            "ORDER BY score DESC, playedAt ASC " +
            "LIMIT :limit"
    )
    suspend fun getTopWithPlayers(limit: Int = 100): List<ScoreWithPlayer>
}
