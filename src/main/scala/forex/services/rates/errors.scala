package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneForgeLookupFailed(msg: String) extends Error

    final case class OneForgeLookupRateIsToolOld(msg: String) extends Error
    final case class OneForgeLookupRateError(msg: String, errCode: Int) extends Error

    final case class OneForgeQuotaError(msg: String, errCode: Int) extends Error
  }

}
