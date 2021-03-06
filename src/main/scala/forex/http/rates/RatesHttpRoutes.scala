package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.{ QuotaLimit, QuotaLookupFailed, RateLookupFailed }
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import io.circe.syntax._
import Converters.ConverterToApiErrorSyntax._

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromValidated) +& ToQueryParam(toValidated) =>
      fromValidated.fold(
        _ => BadRequest("Unable to parse argument [from]".asGetApiError.asJson),
        from =>
          toValidated.fold(
            _ => BadRequest("Unable to parse argument [to]".asGetApiError.asJson),
            to =>
              rates
                .get(RatesProgramProtocol.GetRatesRequest(from, to))
                .flatMap {
                  case Right(rate) => Ok(rate.asGetApiResponse)
                  case Left(err) =>
                    err match {
                      case RateLookupFailed(_) | QuotaLookupFailed(_) => InternalServerError(err.asGetApiError)
                      case QuotaLimit(_)                              => Forbidden(err.asGetApiError)
                    }
              }
        )
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
