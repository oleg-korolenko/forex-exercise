package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneForgeLookupFailed(msg: String) extends Error
    final case class OneForgeLookupClientError(msg: String ) extends Error
    final case class OneForgeLookupServerError(msg: String ) extends Error
    final case class OneForgeLookupUnknownError(msg: String ) extends Error
    final case class OneForgeLookupRateIsToolOld(msg: String) extends Error

  }

}
