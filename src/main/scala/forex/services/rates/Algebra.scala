package forex.services.rates

import forex.domain.{ Quota, Rate }
import errors._

trait Algebra[F[_]] {
  def getRates(pair: Rate.Pair): F[Error Either Rate]
  def getQuota: F[Error Either Quota]

}
