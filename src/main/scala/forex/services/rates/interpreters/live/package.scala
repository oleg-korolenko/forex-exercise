package forex.services.rates.interpreters

import forex.domain.Currency
import forex.domain.Currency.show
import forex.services.rates.interpreters.live.Protocol.{ ForgeConvertSuccessResponse, ForgeErrorMessageResponse }
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import org.http4s.{ QueryParam, QueryParamEncoder, QueryParameterKey, QueryParameterValue }

/**
  * Created by okorolenko on 2019-04-10.
  */
package object live {

  implicit val currencyQueryParam: QueryParamEncoder[Currency] =
    (value: Currency) => QueryParameterValue(show.show(value))

  object ForgeFromQueryParam extends QueryParam[Currency] {
    override def key: QueryParameterKey = QueryParameterKey("from")
  }
  object ForgeToQueryParam extends QueryParam[Currency] {
    override def key: QueryParameterKey = QueryParameterKey("to")
  }

  implicit val forgeApiResponseEncoder: Decoder[ForgeConvertSuccessResponse] =
    deriveDecoder[ForgeConvertSuccessResponse]

  implicit val forgeApiErrorResponseDecoder: Decoder[ForgeErrorMessageResponse] =
    deriveDecoder[ForgeErrorMessageResponse]

  implicit val forgeApiErrorResponseEncoder: Encoder[ForgeErrorMessageResponse] =
    deriveEncoder[ForgeErrorMessageResponse]

  implicit val forgeApiConvertSuccessResponseEncoder: Encoder[ForgeConvertSuccessResponse] =
    deriveEncoder[ForgeConvertSuccessResponse]
}
