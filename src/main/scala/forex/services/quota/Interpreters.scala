package forex.services.quota

import cats.effect.Sync
import forex.config.OneForgeConfig
import forex.services.quota.interpreters.dummy.OneForgeQuotaDummy
import forex.services.quota.interpreters.live.OneForgeQuotaLive
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Sync](): Algebra[F] = new OneForgeQuotaDummy[F]()
  def live[F[_]: Sync](config: OneForgeConfig, client: Client[F]): Algebra[F] =
    new OneForgeQuotaLive[F](config, client)
}
