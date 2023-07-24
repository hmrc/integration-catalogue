package uk.gov.hmrc.integrationcatalogue.support

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

trait AwaitTestSupport {
  def await[A](future: Future[A], timeout: Duration = Duration.apply(5, SECONDS)): A = Await.result(future, timeout)
}
