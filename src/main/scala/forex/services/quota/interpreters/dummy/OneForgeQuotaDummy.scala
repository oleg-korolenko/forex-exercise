package forex.services.quota.interpreters.dummy

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.Quota
import forex.services.quota.Algebra
import forex.services.quota.errors.QuotaError

class OneForgeQuotaDummy[F[_]: Applicative] extends Algebra[F] {
  override def getQuota: F[Either[QuotaError, Quota]] = Quota(100, 1000, 900, 10).asRight[QuotaError].pure[F]
}
