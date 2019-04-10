package forex.services.rates.interpreters

import java.time.OffsetDateTime

import cats.effect.IO
import forex.config.OneForgeConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.http.rates.Protocol._
import forex.http.rates.QueryParams.{ FromQueryParam, QuantityQueryParam, ToQueryParam }
import forex.services.rates.errors.Error
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, Response }
import org.scalatest.{ FlatSpec, Matchers }

/**
  * Created by okorolenko on 2019-04-09.
  */
class OneForgeLiveTest extends FlatSpec with Matchers with Http4sDsl[IO] {

  val config = OneForgeConfig("http://myservice", "v1", 5, "secret")

  val currencyPair = Rate.Pair(Currency.USD, Currency.EUR)
  val currentTs    = OffsetDateTime.now.toEpochSecond
  val price        = Price(0.9d)

  "GET /rates" should "return Rate if Forge API responds correctly" in {
    val forgeRateResponse = ForgeConvertSuccessResponse(
      price.value.doubleValue(),
      "conversion text",
      currentTs
    )
    val expectedRate = Rate(currencyPair, price, Timestamp.fromUtcTimestamp(currentTs))
    val forgeApi     = prepareTestAPI(Ok(forgeRateResponse.asJson))
    val result       = new OneForgeLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getRates(currencyPair).unsafeRunSync()
    result.isRight should be(true)
    result.right.get should be(expectedRate)
  }

  it should "return OneForgeLookupUnknownError if Forge API responds with 200 but with error in JSON" in {
    val apiErrMsg         = "API Key Not Valid. Please go to 1forge.com to get an API key"
    val expectedError     = Error.OneForgeLookupUnknownError(s"$apiErrMsg")
    val forgeRateResponse = ForgeErrorMessageResponse(error = true, apiErrMsg)
    val forgeApi          = prepareTestAPI(Ok(forgeRateResponse.asJson))
    val result            = new OneForgeLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getRates(currencyPair).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }

  it should "return OneForgeLookupRateIsToolOld if Forge API returns a Rate older than defined threshold" in {
    // ts older than NOW - THRESHOLD
    val olderTs = currentTs - config.oldRateThresholdInSecs - 5

    val forgeRateResponse = ForgeConvertSuccessResponse(
      price.value.doubleValue(),
      "conversion text",
      olderTs
    )
    val apiErrMsg     = s"Rate is too old: ${Timestamp.fromUtcTimestamp(olderTs).value.toString}"
    val expectedError = Error.OneForgeLookupRateIsToolOld(s"$apiErrMsg")

    val forgeApi = prepareTestAPI(Ok(forgeRateResponse.asJson))
    val result   = new OneForgeLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getRates(currencyPair).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }

  it should "return OneForgeLookupServerError if Forge API returns one of server error codes" in {
    val expectedError = Error.OneForgeLookupServerError("Server problem")
    val forgeApi      = prepareTestAPI(InternalServerError("something wrong"))
    val result        = new OneForgeLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getRates(currencyPair).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }

  it should "return OneForgeLookupClientError if Forge API returns one of client error codes" in {
    val expectedError = Error.OneForgeLookupClientError("Client problem")
    val forgeApi      = prepareTestAPI(BadRequest("something wrong"))
    val result        = new OneForgeLive[IO](config, Client.fromHttpApp[IO](forgeApi)).getRates(currencyPair).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedError)
  }

  private def prepareTestAPI(response: IO[Response[IO]]) =
    HttpRoutes
      .of[IO] {
        case GET -> Root / config.version / "convert" :? FromQueryParam(currencyPair.from) +& ToQueryParam(
              currencyPair.to
            ) +& QuantityQueryParam(1) =>
          response
      }
      .orNotFound

}
