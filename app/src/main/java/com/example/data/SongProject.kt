package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_projects")
data class SongProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val durationFormatted: String,
    val format: String,
    val sampleRate: Int,
    val channels: Int,
    val originalFilePath: String,
    val polishedFilePath: String? = null,
    val masterFilePath: String? = null,
    val isPolished: Boolean = false,
    val vocalScore: Int = 80,
    val loudnessLufs: Float = -14.0f,
    val diagnosisSummary: String = "",
    val masterProfile: String = "Natural",
    val timestamp: Long = System.currentTimeMillis()
)
