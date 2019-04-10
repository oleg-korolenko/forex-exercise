package forex.programs.rates

import cats.Applicative
import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain._
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.rates.errors.Error
import org.scalatest.{ FlatSpec, Matchers }

/**
  * Created by okorolenko on 2019-04-09.
  */
class ProgramTest extends FlatSpec with Matchers {

  "Program" should "return Rate if RateService returns quota and rate" in {

    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val now  = Timestamp.now

    val stubRateResp  = Rate(pair, Price(BigDecimal(100)), now).asRight[Error].pure[IO]
    val stubQuotaResp = Quota(100, 1000, 900, 10).asRight[Error].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp, stubQuotaResp)
    val program      = Program[IO](ratesService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    val expectedRate = Rate(pair, Price(BigDecimal(100)), now)

    result.isRight should be(true)
    result.right.get should be(expectedRate)

  }

  it should "return program RateLookupFailed if RateService doesn't return rate" in {

    val pair                 = Rate.Pair(Currency.USD, Currency.EUR)
    val errMessage           = "Can't get Rate"
    val rateServiceError     = Error.OneForgeLookupRateError(errMessage, 500)
    val expectedProgramError = errors.toProgramError(rateServiceError)

    val stubQuotaResp = Quota(100, 1000, 900, 10).asRight[Error].pure[IO]
    val stubRateResp  = rateServiceError.asLeft[Rate].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp, stubQuotaResp)
    val program      = Program[IO](ratesService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedProgramError)

  }

  it should "return program QuotaLookupFailed if RateService doesn't return quote" in {

    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val now  = Timestamp.now

    val rateServiceQuotaError = Error.OneForgeQuotaError("Can't get Quota", 500)
    val expectedProgramError  = errors.toProgramError(rateServiceQuotaError)

    val stubQuotaResp = rateServiceQuotaError.asLeft[Quota].pure[IO]
    val stubRateResp  = Rate(pair, Price(BigDecimal(100)), now).asRight[Error].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp, stubQuotaResp)
    val program      = Program[IO](ratesService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedProgramError)

  }

  it should "return program QuotaLimit if returned quota indicates that of don't have requests left" in {

    val pair  = Rate.Pair(Currency.USD, Currency.EUR)
    val now   = Timestamp.now
    val quota = Quota(100, 100, 0, 12)

    val expectedProgramError =
      errors.Error.QuotaLimit(s"No quota left, please wait for ${quota.hours_until_reset} hours")

    val stubQuotaResp = quota.asRight[Error].pure[IO]
    val stubRateResp  = Rate(pair, Price(BigDecimal(100)), now).asRight[Error].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp, stubQuotaResp)
    val program      = Program[IO](ratesService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedProgramError)
  }

  private def createRatesServicesInterpreter(stubRateResp: IO[Error Either Rate],
                                             stubQuotaResp: IO[Either[Error, Quota]]) =
    new RateServiceStubInterpreter[IO](stubRateResp, stubQuotaResp)

}

class RateServiceStubInterpreter[F[_]: Applicative](stubRate: F[Error Either Rate], stubQuota: F[Either[Error, Quota]])
    extends forex.services.rates.Algebra[F] {

  override def getRates(pair: Rate.Pair): F[Error Either Rate] = stubRate

  override def getQuota: F[Either[Error, Quota]] = stubQuota
}
