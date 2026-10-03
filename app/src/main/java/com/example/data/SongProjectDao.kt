package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongProjectDao {
    @Query("SELECT * FROM song_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<SongProject>>

    @Query("SELECT * FROM song_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): SongProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SongProject): Long

    @Update
    suspend fun updateProject(project: SongProject)

    @Query("DELETE FROM song_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("DELETE FROM song_projects")
    suspend fun clearAll()
}
