package forex.services.quota

import forex.services.errors.CauseError

object errors {
  sealed trait QuotaError
  object QuotaError {
    final case class OneForgeQuotaError(msg: String, cause: CauseError) extends QuotaError
  }

}
