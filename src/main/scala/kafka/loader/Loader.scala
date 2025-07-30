package kafka.loader

import com.typesafe.scalalogging.LazyLogging
import kafka.cassandra.CassandraSession
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Loader[T] extends LazyLogging {

  protected[this] def session: CassandraSession

  def process(message: String)(implicit ec: ExecutionContext): Future[Done] = {
    Try(store(parse(message))) match {
      case Success(_) =>
        logger.debug("Stored: {}", message)
        Future.successful(Done)
      case Failure(ex) =>
        logger.error("Error processing message: {}. Detail: {}", message, ex.getMessage)
        Future.failed(ex)
    }
  }

  def parse(line: String): T

  def store(entity: T)(implicit ec: ExecutionContext): Future[Unit]

}
