package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.{ QuotaLimit, QuotaLookupFailed, RateIsTooOldLookupFailed, RateLookupFailed }
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates
        .get(RatesProgramProtocol.GetRatesRequest(from, to))
        //.flatMap(Sync[F].fromEither)
        .flatMap {
          case Right(rate) => Ok(rate.asGetApiResponse)
          case Left(err) =>
            err match {
              case RateLookupFailed(_) | QuotaLookupFailed(_) => InternalServerError(err.asGetApiError)
              case QuotaLimit(_)                              => Forbidden(err.asGetApiError)
              case RateIsTooOldLookupFailed(_)                => Accepted(err.asGetApiError)
            }
        }

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
