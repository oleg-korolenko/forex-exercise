package forex.http.rates

import cats.effect.IO
import forex.domain.Currency._
import forex.domain._
import forex.http.rates.Protocol._
import forex.programs.{ ProgramError, RatesProgram }
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors
import io.circe.Json
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, Method, Request, Response, Status, Uri }
import org.scalatest.{ Assertion, FlatSpec, Matchers }
import cats.syntax.applicative._
import cats.syntax.either._
import forex.http.rates.Converters.ConverterToApiErrorSyntax._
import forex.http.rates.Converters._
import org.http4s.circe._

/**
  * Created by okorolenko on 2019-04-09.
  */
class RatesHttpRoutesTest extends FlatSpec with Matchers with Http4sDsl[IO] {

  "/rates" should "return 200 with if RatesProgram returns a  Rate" in {

    val pair = Rate.Pair(USD, EUR)
    val rate = Rate(pair, Price(0.9d), Timestamp.now)
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        rate.asRight[ProgramError].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=USD&to=EUR"))
    )

    val expectedJson = rate.asGetApiResponse.asJson

    check[Json](response, Status.Ok, Some(expectedJson))
  }

  it should "return 400  if 'from' param can't be parsed to currency" in {
    val pair = Rate.Pair(USD, EUR)

    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        Rate(pair, Price(0.9d), Timestamp.now).asRight[ProgramError].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=UUU&to=EUR"))
    )

    val expectedMessage = "Unable to parse argument [from]".asGetApiError.asJson

    check[Json](response, Status.BadRequest, Some(expectedMessage))
  }

  it should "return 400  if 'to'  param can't be parsed to currency" in {
    val pair = Rate.Pair(USD, EUR)
    val rate = Rate(pair, Price(0.9d), Timestamp.now)
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        rate.asRight[ProgramError].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=USD&to=EU"))
    )

    val expectedMsg = "Unable to parse argument [to]".asGetApiError.asJson

    check[Json](response, Status.BadRequest, Some(expectedMsg))
  }

  it should "return 500  if RatesProgram fails to get the rate" in {
    val error: ProgramError = errors.Error.RateLookupFailed("error")
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        error.asLeft[Rate].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=USD&to=EUR"))
    )

    val expectedJson = error.asGetApiError.asJson

    check[Json](response, Status.InternalServerError, Some(expectedJson))
  }

  it should "return 202  if RatesProgram gets the rate but it's already out-of-date" in {
    val error: ProgramError = errors.Error.RateIsTooOldLookupFailed("error")
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        error.asLeft[Rate].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=USD&to=EUR"))
    )

    val expectedJson = error.asGetApiError.asJson

    check[Json](response, Status.Accepted, Some(expectedJson))
  }

  it should "return 403  if RatesProgram returns that quota is spent" in {
    val error: ProgramError = errors.Error.QuotaLimit("error")
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        error.asLeft[Rate].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=USD&to=EUR"))
    )

    val expectedJson = error.asGetApiError.asJson

    check[Json](response, Status.Forbidden, Some(expectedJson))
  }

  it should "return 500  if RatesProgram fails to check the quota" in {

    val error: ProgramError = errors.Error.QuotaLookupFailed("error")
    val ratesProgram = new RatesProgram[IO] {
      override def get(request: GetRatesRequest): IO[Either[ProgramError, Rate]] =
        error.asLeft[Rate].pure[IO]
    }
    val rateHttp = new RatesHttpRoutes[IO](ratesProgram).routes

    val response: IO[Response[IO]] = rateHttp.orNotFound.run(
      Request(method = Method.GET, uri = Uri.uri("/rates?from=USD&to=EUR"))
    )

    val expectedJson = error.asGetApiError.asJson

    check[Json](response, Status.InternalServerError, Some(expectedJson))
  }

  def check[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(
      implicit ev: EntityDecoder[IO, A]
  ): Assertion = {
    val actualResp  = actual.unsafeRunSync
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck =
      expectedBody.fold[Boolean](actualResp.body.compile.toVector.unsafeRunSync.isEmpty)( // Verify Response's body is empty.
        expected => actualResp.as[A].unsafeRunSync == expected
      )
    statusCheck && bodyCheck should be(true)
  }
}
