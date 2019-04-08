package forex.domain

import java.time.{ Instant, OffsetDateTime, ZoneId }

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  def fromUtcTimestamp(ts: Long) = Timestamp(Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toOffsetDateTime)

}
