package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    forge: OneForgeConfig,
    cache: CacheConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneForgeConfig(
    host: String,
    version: String,
    apiKey: String
)

case class CacheConfig(
    ttl: Int
)
