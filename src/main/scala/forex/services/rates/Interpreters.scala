package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.OneForgeConfig
import forex.services.rates.interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative](): Algebra[F] = new OneForgeDummy[F]()
  def live[F[_]: Sync](config: OneForgeConfig, client: Client[F]): Algebra[F] =
    new OneForgeLive[F](config, client)
}
