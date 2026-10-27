package io.allitov.bugs.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/** Операции с таблицей игроков. */
@Dao
interface PlayerDao {

    @Insert
    suspend fun insert(player: PlayerEntity): Long

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("SELECT * FROM players ORDER BY fullName")
    suspend fun getAll(): List<PlayerEntity>
}
