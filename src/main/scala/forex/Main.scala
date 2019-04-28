package forex

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import forex.config._
import forex.domain.{ Rate, Timestamp }
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].server.as(ExitCode.Success)
}

class Application[F[_]: ConcurrentEffect: Timer] {

  val server: F[Unit] = {

    Ref.of[F, Map[Rate.Pair, (Rate, Timestamp)]](Map.empty).flatMap {
      cache: Ref[F, Map[Rate.Pair, (Rate, Timestamp)]] =>
        BlazeClientBuilder[F](ExecutionContext.global).resource.use[Unit] { client =>
          {
            val stream: Stream[F, Unit] = for {
              config <- Config.stream("app")
              module = new Module[F](config, client, cache)
              _ <- BlazeServerBuilder[F]
                    .bindHttp(config.http.port, config.http.host)
                    .withHttpApp(module.httpApp)
                    .serve
            } yield ()

            stream.compile.drain
          }
        }
    }
  }
}
