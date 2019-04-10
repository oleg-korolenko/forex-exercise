package forex.services.rates.interpreters.live

object Protocol {
  final case class ForgeConvertSuccessResponse(value: Double, text: String, timestamp: Long)
  final case class ForgeErrorMessageResponse(error: Boolean, message: String)
}
