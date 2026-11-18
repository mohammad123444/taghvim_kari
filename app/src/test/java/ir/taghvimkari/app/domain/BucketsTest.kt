package ir.taghvimkari.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BucketsTest {

    // 2026-11-14 is a Saturday.
    private val saturday = LocalDate.of(2026, 11, 14)

    private fun person(
        pattern: Pattern,
        category: Category = Category.OPERATIONAL,
        name: String = "x",
        id: String = name,
        leader: Boolean = false,
        leaderId: String? = null
    ) = Person(
        id = id, name = name, workType = WorkTypes.default, employment = Employment.OFFICIAL,
        pattern = pattern, startDate = saturday, startWeekday = 0,
        isLeader = leader, leaderId = leaderId, category = category
    )

    @Test fun administrativeStaffOnlyHaveTwoStates() {
        val p = person(Pattern.WORK_REST, Category.ADMIN)
        assertEquals(Bucket.DAY, bucketOf(p, ShiftEngine.status(p, saturday)))
        assertEquals(Bucket.REST, bucketOf(p, ShiftEngine.status(p, saturday.plusDays(8))))
    }

    @Test fun operationalRestIsSplitIntoFirstAndSecondWeek() {
        val p = person(Pattern.DAY_NIGHT_REST2) // 1 week day, 1 week night, 2 weeks rest
        assertEquals(Bucket.DAY, bucketOf(p, ShiftEngine.status(p, saturday)))
        assertEquals(Bucket.NIGHT, bucketOf(p, ShiftEngine.status(p, saturday.plusDays(7))))
        assertEquals(Bucket.REST_WEEK1, bucketOf(p, ShiftEngine.status(p, saturday.plusDays(14))))
        assertEquals(Bucket.REST_WEEK1, bucketOf(p, ShiftEngine.status(p, saturday.plusDays(20))))
        assertEquals(Bucket.REST_WEEK2, bucketOf(p, ShiftEngine.status(p, saturday.plusDays(21))))
    }

    @Test fun notStartedPeopleGetTheirOwnBucket() {
        val p = person(Pattern.WORK_REST)
        assertEquals(Bucket.NOT_STARTED, bucketOf(p, ShiftEngine.status(p, saturday.minusDays(2))))
    }

    @Test fun teamsAreListedLeaderFirstThenMembers() {
        val leader = person(Pattern.WORK_REST, name = "ب", id = "L", leader = true)
        val member = person(Pattern.WORK_REST, name = "الف", id = "M", leaderId = "L")
        val single = person(Pattern.WORK_REST, name = "پ", id = "S")
        val sorted = listOf(member, single, leader).sortedByTeam(mapOf("L" to "ب")) { it }
        assertEquals(listOf("L", "M", "S"), sorted.map { it.id })
    }
}
