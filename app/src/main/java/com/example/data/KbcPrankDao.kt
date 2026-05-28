package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KbcPrankDao {
    @Query("SELECT * FROM kbc_pranks ORDER BY timestamp DESC")
    fun getAllPranks(): Flow<List<KbcPrank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrank(prank: KbcPrank)

    @Query("DELETE FROM kbc_pranks WHERE id = :id")
    suspend fun deletePrankById(id: Int)

    @Query("DELETE FROM kbc_pranks")
    suspend fun clearAllPranks()
}
