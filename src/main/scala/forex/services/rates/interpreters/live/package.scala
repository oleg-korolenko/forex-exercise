package forex.services.rates.interpreters

import forex.domain.{ Currency, Quota }
import forex.domain.Currency.show
import forex.services.rates.interpreters.live.Protocol.{ ForgeRateErrorResponse, ForgeRateSuccessResponse }
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

  implicit val forgeApiConvertSuccessResponseEncoder: Encoder[ForgeRateSuccessResponse] =
    deriveEncoder[ForgeRateSuccessResponse]

  implicit val forgeApiResponseEncoder: Decoder[ForgeRateSuccessResponse] =
    deriveDecoder[ForgeRateSuccessResponse]

  implicit val forgeApiErrorResponseDecoder: Decoder[ForgeRateErrorResponse] =
    deriveDecoder[ForgeRateErrorResponse]

  implicit val forgeApiErrorResponseEncoder: Encoder[ForgeRateErrorResponse] =
    deriveEncoder[ForgeRateErrorResponse]

}
