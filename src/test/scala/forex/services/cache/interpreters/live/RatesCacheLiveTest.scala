package forex.services.cache.interpreters.live

import java.time.OffsetDateTime

import cats.effect.IO
import cats.effect.concurrent.Ref
import forex.config.CacheConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
import org.scalatest.{ FlatSpec, Matchers }

/**
  * Created by okorolenko on 2019-04-28.
  */
class RatesCacheLiveTest extends FlatSpec with Matchers {

  val config = CacheConfig(ttl = 5)

  "RatesCacheLive" should "put a new rate into cache" in {
    val cache = new RatesCacheLive[IO](
      config,
      Ref.of[IO, Map[Rate.Pair, (Rate, Timestamp)]](Map.empty).unsafeRunSync()
    )
    val pair = Rate.Pair(Currency.EUR, Currency.USD)
    val rate = Rate(pair, Price(10d), Timestamp.now)

    cache.set(pair, rate).unsafeRunSync()

    val result: Option[Rate] = cache.get(pair).unsafeRunSync()

    result should be(Some(rate))
  }

  it should "replace an existing rate for the same currency pair " in {
    val pair    = Rate.Pair(Currency.EUR, Currency.USD)
    val oldRate = Rate(pair, Price(10d), Timestamp.now)

    val cache = new RatesCacheLive[IO](
      config,
      Ref.of[IO, Map[Rate.Pair, (Rate, Timestamp)]](Map(pair -> Tuple2(oldRate, oldRate.timestamp))).unsafeRunSync()
    )

    val newRate = Rate(pair, Price(12d), Timestamp.now)
    cache.set(pair, newRate).unsafeRunSync()

    val result: Option[Rate] = cache.get(pair).unsafeRunSync()

    result should be(Some(newRate))
  }

  it should "not return the rate from the cache as TTL is already exceeded for the entry " in {
    val cache = new RatesCacheLive[IO](
      config,
      Ref.of[IO, Map[Rate.Pair, (Rate, Timestamp)]](Map.empty).unsafeRunSync()
    )
    val pair      = Rate.Pair(Currency.EUR, Currency.USD)
    val currentTs = OffsetDateTime.now.toEpochSecond
    val olderTs   = currentTs - config.ttl - 5
    val rate      = Rate(pair, Price(10d), Timestamp.fromUtcTimestamp(olderTs))

    cache.set(Rate.Pair(Currency.EUR, Currency.USD), rate).unsafeRunSync()

    val result = cache.get(pair).unsafeRunSync()

    result should be(None)
  }
}
