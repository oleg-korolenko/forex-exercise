package forex.services.quota.interpreters.live

import cats.effect.IO
import forex.config.OneForgeConfig
import forex.domain.Quota
import forex.services.errors.CauseError
import forex.services.quota.errors.QuotaError
import forex.services.rates.interpreters.live.Protocol.ForgeRateErrorResponse
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, Response }
import org.scalatest.{ FlatSpec, Matchers }

/**
  * Created by okorolenko on 2019-04-11.
  */
class OneForgeQuotaLiveTest extends FlatSpec with Matchers with Http4sDsl[IO] {
  val config = OneForgeConfig("http://myservice", "v1", 5, "secret")

  "getQuota" should "return Quota if Forge API responds correctly" in {
    val expectedQuota = Quota(1, 10, 9, 12)
    val forgeApi      = stubForgeGetQuotaAPI(Ok(expectedQuota.asJson))
    val result        = new OneForgeQuotaLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getQuota.unsafeRunSync()
    result.isRight should be(true)
    result.right.get should be(expectedQuota)
  }
  it should "return OneForgeQuotaError  if Forge API responds with 200 but with error in JSON" in {
    val apiErrMsg         = "API Key Not Valid. Please go to 1forge.com to get an API key"
    val expectedError     = QuotaError.OneForgeQuotaError("Unable to retrieve quota", CauseError(apiErrMsg, 200))
    val forgeRateResponse = ForgeRateErrorResponse(error = true, apiErrMsg)
    val forgeApi          = stubForgeGetQuotaAPI(Ok(forgeRateResponse.asJson))
    val result            = new OneForgeQuotaLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getQuota.unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }
  it should "return OneForgeLookupServerError if Forge API returns one of server error codes" in {
    val expectedError = QuotaError.OneForgeQuotaError("Unable to retrieve quota", CauseError("something wrong", 500))
    val forgeApi      = stubForgeGetQuotaAPI(InternalServerError("something wrong"))
    val result        = new OneForgeQuotaLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getQuota.unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }
  it should "return OneForgeLookupClientError if Forge API returns one of client error codes" in {
    val expectedError = QuotaError.OneForgeQuotaError("Unable to retrieve quota", CauseError("something wrong", 400))
    val forgeApi      = stubForgeGetQuotaAPI(BadRequest("something wrong"))
    val result        = new OneForgeQuotaLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getQuota.unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }
  private def stubForgeGetQuotaAPI(response: IO[Response[IO]]) =
    HttpRoutes
      .of[IO] {
        case GET -> Root / config.version / "quota" =>
          response
      }
      .orNotFound
}
