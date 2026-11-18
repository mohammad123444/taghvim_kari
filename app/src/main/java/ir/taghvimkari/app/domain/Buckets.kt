package ir.taghvimkari.app.domain

/**
 * The groups used in reports. Administrative staff only have two states (روزکار / استراحت);
 * operational staff are split into day, night and the first/second rest week.
 */
enum class Bucket(val title: String) {
    DAY("روزکار"),
    NIGHT("شب‌کار"),
    REST("استراحت"),
    REST_WEEK1("هفته اول استراحت"),
    REST_WEEK2("هفته دوم استراحت"),
    NOT_STARTED("هنوز شروع نشده");

    val working: Boolean get() = this == DAY || this == NIGHT
}

fun bucketOf(person: Person, status: DayStatus): Bucket {
    if (person.category == Category.ADMIN) {
        return if (status.working) Bucket.DAY else Bucket.REST
    }
    return when {
        !status.started -> Bucket.NOT_STARTED
        status.working -> if (status.shift == Shift.NIGHT) Bucket.NIGHT else Bucket.DAY
        status.restWeek == 1 -> Bucket.REST_WEEK1
        status.restWeek >= 2 -> Bucket.REST_WEEK2
        else -> Bucket.REST
    }
}

/**
 * Lists everywhere show a team together: the shift leader first, immediately followed by
 * his members. People without a team come last.
 */
fun <T> List<T>.sortedByTeam(leaderNames: Map<String, String>, person: (T) -> Person): List<T> =
    sortedWith(
        compareBy<T>(
            { val p = person(it); if (p.isLeader || p.leaderId != null) 0 else 1 },
            { val p = person(it); if (p.isLeader) p.name else (p.leaderId?.let { id -> leaderNames[id] } ?: p.name) },
            { val p = person(it); p.leaderId ?: p.id },
            { if (person(it).isLeader) 0 else 1 },
            { person(it).name }
        )
    )
