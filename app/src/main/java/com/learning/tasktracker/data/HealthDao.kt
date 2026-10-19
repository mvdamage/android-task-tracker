package com.learning.tasktracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_persons ORDER BY createdAt ASC")
    fun observePersons(): Flow<List<HealthPersonEntity>>

    @Query("SELECT COUNT(*) FROM health_persons")
    suspend fun personCount(): Int

    @Query("SELECT * FROM health_persons WHERE id = :id LIMIT 1")
    suspend fun getPerson(id: Long): HealthPersonEntity?

    @Insert
    suspend fun insertPerson(person: HealthPersonEntity): Long

    @Update
    suspend fun updatePerson(person: HealthPersonEntity)

    @Delete
    suspend fun deletePerson(person: HealthPersonEntity)

    @Query("SELECT * FROM health_analyses ORDER BY status ASC, plannedDateEpochDay ASC, createdAt DESC")
    fun observeAnalyses(): Flow<List<HealthAnalysisEntity>>

    @Insert
    suspend fun insertAnalysis(analysis: HealthAnalysisEntity): Long

    @Update
    suspend fun updateAnalysis(analysis: HealthAnalysisEntity)

    @Delete
    suspend fun deleteAnalysis(analysis: HealthAnalysisEntity)

    @Query(
        """
        SELECT * FROM health_prescriptions
        WHERE archived = 0
        ORDER BY type ASC, createdAt DESC
        """
    )
    fun observeActivePrescriptions(): Flow<List<HealthPrescriptionEntity>>

    @Query("SELECT * FROM health_prescriptions WHERE id = :id LIMIT 1")
    suspend fun getPrescription(id: Long): HealthPrescriptionEntity?

    @Insert
    suspend fun insertPrescription(prescription: HealthPrescriptionEntity): Long

    @Update
    suspend fun updatePrescription(prescription: HealthPrescriptionEntity)

    @Delete
    suspend fun deletePrescription(prescription: HealthPrescriptionEntity)

    @Insert
    suspend fun insertLink(link: HealthPrescriptionTaskLinkEntity): Long

    @Query("SELECT * FROM health_prescription_task_links")
    fun observeLinks(): Flow<List<HealthPrescriptionTaskLinkEntity>>
}
