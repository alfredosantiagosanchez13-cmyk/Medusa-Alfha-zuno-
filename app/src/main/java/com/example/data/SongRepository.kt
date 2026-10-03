package com.example.data

import kotlinx.coroutines.flow.Flow

class SongRepository(private val dao: SongProjectDao) {
    val allProjects: Flow<List<SongProject>> = dao.getAllProjects()

    suspend fun saveProject(project: SongProject): Long {
        return dao.insertProject(project)
    }

    suspend fun updateProject(project: SongProject) {
        dao.updateProject(project)
    }

    suspend fun getProject(id: Long): SongProject? {
        return dao.getProjectById(id)
    }

    suspend fun deleteProject(id: Long) {
        dao.deleteProjectById(id)
    }
}
