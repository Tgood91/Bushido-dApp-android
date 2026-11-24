package io.github.tgood91.bushidodapp

data class Virtue(val kanji: String, val name: String, val line: String)

/** The eight virtues, one a day. Plain Kotlin/JVM. */
object Virtues {
    val ALL = listOf(
        Virtue("義", "Gi — Rectitude", "Do the right thing because it is right, not because anyone is watching."),
        Virtue("勇", "Yu — Courage", "Act despite fear. A trade, a decision, a hard conversation — move anyway."),
        Virtue("仁", "Jin — Benevolence", "Strength exists to protect. Use what you build for the people around you."),
        Virtue("礼", "Rei — Respect", "Treat every counterparty, builder and beginner with courtesy."),
        Virtue("誠", "Makoto — Honesty", "Say only what is true. When you are wrong, say that too."),
        Virtue("名誉", "Meiyo — Honour", "Your word is your bond. Reputation compounds longer than capital."),
        Virtue("忠義", "Chugi — Loyalty", "Stand by your people and your principles when it costs you something."),
        Virtue("自制", "Jisei — Self-Control", "Master the impulse. Size the position, set the stop, walk away clean."),
    )

    /**
     * The virtue for local epoch day [day] (see [CheckInStore.localEpochDay]); it changes at local midnight,
     * the same moment check-ins roll over. Unlike LocalDate.dayOfYear it never skips or repeats at New Year.
     */
    fun ofTheDay(day: Long = CheckInStore.localEpochDay()): Virtue = ALL[Math.floorMod(day, ALL.size.toLong()).toInt()]
}
