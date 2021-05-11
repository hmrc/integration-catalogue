package uk.gov.hmrc.integrationcatalogue.support


import org.scalatest.{BeforeAndAfterEach, Suite, TestSuite}
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

trait MongoApp extends DefaultPlayMongoRepositorySupport[IntegrationDetail]  with BeforeAndAfterEach  {
  me: Suite with TestSuite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropMongoDb()
  }

  def dropMongoDb(): Unit =
    mongoDatabase.drop()
}


