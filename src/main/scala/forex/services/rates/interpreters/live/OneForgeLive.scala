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
import org.http4s.Status.{ ClientError, ServerError, Successful }
import org.http4s.Uri
import org.http4s.client.Client

class OneForgeLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  // TODO  deal better with  failure
  private val baseUri      = Uri.fromString(s"${config.host.show}/${config.version.show}/convert").toOption.get
  private val isRateTooOld = Timestamp.isOlderThan(config.oldRateThresholdInSecs)

  override def getRates(pair: Rate.Pair): F[Error Either Rate] = {

    val uriToCall = baseUri +? ("from", pair.from) +? ("to", pair.to) +? ("api_key", config.apiKey) +? ("quantity", 1)

    client
      .get[Error Either Rate](uriToCall) {
        case ClientError(resp) =>
          val err: Error = Error.OneForgeLookupClientError(s"Client problem")
          EitherT.left[Rate](err.pure[F]).value

        case ServerError(resp) =>
          val err: Error = Error.OneForgeLookupServerError(s"Server problem")
          EitherT.left[Rate](err.pure[F]).value

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
                  .flatMap(errMsg => {
                    val err: Error = Error.OneForgeLookupUnknownError(s"${errMsg.message}")
                    EitherT.left[Rate](err.pure[F]).value
                  })

            }
      }

  }

  override def getQuota: F[Either[Error, Quota]] = ???
}
