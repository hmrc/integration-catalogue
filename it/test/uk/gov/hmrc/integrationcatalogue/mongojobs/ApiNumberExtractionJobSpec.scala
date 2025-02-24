/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.mongojobs

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import play.api.inject.bind
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.testdata.ApiTestData
import uk.gov.hmrc.integrationcatalogue.utils.ApiNumberExtractor
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.integrationcatalogue.models.ApiDetail
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId

import java.util.UUID
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

class ApiNumberExtractionJobSpec extends AnyFreeSpec
  with Matchers
  with ScalaFutures
  with DefaultPlayMongoRepositorySupport[IntegrationDetail]
  with ApiTestData
{
  private lazy val playApplication = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent)
      )
      .build()
  }
  private implicit lazy val executionContext: ExecutionContext = {
    playApplication.injector.instanceOf[ExecutionContext]
  }

  override protected val repository: IntegrationRepository = playApplication.injector.instanceOf[IntegrationRepository]
  val apiNumberExtractor = playApplication.injector.instanceOf[ApiNumberExtractor]

  private def insertApiDetailsWithTitles(titles: Seq[String]) = {
    repository.collection.insertMany(
      titles.map(title => apiDetail0.copy(
        id = IntegrationId(UUID.randomUUID()),
        title = title,
        publisherReference = "pubref-" + title)
      )
    ).head().futureValue
  }

  private def runExtractionJob() = {
    new ApiNumberExtractionJob(apiNumberExtractor, repository).run().futureValue
  }

  private def checkApiTitles(expectedTitles: Set[ApiNumberResult]) = {
    repository.collection.find().foldLeft(Set.empty[ApiNumberResult]) { (acc, integrationDetail) => {
      val apiDetail = integrationDetail.asInstanceOf[ApiDetail]
      acc + ApiNumberResult(apiDetail.apiNumber, apiDetail.title)
    }
    }.head.futureValue mustBe expectedTitles
  }

  "ApiNumberExtractionJobSpec" - {
    "must update API number and titles when a valid number is found" in {
      insertApiDetailsWithTitles(Seq(
        "API#101 - title 1",
        "DAPI-102 title 2",
        "acc3   title 3"
      ))
      
      runExtractionJob()

      checkApiTitles(Set(
        ApiNumberResult(Some("API#101"), "title 1"),
        ApiNumberResult(Some("DAPI-102"), "title 2"),
        ApiNumberResult(Some("acc3"), "title 3")
      ))
    }

    "must not update API number and title when no valid number is found" in {
      insertApiDetailsWithTitles(Seq(
        "Some API without a number",
        "AB123 - does not match number format",
        "ABCDE123 - does not match number format",
        "API#123-need_a_space_somewhere"
      ))

      runExtractionJob()

      checkApiTitles(Set(
        ApiNumberResult(None, "Some API without a number"),
        ApiNumberResult(None, "AB123 - does not match number format"),
        ApiNumberResult(None, "ABCDE123 - does not match number format"),
        ApiNumberResult(None, "API#123-need_a_space_somewhere"),
      ))
    }

    "must not update API number when number is on ignore list" in {
      insertApiDetailsWithTitles(Seq(
        "CSRD2 title 1",
        "CSRD2 - title 2",
      ))

      runExtractionJob()

      checkApiTitles(Set(
        ApiNumberResult(None, "CSRD2 title 1"),
        ApiNumberResult(None, "CSRD2 - title 2"),
      ))
    }
  }
}

case class ApiNumberResult(apiNumber: Option[String], title: String)
