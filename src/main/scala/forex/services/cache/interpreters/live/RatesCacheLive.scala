package forex.services.cache.interpreters.live

import cats.Monad
import cats.effect.concurrent.Ref
import cats.implicits._
import forex.config.CacheConfig
import forex.domain._
import forex.services.cache.Algebra

/**
  * Created by okorolenko on 2019-04-27.
  */
class RatesCacheLive[F[_]: Monad](
    config: CacheConfig,
    internalState: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]]
) extends Algebra[F, Rate.Pair, Rate] {

  override def set(key: Rate.Pair, rate: Rate): F[Unit] =
    internalState.update(_.updated(key, (rate, rate.timestamp)))

  override def get(key: Rate.Pair): F[Option[Rate]] =
    internalState.get.map(_.get(key).filter(entry => isEntryStillValid(entry._2)).map(_._1))

  private def isEntryStillValid(ts: Timestamp) =
    !Timestamp.isOlderThan(config.ttl)(ts)
}
