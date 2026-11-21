package com.example.plate

import com.example.data.StolenVehicleEntity
import java.util.Locale

object PlateNormalizer {

    // Standard Saudi/Gulf + general Arabic license plate letter transliteration map
    private val arabicToLatinMap = mapOf(
        'أ' to 'A', 'إ' to 'A', 'آ' to 'A', 'ا' to 'A',
        'ب' to 'B',
        'ت' to 'T', 'ث' to 'T',
        'ج' to 'J', 'ح' to 'J', 'خ' to 'K',
        'د' to 'D', 'ذ' to 'D',
        'ر' to 'R', 'ز' to 'Z',
        'س' to 'S', 'ش' to 'S',
        'ص' to 'X', 'ض' to 'D',
        'ط' to 'T', 'ظ' to 'Z',
        'ع' to 'E', 'غ' to 'G',
        'ف' to 'F', 'ق' to 'G',
        'ك' to 'K', 'ل' to 'L',
        'م' to 'Z', 'ن' to 'N',
        'ه' to 'H', 'ة' to 'H',
        'و' to 'U',
        'ي' to 'V', 'ى' to 'V'
    )

    // Secondary general Arabic phonetic map (for non-Gulf plates e.g. Egypt/Jordan/Morocco)
    private val arabicPhoneticMap = mapOf(
        'أ' to 'A', 'إ' to 'A', 'آ' to 'A', 'ا' to 'A',
        'ب' to 'B',
        'ت' to 'T', 'ث' to 'T',
        'ج' to 'G', 'ح' to 'H', 'خ' to 'K',
        'د' to 'D', 'ذ' to 'D',
        'ر' to 'R', 'ز' to 'Z',
        'س' to 'S', 'ش' to 'S',
        'ص' to 'S', 'ض' to 'D',
        'ط' to 'T', 'ظ' to 'Z',
        'ع' to 'A', 'غ' to 'G',
        'ف' to 'F', 'ق' to 'Q',
        'ك' to 'K', 'ل' to 'L',
        'م' to 'M', 'ن' to 'N',
        'ه' to 'H', 'ة' to 'H',
        'و' to 'W',
        'ي' to 'Y', 'ى' to 'Y'
    )

    private val noiseTokens = setOf(
        "KSA", "UAE", "DUBAI", "ABUDHABI", "SHARJAH", "AJMAN", "QATAR",
        "KUWAIT", "OMAN", "BAHRAIN", "EGYPT", "JORDAN", "IRAQ", "MOROCCO",
        "TUNISIA", "ALGERIA", "LEBANON", "RIYADH", "JEDDAH", "POLICE",
        "PRIVATE", "TAXI", "EXPORT", "GCC", "USA", "EU", "GB"
    )

