package forex.services.quota

object Protocol {
  final case class ForgeRateSuccessResponse(value: Double, text: String, timestamp: Long)
  final case class ForgeRateErrorResponse(error: Boolean, message: String)
}
