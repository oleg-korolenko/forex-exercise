package forex.programs.rates

import cats.Applicative
import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain._
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.errors.CauseError
import forex.services.quota.errors.QuotaError
import forex.services.rates.errors._
import org.scalatest.{ FlatSpec, Matchers }

/**
  * Created by okorolenko on 2019-04-09.
  */
class ProgramTest extends FlatSpec with Matchers {

  "Program" should "return Rate if RateService returns quota and rate" in {

    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val now  = Timestamp.now

    val stubRateResp  = Rate(pair, Price(BigDecimal(100)), now).asRight[RateError].pure[IO]
    val stubQuotaResp = Quota(100, 1000, 900, 10).asRight[QuotaError].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp)
    val quotaService = createQuotaServicesInterpreter(stubQuotaResp)
    val program      = Program[IO](ratesService, quotaService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    val expectedRate = Rate(pair, Price(BigDecimal(100)), now)

    result.isRight should be(true)
    result.right.get should be(expectedRate)

  }

  it should "return program RateLookupFailed if RateService doesn't return rate" in {

    val pair                 = Rate.Pair(Currency.USD, Currency.EUR)
    val errMessage           = "Can't get Rate"
    val rateServiceError     = RateError.OneForgeLookupRateError("Unable to retrieve rate", CauseError(errMessage, 500))
    val expectedProgramError = errors.rateErrorToProgramError(rateServiceError)

    val stubQuotaResp = Quota(100, 1000, 900, 10).asRight[QuotaError].pure[IO]
    val stubRateResp  = rateServiceError.asLeft[Rate].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp)
    val quotaService = createQuotaServicesInterpreter(stubQuotaResp)
    val program      = Program[IO](ratesService, quotaService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedProgramError)

  }

  it should "return program QuotaLookupFailed if RateService doesn't return quote" in {

    val pair = Rate.Pair(Currency.USD, Currency.EUR)
    val now  = Timestamp.now

    val quotaError =
      QuotaError.OneForgeQuotaError("Unable to retrieve quota", CauseError("something wrong", 500))
    val expectedProgramError = errors.quotaErrorToProgramError(quotaError)

    val stubQuotaResp = quotaError.asLeft[Quota].pure[IO]
    val stubRateResp  = Rate(pair, Price(BigDecimal(100)), now).asRight[RateError].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp)
    val quotaService = createQuotaServicesInterpreter(stubQuotaResp)
    val program      = Program[IO](ratesService, quotaService)

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

    val stubQuotaResp = quota.asRight[QuotaError].pure[IO]
    val stubRateResp  = Rate(pair, Price(BigDecimal(100)), now).asRight[RateError].pure[IO]

    val ratesService = createRatesServicesInterpreter(stubRateResp)
    val quotaService = createQuotaServicesInterpreter(stubQuotaResp)
    val program      = Program[IO](ratesService, quotaService)

    val result = program.get(GetRatesRequest(pair.from, pair.to)).unsafeRunSync()

    result.isLeft should be(true)
    result.left.get should be(expectedProgramError)
  }

  private def createRatesServicesInterpreter(stubRateResp: IO[RateError Either Rate]) =
    new RateServiceStubInterpreter[IO](stubRateResp)

  private def createQuotaServicesInterpreter(stubQuotaResp: IO[Either[QuotaError, Quota]]) =
    new QuotaServiceStubInterpreter[IO](stubQuotaResp)

}

class RateServiceStubInterpreter[F[_]: Applicative](stubRate: F[RateError Either Rate])
    extends forex.services.rates.Algebra[F] {

  override def getRates(pair: Rate.Pair): F[RateError Either Rate] = stubRate

}
class QuotaServiceStubInterpreter[F[_]: Applicative](stubQuota: F[Either[QuotaError, Quota]])
    extends forex.services.quota.Algebra[F] {

  override def getQuota: F[Either[QuotaError, Quota]] = stubQuota
}
