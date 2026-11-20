package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 5")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentSearchEntity)

    @Query(
        "DELETE FROM recent_searches WHERE code NOT IN " +
            "(SELECT code FROM recent_searches ORDER BY searchedAt DESC LIMIT 5)"
    )
    suspend fun trimToLastFive()

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
