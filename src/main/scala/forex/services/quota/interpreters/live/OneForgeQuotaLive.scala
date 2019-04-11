package forex.services.quota.interpreters.live

import cats.effect.Sync
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.Quota
import forex.services.errors.CauseError
import forex.services.quota.Algebra
import forex.services.quota.errors.QuotaError
import forex.services.rates.interpreters.live.Protocol.ForgeRateErrorResponse
import org.http4s.Status.Successful
import org.http4s.Uri
import org.http4s.client.Client
import forex.services.rates.interpreters.live._
import forex.services._

class OneForgeQuotaLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  private val baseUri = Uri.fromString(s"${config.host.show}/${config.version.show}").toOption.get

  override def getQuota: F[Either[QuotaError, Quota]] = {
    val uriToCall = baseUri / "quota" +? ("api_key", config.apiKey)

    client
      .get[QuotaError Either Quota](uriToCall) {

        case Successful(resp) =>
          resp
            .as[Quota]
            .flatMap(_.asRight[QuotaError].pure[F])
            // could be still an error thanks to the Forge API (f.i missing API key)
            .recoverWith {
              case _ =>
                resp
                  .as[ForgeRateErrorResponse]
                  .flatMap(errResp => {
                    val err: QuotaError =
                      QuotaError.OneForgeQuotaError("Unable to retrieve quota", CauseError(errResp.message, 200))
                    err.asLeft[Quota].pure[F]
                  })

            }
        case resp =>
          resp
            .as[String]
            .map(cause => {
              val err: QuotaError =
                QuotaError.OneForgeQuotaError("Unable to retrieve quota", CauseError(cause, resp.status.code))
              err.asLeft[Quota]
            })
      }
  }
}
