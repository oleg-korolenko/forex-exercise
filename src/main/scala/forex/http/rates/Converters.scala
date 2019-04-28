package forex.http.rates

import forex.domain._
import forex.programs.rates.errors._

object Converters {
  import Protocol._

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }

  private[rates] trait ConverterToApiError[T] {
    def asGetApiError(toConvert: T): GetApiError
  }

  private[rates] implicit val programErrorAsGetApiError = new ConverterToApiError[Error] {
    override def asGetApiError(error: Error): GetApiError = GetApiError(error.msg)
  }

  private[rates] implicit val stringAsGetApiError = new ConverterToApiError[String] {
    override def asGetApiError(toConvert: String): GetApiError = GetApiError(toConvert)
  }

  object ConverterToApiErrorSyntax {

    private[rates] implicit class ConverterToApiErrorOps[T](toConvert: T) {

      def asGetApiError(implicit converter: ConverterToApiError[T]): GetApiError =
        converter.asGetApiError(toConvert)
    }

  }

}
