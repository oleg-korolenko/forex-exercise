package forex.services.rates

object errors {

  final case class CauseError(msg: String, code: Int)

  sealed trait Error
  object Error {
    final case class OneForgeLookupRateIsToolOld(msg: String) extends Error
    final case class OneForgeLookupRateError(msg: String, cause: CauseError) extends Error
    final case class OneForgeQuotaError(msg: String, cause: CauseError) extends Error
  }

}
