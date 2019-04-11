package forex.services

/**
  * Created by okorolenko on 2019-04-11.
  */
object errors {

  final case class CauseError(msg: String, code: Int)

}
