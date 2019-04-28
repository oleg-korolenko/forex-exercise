package forex.services.cache

import cats.effect.Sync
import cats.effect.concurrent.Ref
import forex.config.CacheConfig
import forex.domain.{ Rate, Timestamp }
import forex.services.cache.interpreters.live.RatesCacheLive

/**
  * Created by okorolenko on 2019-04-27.
  */
object Interpreters {
  def live[F[_]: Sync](config: CacheConfig,
                       cache: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]]): Algebra[F, Rate.Pair, Rate] =
    new RatesCacheLive[F](
      config,
      cache
    )
}
