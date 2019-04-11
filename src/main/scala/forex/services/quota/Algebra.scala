package forex.services.quota

import forex.domain.Quota
import forex.services.quota.errors.QuotaError

trait Algebra[F[_]] {
  def getQuota: F[QuotaError Either Quota]
}
