package kafka.error

object AppExceptions {

  sealed abstract class AppException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
    def this(message: String) = this(message, null)
  }

  final case class CassandraException(message: String) extends AppException(message)

  final case class CsvParseException(message: String) extends AppException(message)

}
