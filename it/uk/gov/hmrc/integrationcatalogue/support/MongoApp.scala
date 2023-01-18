package uk.gov.hmrc.integrationcatalogue.support

import org.scalatest.{BeforeAndAfterEach, Suite, TestSuite}
import play.api.test.Helpers.await
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

trait MongoApp extends DefaultPlayMongoRepositorySupport[IntegrationDetail] with BeforeAndAfterEach with AwaitTestSupport {
  me: Suite with TestSuite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropMongoDb()
  }

  def dropMongoDb(): Unit =
    await(mongoDatabase.drop().toFuture())
}
