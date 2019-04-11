package forex.services.rates

import forex.services.errors.CauseError

object errors {
  sealed trait RateError
  object RateError {
    final case class OneForgeLookupRateIsToolOld(msg: String) extends RateError
    final case class OneForgeLookupRateError(msg: String, cause: CauseError) extends RateError
  }

}
