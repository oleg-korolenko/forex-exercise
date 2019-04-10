package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception {
    val msg: String
  }
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class RateIsTooOldLookupFailed(msg: String) extends Error
    final case class QuotaLookupFailed(msg: String) extends Error
    final case class QuotaLimit(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {

    case RatesServiceError.OneForgeLookupRateError(msg, _) =>
      Error.RateLookupFailed("Failed to get the rate due to an error")

    case RatesServiceError.OneForgeLookupRateIsToolOld(msg) =>
      Error.RateIsTooOldLookupFailed(msg)

    case RatesServiceError.OneForgeQuotaError(msg, _) =>
      Error.QuotaLookupFailed(msg)

  }
}
