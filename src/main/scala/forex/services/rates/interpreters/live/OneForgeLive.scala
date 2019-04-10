package forex.services.rates.interpreters.live

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.{ Price, Quota, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import forex.services.rates.interpreters._
import forex.services.rates.interpreters.live.Protocol.{ ForgeConvertSuccessResponse, ForgeErrorMessageResponse }
import org.http4s.Status.Successful
import org.http4s.Uri
import org.http4s.client.Client

class OneForgeLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  // TODO  deal better with  failure
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
                  EitherT.left[Rate](err.pure[F]).value
                } else EitherT.right[Error](Rate(pair, Price(r.value), ts).pure[F]).value
              }
            )
            // could be still an error thanks to the Forge API (f.i missing API key)
            .recoverWith {
              case _ =>
                resp
                  .as[ForgeErrorMessageResponse]
                  .flatMap(_ => {
                    val err: Error = Error.OneForgeLookupRateError("Unable to retrieve rate", resp.status.code)
                    EitherT.left[Rate](err.pure[F]).value
                  })

            }
        case resp =>
          val err: Error = Error.OneForgeLookupRateError("Unable to retrieve rate", resp.status.code)
          EitherT.left[Rate](err.pure[F]).value
      }

  }

  override def getQuota: F[Either[Error, Quota]] = {
    val uriToCall = baseUri / "quota" +? ("api_key", config.apiKey)

    client
      .get[Error Either Quota](uriToCall) {

        case Successful(resp) =>
          resp
            .as[Quota]
            .flatMap(quota => EitherT.right[Error](quota.pure[F]).value)
            // could be still an error thanks to the Forge API (f.i missing API key)
            .recoverWith {
              case _ =>
                resp
                  .as[ForgeErrorMessageResponse]
                  .flatMap(errMsg => {
                    val err: Error =
                      Error.OneForgeQuotaError("Unable to retrieve quota", 200)
                    EitherT.left[Quota](err.pure[F]).value
                  })

            }
        case resp =>
          val err: Error = Error.OneForgeQuotaError("Unable to retrieve quota", resp.status.code)
          EitherT.left[Quota](err.pure[F]).value
      }
  }
}
