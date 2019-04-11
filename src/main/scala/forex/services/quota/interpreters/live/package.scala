package forex.services.quota.interpreters

import forex.domain.Quota
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

/**
  * Created by okorolenko on 2019-04-11.
  */
package object live {

  implicit val quotaEncoder: Encoder[Quota] =
    deriveEncoder[Quota]

  implicit val quotaDecoder: Decoder[Quota] =
    deriveDecoder[Quota]

}
