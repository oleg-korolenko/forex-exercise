package forex.services.rates.interpreters.live

import cats.effect.Sync
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.{ Price, Quota, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors.{ CauseError, Error }
import forex.services.rates.interpreters._
import forex.services.rates.interpreters.live.Protocol.{ ForgeConvertSuccessResponse, ForgeErrorMessageResponse }
import org.http4s.Status.Successful
import org.http4s.Uri
import org.http4s.client.Client

class OneForgeLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  private val baseUri      = Uri.fromString(s"${config.host.show}/${config.version.show}").toOption.get
  private val isRateTooOld = Timestamp.isOlderThan(config.oldRateThresholdInSecs)

  override def getRates(pair: Rate.Pair): F[Error Either Rate] = {

    val uriToCall = baseUri / "convert" +? ("from", pair.from) +? ("to", pair.to) +? ("api_key", config.apiKey) +? ("quantity", 1)

    client
      .get[Error Either Rate](uriToCall) {
        case Successful(resp) =>
          resp
            .as[ForgeConvertSuccessResponse]
            .flatMap(
              r => {
                val ts = Timestamp.fromUtcTimestamp(r.timestamp)
                if (isRateTooOld(ts)) {
                  val err: Error = Error.OneForgeLookupRateIsToolOld(s"Rate is too old: ${ts.value.toString}")
                  err.asLeft[Rate].pure[F]
                } else Rate(pair, Price(r.value), ts).asRight[Error].pure[F]
              }
            )
            // could be still an error thanks to the Forge API (f.i missing API key)
            .recoverWith {
              case _ =>
                resp
                  .as[ForgeErrorMessageResponse]
                  .flatMap(errResp => {
                    val err: Error = Error.OneForgeLookupRateError(
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
              val err: Error =
                Error.OneForgeLookupRateError("Unable to retrieve rate", CauseError(cause, resp.status.code))
              err.asLeft[Rate]
            })
      }

  }

  override def getQuota: F[Either[Error, Quota]] = {
    val uriToCall = baseUri / "quota" +? ("api_key", config.apiKey)

    client
      .get[Error Either Quota](uriToCall) {

        case Successful(resp) =>
          resp
            .as[Quota]
            .flatMap(_.asRight[Error].pure[F])
            // could be still an error thanks to the Forge API (f.i missing API key)
            .recoverWith {
              case _ =>
                resp
                  .as[ForgeErrorMessageResponse]
                  .flatMap(errResp => {
                    val err: Error =
                      Error.OneForgeQuotaError("Unable to retrieve quota", CauseError(errResp.message, 200))
                    err.asLeft[Quota].pure[F]
                  })

            }
        case resp =>
          resp
            .as[String]
            .map(cause => {
              val err: Error = Error.OneForgeQuotaError("Unable to retrieve quota", CauseError(cause, resp.status.code))
              err.asLeft[Quota]
            })
      }
  }
}