    /**
     * Converts Arabic-Indic and Persian digits to ASCII digits (0-9).
     */
    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                in '\u06F0'..'\u06F9' -> sb.append(('0'.code + (ch.code - '\u06F0'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Produces the canonical uppercase alphanumeric plate key (letters & digits only, no spaces).
     */
    fun normalizePlate(raw: String, usePhoneticFallback: Boolean = false): String {
        val digitsNormalized = normalizeDigits(raw.trim())
        val map = if (usePhoneticFallback) arabicPhoneticMap else arabicToLatinMap
        val sb = StringBuilder()
        for (ch in digitsNormalized) {
            when {
                ch in '0'..'9' -> sb.append(ch)
                ch in 'A'..'Z' -> sb.append(ch)
                ch in 'a'..'z' -> sb.append(ch.uppercaseChar())
                map.containsKey(ch) -> sb.append(map.getValue(ch))
            }
        }
        return sb.toString()
    }

    /**
     * Extracts only the numeric portion of a plate (helpful for quick secondary matching).
     */
    fun extractDigitsOnly(raw: String): String {
        return normalizeDigits(raw).filter { it in '0'..'9' }
    }

    /**
     * Formats a plate string cleanly for display while preserving original Arabic or Latin script.
     */
    fun formatForDisplay(raw: String): String {
        val collapsed = normalizeDigits(raw)
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        return collapsed.uppercase(Locale.US)
    }

    /**
     * Extracts likely license plate candidates from raw multi-line OCR text.
     */
    fun extractCandidates(rawOcrText: String): List<String> {
        if (rawOcrText.isBlank()) return emptyList()

        val candidates = LinkedHashSet<String>()
        val lines = rawOcrText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 1. Evaluate each cleaned line
        val cleanedLines = lines.mapNotNull { line ->
            val tokens = line.split(Regex("[\\s\\-|:•_/.]+"))
                .map { normalizePlate(it) }
                .filter { it.isNotEmpty() && it !in noiseTokens }
            if (tokens.isEmpty()) null else tokens.joinToString("")
        }

        for (lineKey in cleanedLines) {
            if (isPlausiblePlate(lineKey)) {
                candidates.add(lineKey)
            }
        }

        // 2. Evaluate adjacent line pairs (for 2-row plates: letters above digits or vice versa)
        for (i in 0 until cleanedLines.size - 1) {
            val combined = cleanedLines[i] + cleanedLines[i + 1]
            if (isPlausiblePlate(combined)) {
                candidates.add(combined)
            }
            val reversed = cleanedLines[i + 1] + cleanedLines[i]
            if (isPlausiblePlate(reversed)) {
                candidates.add(reversed)
            }
        }

        // 3. Evaluate sliding token pairs within lines
        val allTokens = rawOcrText
            .split(Regex("[\\s\\-|:•_/.,\\n]+"))
            .map { normalizePlate(it) }
            .filter { it.isNotEmpty() && it !in noiseTokens }

        for (token in allTokens) {
            if (isPlausiblePlate(token)) {
                candidates.add(token)
            }
        }
        for (i in 0 until allTokens.size - 1) {
            val pair = allTokens[i] + allTokens[i + 1]
            if (isPlausiblePlate(pair)) {
                candidates.add(pair)
            }
        }

        return candidates.toList()
    }

    /**
     * Returns true if a normalized alphanumeric string looks like a real license plate
     * (between 3 and 10 characters, containing at least 2 digits).
     */
    fun isPlausiblePlate(normalized: String): Boolean {
        if (normalized.length !in 3..10) return false
        val digitCount = normalized.count { it in '0'..'9' }
        return digitCount >= 2
    }

    /**
     * Matches recognized OCR text or manual plate input against active stolen vehicles.
     * Supports exact normalized match, swapped letters/digits order (e.g. ABC1234 vs 1234ABC),
     * Arabic Gulf/phonetic mapping, and common OCR lookalike tolerances (O/0, I/1, S/5, B/8).
     */
    fun findMatchingVehicle(
        rawText: String,
        activeStolenVehicles: List<StolenVehicleEntity>
    ): Pair<StolenVehicleEntity, String>? {
        if (rawText.isBlank() || activeStolenVehicles.isEmpty()) return null

        val directKey = normalizePlate(rawText)
        val directPhoneticKey = normalizePlate(rawText, usePhoneticFallback = true)
        val candidates = buildList {
            if (directKey.length in 3..12) add(directKey)
            if (directPhoneticKey.length in 3..12 && directPhoneticKey != directKey) add(directPhoneticKey)
            addAll(extractCandidates(rawText))
        }.distinct()

        for (vehicle in activeStolenVehicles) {
            if (vehicle.isRecovered) continue
            val targetKeys = buildTargetKeys(vehicle)
            for (candidate in candidates) {
                if (candidate in targetKeys) {
                    return vehicle to candidate
                }
                if (matchesWithOrderSwap(candidate, vehicle.plateNormalized)) {
                    return vehicle to candidate
                }
                if (matchesWithOcrTolerance(candidate, vehicle.plateNormalized)) {
                    return vehicle to candidate
                }
            }
            // Substring containment for long OCR lines (only when target plate is specific >= 5 chars)
            if (vehicle.plateNormalized.length >= 5) {
                val fullNormalized = normalizePlate(rawText)
                if (fullNormalized.contains(vehicle.plateNormalized)) {
                    return vehicle to vehicle.plateNormalized
                }
            }
        }
        return null
    }

    private fun buildTargetKeys(vehicle: StolenVehicleEntity): Set<String> {
        val primary = vehicle.plateNormalized
        val phonetic = normalizePlate(vehicle.plateDisplay, usePhoneticFallback = true)
        return setOf(primary, phonetic).filter { it.isNotEmpty() }.toSet()
    }

    private fun matchesWithOrderSwap(candidate: String, target: String): Boolean {
        if (candidate.length != target.length || candidate.length < 4) return false
        val candLetters = candidate.filter { it in 'A'..'Z' }
        val candDigits = candidate.filter { it in '0'..'9' }
        val targLetters = target.filter { it in 'A'..'Z' }
        val targDigits = target.filter { it in '0'..'9' }
        return candLetters.isNotEmpty() &&
            candDigits.isNotEmpty() &&
            candLetters == targLetters &&
            candDigits == targDigits
    }

    private fun matchesWithOcrTolerance(candidate: String, target: String): Boolean {
        if (candidate.length != target.length || target.length < 5) return false
        var mismatches = 0
        for (i in target.indices) {
            val c = candidate[i]
            val t = target[i]
            if (c != t) {
                if (areOcrConfusable(c, t)) {
                    mismatches++
                    if (mismatches > 1) return false
                } else {
                    return false
                }
            }
        }
        return mismatches == 1
    }

    private fun areOcrConfusable(a: Char, b: Char): Boolean {
        val pair = if (a < b) a to b else b to a
        return pair == ('0' to 'O') ||
            pair == ('0' to 'D') ||
            pair == ('1' to 'I') ||
            pair == ('1' to 'L') ||
            pair == ('5' to 'S') ||
            pair == ('8' to 'B') ||
            pair == ('2' to 'Z')
    }
}
