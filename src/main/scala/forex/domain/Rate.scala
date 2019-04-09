package forex.domain

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

case class Quota(
    quota_used: Int,
    quota_limit: Int,
    quota_remaining: Int,
    hours_until_reset: Int
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )
}
