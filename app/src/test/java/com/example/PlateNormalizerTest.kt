package com.example

import com.example.data.StolenVehicleEntity
import com.example.plate.PlateNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlateNormalizerTest {

    private fun vehicle(plate: String, recovered: Boolean = false): StolenVehicleEntity {
        return StolenVehicleEntity(
            id = 1L,
            plateDisplay = PlateNormalizer.formatForDisplay(plate),
            plateNormalized = PlateNormalizer.normalizePlate(plate),
            plateDigits = PlateNormalizer.extractDigitsOnly(plate),
            makeModel = "تويوتا كامري",
            color = "أبيض",
            year = "2023",
            caseNumber = "1044",
            notes = "",
            isRecovered = recovered
        )
    }

    @Test
    fun `normalizes Arabic digits and Gulf Arabic letters to canonical key`() {
        assertEquals("ABJ1234", PlateNormalizer.normalizePlate("أ ب ح ١٢٣٤"))
        assertEquals("ABJ1234", PlateNormalizer.normalizePlate("a b j 1234"))
    }

    @Test
    fun `matches stolen vehicle from multiline OCR with country noise`() {
        val stolen = listOf(vehicle("أ ب ح 4821"))
        val ocrOutput = """
            KSA
            4821
            ABJ
        """.trimIndent()

        val match = PlateNormalizer.findMatchingVehicle(ocrOutput, stolen)
        assertNotNull(match)
        assertEquals(1L, match?.first?.id)
    }

    @Test
    fun `matches with single OCR lookalike confusion`() {
        val stolen = listOf(vehicle("RSL 5082"))
        val match = PlateNormalizer.findMatchingVehicle("RSL 5O82", stolen)
        assertNotNull(match)
        assertEquals(1L, match?.first?.id)
    }

    @Test
    fun `ignores recovered vehicles during matching`() {
        val recovered = listOf(vehicle("ABC 9912", recovered = true))
        val match = PlateNormalizer.findMatchingVehicle("ABC 9912", recovered)
        assertNull(match)
    }
}
