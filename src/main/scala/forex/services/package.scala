package forex

import cats.effect.Sync
import forex.domain.Rate
import io.circe.generic.extras.decoding.{ EnumerationDecoder, UnwrappedDecoder }
import io.circe.generic.extras.encoding.{ EnumerationEncoder, UnwrappedEncoder }
import io.circe.{ Decoder, Encoder }
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import org.http4s.{ EntityDecoder, EntityEncoder }

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  type QuotaService[F[_]] = quota.Algebra[F]
  type CacheService[F[_]] = cache.Algebra[F, Rate.Pair, Rate]

  final val RatesServices = rates.Interpreters
  final val QuotaServices = quota.Interpreters
  final val CacheServices = cache.Interpreters

  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def valueClassDecoder[A: UnwrappedDecoder]: Decoder[A] = implicitly

  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A] = implicitly
  implicit def enumDecoder[A: EnumerationDecoder]: Decoder[A] = implicitly

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Sync]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder, F[_]: Sync]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}
