/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.integrationcatalogue.support

import org.scalatest.{BeforeAndAfterEach, Suite, TestSuite}
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
