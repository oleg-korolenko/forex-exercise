package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }
// TODO handle differently depending on the type of error
  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneForgeLookupFailed(msg) =>
      Error.RateLookupFailed(msg)
    case RatesServiceError.OneForgeLookupServerError(msg) =>
      Error.RateLookupFailed(msg)
    case RatesServiceError.OneForgeLookupClientError(msg, uri) =>
      Error.RateLookupFailed(msg)
    case RatesServiceError.OneForgeLookupUnknownError(msg, uri) =>
      Error.RateLookupFailed(msg)
    case RatesServiceError.OneForgeLookupRateIsToolOld(msg) =>
      Error.RateLookupFailed(msg)
  }
}
