package forex.services.rates

import cats.effect.Sync
import forex.config.OneForgeConfig
import forex.services.rates.interpreters._
import forex.services.rates.interpreters.dummy.OneForgeRateDummy
import forex.services.rates.interpreters.live.OneForgeRatesLive
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Sync](): Algebra[F] = new OneForgeRateDummy[F]()
  def live[F[_]: Sync](config: OneForgeConfig, client: Client[F]): Algebra[F] =
    new OneForgeRatesLive[F](config, client)
}
