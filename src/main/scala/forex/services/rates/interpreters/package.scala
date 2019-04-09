package forex.services.rates

import cats.effect.Sync
import forex.domain.Currency
import forex.domain.Currency.show
import io.circe.generic.extras.decoding.{ EnumerationDecoder, UnwrappedDecoder }
import io.circe.generic.extras.encoding.{ EnumerationEncoder, UnwrappedEncoder }
import io.circe.{ Decoder, Encoder }
import org.http4s.circe._
import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  QueryParam,
  QueryParamEncoder,
  QueryParameterKey,
  QueryParameterValue
}

package object interpreters {

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A] = implicitly
  implicit def enumDecoder[A: EnumerationDecoder]: Decoder[A] = implicitly

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Sync]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder, F[_]: Sync]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  implicit val currencyQueryParam: QueryParamEncoder[Currency] =
    (value: Currency) => QueryParameterValue(show.show(value))

  object ForgeFromQueryParam extends QueryParam[Currency] {
    override def key: QueryParameterKey = QueryParameterKey("from")
  }
  object ForgeToQueryParam extends QueryParam[Currency] {
    override def key: QueryParameterKey = QueryParameterKey("to")
  }
}
