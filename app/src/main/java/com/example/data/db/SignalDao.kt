package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY timestampMillis DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE userResult = :result ORDER BY timestampMillis DESC")
    fun getSignalsByResult(result: String): Flow<List<SignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity): Long

    @Query("UPDATE signals SET userResult = :result WHERE id = :id")
    suspend fun updateSignalResult(id: Long, result: String)

    @Query("DELETE FROM signals WHERE id = :id")
    suspend fun deleteSignalById(id: Long)

    @Query("DELETE FROM signals")
    suspend fun clearAllSignals()

    @Query("SELECT COUNT(*) FROM signals WHERE userResult = 'WIN'")
    fun getWinCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM signals WHERE userResult = 'LOSS'")
    fun getLossCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM signals")
    fun getTotalCount(): Flow<Int>
}
