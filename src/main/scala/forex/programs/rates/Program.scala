package forex.programs.rates

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import forex.domain._
import forex.programs.rates.errors._
import forex.services.{ QuotaService, RatesService }

class Program[F[_]: Sync](
    ratesService: RatesService[F],
    quotaService: QuotaService[F],
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {

//(s"No quota left, please wait for ${quota.hours_until_reset} hours"
    val rateOrError = for {
      quota <- EitherT(quotaService.getQuota).leftMap(quotaErrorToProgramError)

      rateOrError <- if (quota.quota_remaining > 0)
                      EitherT(ratesService.getRates(Rate.Pair(request.from, request.to)))
                        .leftMap(rateErrorToProgramError)
                    else {
                      val quotaLimitError: errors.Error =
                        errors.Error.QuotaLimit(s"No quota left, please wait for ${quota.hours_until_reset} hours")
                      EitherT.left[Rate](quotaLimitError.pure[F])
                    }

    } yield rateOrError

    rateOrError.value

  }

}

object Program {

  def apply[F[_]: Sync](
      ratesService: RatesService[F],
      quotaService: QuotaService[F],
  ): Algebra[F] = new Program[F](ratesService, quotaService)

}
