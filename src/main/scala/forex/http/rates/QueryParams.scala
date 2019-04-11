package forex.http.rates

import forex.domain.Currency
import org.http4s.QueryParamDecoder
import org.http4s.QueryParamDecoder.fromUnsafeCast
import org.http4s.dsl.impl.{ QueryParamDecoderMatcher, ValidatingQueryParamDecoderMatcher }

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    fromUnsafeCast[Currency](s => Currency.fromString(s.value))("Currency")

//  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")

  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

  object QuantityQueryParam extends QueryParamDecoderMatcher[Int]("quantity")

}
