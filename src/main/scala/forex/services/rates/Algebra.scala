package forex.services.rates

import forex.domain.Rate
import forex.services.rates.errors._

trait Algebra[F[_]] {
  def getRates(pair: Rate.Pair): F[RateError Either Rate]

}
