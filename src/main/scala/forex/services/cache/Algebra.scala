package forex.services.cache

/**
  * Created by okorolenko on 2019-04-27.
  */
trait Algebra[F[_], K, V] {
  def set(key: K, obj: V): F[Unit]
  def get(key: K): F[Option[V]]
}
