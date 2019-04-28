package forex

import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, Timer }
import forex.config.ApplicationConfig
import forex.domain.{ Rate, Timestamp }
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.middleware.{ Logger => ClientLogger }
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout, Logger => ServerLogger }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig,
                                      client: Client[F],
                                      cache: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]]) {

  val clientWithLogger: Client[F] = ClientLogger(logBody = true, logHeaders = true)(client)

  private val ratesService: RatesService[F] = RatesServices.live[F](config.forge, clientWithLogger)
  private val quotaService: QuotaService[F] = QuotaServices.live[F](config.forge, clientWithLogger)
  private val cacheService: CacheService[F] = CacheServices.live[F](config.cache, cache)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService, quotaService, cacheService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] =
    ServerLogger(logBody = true, logHeaders = true)(appMiddleware(routesMiddleware(http).orNotFound))

}
