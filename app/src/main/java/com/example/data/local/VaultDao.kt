package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_tracks ORDER BY downloadedAt DESC")
    fun observeVaultTracks(): Flow<List<VaultTrackEntity>>

    @Query("SELECT * FROM vault_tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): VaultTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: VaultTrackEntity)

    @Query("DELETE FROM vault_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 12")
    fun observeRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearch(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)
}
