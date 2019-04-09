package forex.domain

import java.time.temporal.ChronoUnit
import java.time.{ Instant, OffsetDateTime, ZoneId }

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  def fromUtcTimestamp(ts: Long) = Timestamp(Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toOffsetDateTime)

  def isOlderThan(thresholdInSecs: Int) =
    (tsToCheck: Timestamp) => Timestamp.now.value.minus(thresholdInSecs, ChronoUnit.SECONDS).isAfter(tsToCheck.value)

}
