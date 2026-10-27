package io.allitov.bugs.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Зарегистрированный игрок. */
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val gender: String,
    val course: Int,
    val difficulty: String,
    val birthDate: String,
    val zodiacSign: String,
    val createdAt: Long = System.currentTimeMillis()
)
