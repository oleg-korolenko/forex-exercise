package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Quota, Rate, Timestamp }
import forex.services.rates.errors._

class OneForgeDummy[F[_]: Applicative] extends Algebra[F] {

  override def getRates(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]

  override def getQuota: F[Either[Error, Quota]] = Quota(100, 1000, 900, 10).asRight[Error].pure[F]
}
