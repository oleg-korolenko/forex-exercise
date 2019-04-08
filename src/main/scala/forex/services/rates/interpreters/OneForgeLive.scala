package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.{ Price, Rate, Timestamp }
import forex.http.rates.Protocol.{ ForgeConvertRateResponse, _ }
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import org.http4s.Uri
import org.http4s.client.Client

class OneForgeLive[F[_]: Sync](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  // we can stop directly the server since this service will not work without the correct base URL
  // TODO  deal better with  failure
  private val baseUri = Uri.fromString(s"${config.host.show}/${config.version.show}/convert").toOption.get

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val uriToCall = baseUri +? ("from", pair.from) +? ("to", pair.to) +? ("api_key", config.apikey) +? ("quantity", 1)

    val rate: F[Rate] = client
      .get[ForgeConvertRateResponse](uriToCall)(_.as[ForgeConvertRateResponse])
      // TODO replace by map , and deal with impl evidence collusion
      .flatMap(r => Rate(pair, Price(r.value), Timestamp.fromUtcTimestamp(r.timestamp)).pure[F])

    EitherT.right[Error](rate).value
  }

}
