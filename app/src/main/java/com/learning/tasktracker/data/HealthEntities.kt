package com.learning.tasktracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "health_persons")
data class HealthPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "health_analyses",
    foreignKeys = [
        ForeignKey(
            entity = HealthPersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class HealthAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val title: String,
    val status: HealthAnalysisStatus = HealthAnalysisStatus.PLANNED,
    val plannedDateEpochDay: Long? = null,
    val doneDateEpochDay: Long? = null,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "health_prescriptions",
    foreignKeys = [
        ForeignKey(
            entity = HealthPersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class HealthPrescriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: HealthPrescriptionType,
    val title: String,
    val dosage: String = "",
    val comment: String = "",
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null,
    val visitDateEpochDay: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "health_prescription_task_links",
    foreignKeys = [
        ForeignKey(
            entity = HealthPrescriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("prescriptionId"), Index("taskId")]
)
data class HealthPrescriptionTaskLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prescriptionId: Long,
    val taskId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
