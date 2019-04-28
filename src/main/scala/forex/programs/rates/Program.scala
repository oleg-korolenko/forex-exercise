package forex.programs.rates

import cats.data.{ EitherT, OptionT }
import cats.effect.Sync
import cats.implicits._
import forex.domain._
import forex.programs.ProgramError
import forex.programs.rates.errors._
import forex.services.{ CacheService, QuotaService, RatesService }

class Program[F[_]: Sync](
    ratesService: RatesService[F],
    quotaService: QuotaService[F],
    cacheService: CacheService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[ProgramError Either Rate] = {

    val pair = Rate.Pair(request.from, request.to)

    val getNewRate: EitherT[F, _root_.forex.programs.rates.errors.Error, Rate] =
      for {
        quota <- EitherT(quotaService.getQuota).leftMap(quotaErrorToProgramError)
        rateOrError <- if (quota.quota_remaining > 0)
                        EitherT(ratesService.getRates(pair))
                          .leftMap(rateErrorToProgramError)
                      else {
                        val quotaLimitError: ProgramError =
                          errors.Error.QuotaLimit(
                            s"No quota left, please wait for ${quota.hours_until_reset} hours"
                          )
                        EitherT.left[Rate](quotaLimitError.pure[F])
                      }

      } yield rateOrError

    val addToCache: Rate => F[Rate] = (rate: Rate) => {
      for {
        _ <- cacheService.set(pair, rate)
      } yield rate
    }
    val getFromCacheOrFromOneForge: F[Either[Error, Rate]] =
      OptionT(cacheService.get(pair))
        .fold(
          getNewRate
            .flatMap(rate => EitherT.right[Error](addToCache(rate)))
            .value
        )(rate => EitherT.right[Error](rate.pure[F]).value)
        .flatten

    getFromCacheOrFromOneForge
  }

}

object Program {

  def apply[F[_]: Sync](
      ratesService: RatesService[F],
      quotaService: QuotaService[F],
      cacheService: CacheService[F]
  ): Algebra[F] = new Program[F](ratesService, quotaService, cacheService)

}
