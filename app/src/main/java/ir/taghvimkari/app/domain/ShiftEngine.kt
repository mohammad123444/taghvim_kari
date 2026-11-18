package ir.taghvimkari.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ShiftEngine {

    /** Java DayOfWeek: Monday=1 ... Sunday=7. Persian weekday: Saturday=0 ... Friday=6. */
    fun persianWeekday(date: LocalDate): Int = (date.dayOfWeek.value + 1) % 7

    fun shift(person: Person, date: LocalDate, officialHolidays: Set<LocalDate> = emptySet()): Shift {
        if (person.pattern == Pattern.OFFICIAL_DAYS) {
            val isWorkday = persianWeekday(date) in 0..4 && date !in officialHolidays
            return if (isWorkday) Shift.WORK else Shift.REST
        }
        // The cycle starts on the first [startWeekday] on or after the chosen start date.
        val start = person.startDate
        val anchor = start.plusDays(((person.startWeekday - persianWeekday(start) + 7) % 7).toLong())
        val days = ChronoUnit.DAYS.between(anchor, date)
        if (days < 0) return Shift.REST
        val week = ((days / 7) % person.pattern.cycleWeeks).toInt()
        return when (person.pattern) {
            Pattern.DAY2_REST1 -> if (week < 2) Shift.DAY else Shift.REST
            Pattern.DAY_NIGHT_REST -> when (week) { 0 -> Shift.DAY; 1 -> Shift.NIGHT; else -> Shift.REST }
            Pattern.WORK_REST -> if (week == 0) Shift.WORK else Shift.REST
            Pattern.DAY_NIGHT_REST2 -> when (week) { 0 -> Shift.DAY; 1 -> Shift.NIGHT; else -> Shift.REST }
            Pattern.OFFICIAL_DAYS -> Shift.REST
        }
    }

    fun isWorking(person: Person, date: LocalDate): Boolean = shift(person, date) != Shift.REST

    /** Days in [from]..[to] (inclusive) on which the person is not resting. */
    fun workingDays(person: Person, from: LocalDate, to: LocalDate): List<Pair<LocalDate, Shift>> {
        val result = ArrayList<Pair<LocalDate, Shift>>()
        var d = from
        while (!d.isAfter(to)) {
            val s = shift(person, d)
            if (s != Shift.REST) result.add(d to s)
            d = d.plusDays(1)
        }
        return result
    }
}
