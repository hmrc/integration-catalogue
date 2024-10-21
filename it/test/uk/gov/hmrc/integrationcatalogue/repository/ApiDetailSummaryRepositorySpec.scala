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

package uk.gov.hmrc.integrationcatalogue.repository

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, ApiDetailSummary, IntegrationDetail}
import uk.gov.hmrc.integrationcatalogue.support.MdcTesting
import uk.gov.hmrc.integrationcatalogue.testdata.OasParsedItTestData
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext

class ApiDetailSummaryRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[IntegrationDetail]
    with OptionValues
    with MdcTesting
    with OasParsedItTestData {

  import ApiDetailSummaryRepositorySpec.*

  override protected def checkIndexedQueries = false

  private lazy val playApplication = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent),
        bind[Clock].toInstance(clock)
      )
      .build()
  }

  private implicit lazy val executionContext: ExecutionContext = {
    playApplication.injector.instanceOf[ExecutionContext]
  }

  override protected val repository: IntegrationRepository = {
    playApplication.injector.instanceOf[IntegrationRepository]
  }

  private lazy val summaryRepository: ApiDetailSummaryRepository = {
    playApplication.injector.instanceOf[ApiDetailSummaryRepository]
  }

  "ApiDetailSummaryRepository.findWithFilters" - {
    "must return all rows when no filters are specified" in {
      val apiSummaries = setupData()

      val actual = summaryRepository.findWithFilters(List.empty, List.empty).futureValue

      actual should contain theSameElementsAs apiSummaries
    }

    "find 3 results when searching for text that exists in title, endpoint summary with no platform filters" in {
      val apiSummaries = setupData()
      val expected = filterExpected(apiSummaries, apiDetail3, apiDetail4, apiDetail5)

      val actual = summaryRepository.findWithFilters(List("BOOP"), List.empty).futureValue

      actual should contain theSameElementsAs expected
    }

    "find 1 result when searching for text that exists in endpoint description with no platform filters" in {
      val apiSummaries = setupData()
      val expected = filterExpected(apiSummaries, apiDetail3)

      val actual = summaryRepository.findWithFilters(List("DEEPSEARCH"), List.empty).futureValue

      actual should contain theSameElementsAs expected
    }

    "find 1 result when searching for text that exists in endpoint description but uses stemming with no platform filters" in {
      val apiSummaries = setupData()
      val expected = filterExpected(apiSummaries, apiDetail3)

      val actual = summaryRepository.findWithFilters(List("DEEPSEARCHES"), List.empty).futureValue

      actual should contain theSameElementsAs expected
    }

    "find 2 results when searching for text getKnownFactsDesc with no platform filters" in {
      val apiSummaries = setupData()
      val expected = filterExpected(apiSummaries, apiDetail1, apiDetail4)

      val actual = summaryRepository.findWithFilters(List("getKnownFactsDesc"), List.empty).futureValue

      actual should contain theSameElementsAs expected
    }

    "find 1 result when searching for for text that exists in all records & DES platform" in {
      val apiSummaries = setupData()
      val expected = filterExpected(apiSummaries, apiDetail4)

      val actual = summaryRepository.findWithFilters(List("getKnownFactsDesc"), List(PlatformType.DES)).futureValue

      actual should contain theSameElementsAs expected
    }

    "find 2 results when searching for text getKnownFactsDesc & DES or CORE_IF platforms" in {
      val apiSummaries = setupData()
      val expected = filterExpected(apiSummaries, apiDetail1, apiDetail4)

      val actual = summaryRepository.findWithFilters(List("getKnownFactsDesc"), List(PlatformType.DES, PlatformType.CORE_IF)).futureValue

      actual should contain theSameElementsAs expected
    }
  }

  private def setupData(): Set[ApiDetailSummary] = {
    (for {
      api1 <- repository.findAndModify(apiDetail1)
      api3 <- repository.findAndModify(apiDetail3)
      api4 <- repository.findAndModify(apiDetail4)
      api5 <- repository.findAndModify(apiDetail5)
    } yield Set(api1, api3, api4, api5))
      .futureValue
      .map {
        case Right(apiDetail: ApiDetail, _ ) => ApiDetailSummary(apiDetail)
        case _ => fail("Setup error")
      }
  }

  private def filterExpected(apiSummaries: Set[ApiDetailSummary], apis: ApiDetail*): Set[ApiDetailSummary] = {
    apiSummaries
      .filter(apiSummary => apis.exists(_.publisherReference == apiSummary.publisherReference))
  }

}

object ApiDetailSummaryRepositorySpec {

  val clock: Clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneId.of("UTC"))

}
