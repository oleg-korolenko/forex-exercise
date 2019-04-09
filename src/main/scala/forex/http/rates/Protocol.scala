package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.semiauto._

object Protocol {

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class ForgeConvertSuccessResponse(value: Double, text: String, timestamp: Long)

  final case class ForgeErrorMessageResponse(error: Boolean, message: String)

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val pairEncoder: Encoder[Pair] =
    deriveEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveEncoder[GetApiResponse]

  implicit val forgeApiResponseEncoder: Decoder[ForgeConvertSuccessResponse] =
    deriveDecoder[ForgeConvertSuccessResponse]

  implicit val forgeApiErrorResponseDecoder: Decoder[ForgeErrorMessageResponse] =
    deriveDecoder[ForgeErrorMessageResponse]

  implicit val forgeApiErrorResponseEncoder: Encoder[ForgeErrorMessageResponse] =
    deriveEncoder[ForgeErrorMessageResponse]

  implicit val forgeApiConvertSuccessResponseEncoder: Encoder[ForgeConvertSuccessResponse] =
    deriveEncoder[ForgeConvertSuccessResponse]

}
