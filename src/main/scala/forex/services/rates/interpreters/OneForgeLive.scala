package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.{ Price, Rate, Timestamp }
import forex.http.rates.Protocol.{ ForgeConvertRateResponse, _ }
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import io.circe.Json
import org.http4s.Status.{ ClientError, ServerError, Successful }
import org.http4s.{ Status, Uri }
import org.http4s.client.Client

class OneForgeLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  // TODO  deal better with  failure
  private val baseUri = Uri.fromString(s"${config.host.show}/${config.version.show}/convert").toOption.get

  override def get(pair: Rate.Pair): F[Error Either Rate] = {

    val uriToCall = baseUri +? ("from", pair.from) +? ("to", pair.to) +? ("api_key", config.apikey) +? ("quantity", 1)

    client
      .get[Error Either Rate](uriToCall) {
        case (resp) => {
          resp
            .as[ForgeConvertRateResponse]
            .flatMap(
              r =>
                EitherT.right[Error](Rate(pair, Price(r.value), Timestamp.fromUtcTimestamp(r.timestamp)).pure[F]).value
            )
        }
        case ClientError(_) =>
          val err: Error = Error.OneForgeLookupFailed(
            s"Client problem. Please check the correctness of your request  ${uriToCall.renderString}"
          )
          EitherT.left[Rate](err.pure[F]).value

        case ServerError(resp) =>
          val err: Error = Error.OneForgeLookupFailed(
            s"Unable to satisfy the request due to server problem: ${resp.toString}"
          )
          EitherT.left[Rate](err.pure[F]).value

      }

  }

}
