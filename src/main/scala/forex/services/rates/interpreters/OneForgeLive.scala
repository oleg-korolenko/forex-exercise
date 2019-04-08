package forex.services.rates.interpreters

import cats.Applicative
import cats.implicits._
import forex.config.OneForgeConfig
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.client.Client
import org.http4s.Http4s.uri

class OneForgeLive[F[_]: Applicative](config: OneForgeConfig, client: Client[F]) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val uriToCall = uri(config.host) +? ("from", pair.from.show) +? ("to", pair.to.show) +? ("api_key", "gujntnw3DyPBqMvbO5N9hx8bgENTOLua")
    println(s"to call = ${uriToCall.renderString}")
    client.get(uriToCall)(resp => {
      // TODO temp
      Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]
    })
  }

}
