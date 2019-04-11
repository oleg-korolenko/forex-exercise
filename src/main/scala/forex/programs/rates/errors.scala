package forex.programs.rates

import forex.services.rates.errors.{ RateError => RatesServiceError }
import forex.services.quota.errors.{ QuotaError => QuotaServiceError }

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

  def rateErrorToProgramError(error: RatesServiceError): Error = error match {

    case RatesServiceError.OneForgeLookupRateError(msg, _) =>
      Error.RateLookupFailed(msg)

    case RatesServiceError.OneForgeLookupRateIsToolOld(msg) =>
      Error.RateIsTooOldLookupFailed(msg)

  }
  def quotaErrorToProgramError(error: QuotaServiceError): Error = error match {
    case QuotaServiceError.OneForgeQuotaError(msg, _) =>
      Error.QuotaLookupFailed(msg)

  }
}
