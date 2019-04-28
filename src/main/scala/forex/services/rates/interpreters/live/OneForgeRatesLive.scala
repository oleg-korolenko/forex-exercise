package forex.services.rates.interpreters.live

import cats.effect.Sync
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.errors.CauseError
import forex.services.rates.Algebra
import forex.services.rates.errors.RateError
import forex.services.rates.interpreters.live.Protocol.{ ForgeRateErrorResponse, ForgeRateSuccessResponse }
import org.http4s.Status.Successful
import org.http4s.Uri
import org.http4s.client.Client

class OneForgeRatesLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  private val baseUri = Uri.fromString(s"${config.host.show}/${config.version.show}").toOption.get

  override def getRates(pair: Rate.Pair): F[RateError Either Rate] = {

    val uriToCall = baseUri / "convert" +? ("from", pair.from) +? ("to", pair.to) +? ("api_key", config.apiKey) +? ("quantity", 1)

    client
      .get[RateError Either Rate](uriToCall) {
        case Successful(resp) =>
          resp
            .as[ForgeRateSuccessResponse]
            .flatMap(
              r => {
                val ts = Timestamp.fromUtcTimestamp(r.timestamp)
                Rate(pair, Price(r.value), ts).asRight[RateError].pure[F]
              }
            )
            // could be still an error thanks to the Forge API (f.i missing API key)
            .recoverWith {
              case _ =>
                resp
                  .as[ForgeRateErrorResponse]
                  .flatMap(errResp => {
                    val err: RateError = RateError.OneForgeLookupRateError(
                      "Unable to retrieve rate",
                      CauseError(errResp.message, resp.status.code)
                    )
                    err.asLeft[Rate].pure[F]
                  })

            }
        case resp =>
          resp
            .as[String]
            .map(cause => {
              val err: RateError =
                RateError.OneForgeLookupRateError("Unable to retrieve rate", CauseError(cause, resp.status.code))
              err.asLeft[Rate]
            })
      }

  }
}
