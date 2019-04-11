package forex.services.rates.interpreters.dummy

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneForgeRateDummy[F[_]: Applicative] extends Algebra[F] {

  override def getRates(pair: Rate.Pair): F[RateError Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[RateError].pure[F]
}
