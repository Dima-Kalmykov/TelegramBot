package solution.utils

import java.time.LocalDateTime

object Utils {

    /**
     * Convert text to seconds.
     */
    fun getSeconds(text: String): Long {
        val values = text.split(' ')
        val updatedValues = values.map { it.toLong() }
        return updatedValues[0] * 30 * 24 * 3600 +
                updatedValues[1] * 24 * 3600
    }

    /**
     * Check if the string representation of text if valid.
     */
    fun isValidPeriod(text: String): Boolean {
        if (text == "") {
            return true
        }

        val values = text.split(" ")
        if (values.size != 2) {
            return false
        }

        var updatedValues = listOf<Int>()
        try {
            updatedValues = values.map { Integer.parseInt(it) }
        } catch (ex: Exception) {
            return false
        }
        val month = updatedValues[0]
        val day = updatedValues[1]

        if (month + day == 0) {
            return false
        }
        if (month < 0 || month > 12) {
            return false
        }

        if (day < 0 || day > 30) {
            return false
        }

        return true
    }

    /**
     * Check two dates for equality.
     */
    fun dateAreEqual(date1: LocalDateTime, date2: LocalDateTime): Boolean {
        return date1.year == date2.year &&
                date1.month == date2.month &&
                date1.dayOfMonth == date2.dayOfMonth &&
                date1.hour == date2.hour &&
                date1.minute == date2.minute
    }
}