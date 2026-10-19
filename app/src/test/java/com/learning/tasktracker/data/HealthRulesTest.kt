package com.learning.tasktracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthRulesTest {

    @Test
    fun maxPersons_allowsThreeOnly() {
        assertTrue(HealthRules.canAddPerson(0))
        assertTrue(HealthRules.canAddPerson(2))
        assertFalse(HealthRules.canAddPerson(3))
        assertEquals(3, HealthRules.MAX_PERSONS)
    }

    @Test
    fun taskTitle_byType() {
        assertEquals(
            "Приём: Амоксициллин",
            HealthRules.taskTitleFor(HealthPrescriptionType.MEDICATION, "Амоксициллин")
        )
        assertEquals(
            "Визит: Терапевт",
            HealthRules.taskTitleFor(HealthPrescriptionType.VISIT, "Терапевт")
        )
    }

    @Test
    fun dueDateFromDaysAhead_rejectsNonPositive() {
        val today = 20_000L
        assertNull(HealthRules.dueDateFromDaysAhead(0, today))
        assertNull(HealthRules.dueDateFromDaysAhead(-1, today))
        assertEquals(20_003L, HealthRules.dueDateFromDaysAhead(3, today))
    }

    @Test
    fun normalizePersonName_trimsAndRejectsEmpty() {
        assertNull(HealthRules.normalizePersonName("   "))
        assertEquals("Марина", HealthRules.normalizePersonName("  Марина  "))
    }
}
