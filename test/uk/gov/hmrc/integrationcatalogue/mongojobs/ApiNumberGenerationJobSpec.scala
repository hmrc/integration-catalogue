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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{framework, when}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus.LIVE
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{CDS_CLASSIC, HIP}
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, Maintainer, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, ApiDetailSummary}
import uk.gov.hmrc.integrationcatalogue.mongojobs.ApiNumberGenerationJob.MigrationSummary
import uk.gov.hmrc.integrationcatalogue.repository.{ApiDetailSummaryRepository, IntegrationRepository}
import uk.gov.hmrc.integrationcatalogue.utils.ApiNumberGenerator

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class ApiNumberGenerationJobSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  import ApiNumberGenerationJobSpec.*

  "ApiNumberGenerationJob" - {
    "must process an empty list of APIs and return the correct summary" in {
      val fixture = buildFixture()

      when(fixture.summaryRepository.findWithFilters(any, any))
        .thenReturn(Future.successful(List.empty))

      fixture.job.processApis().map {
        result =>
          result mustBe MigrationSummary()
      }
    }

    "must process a non-empty list of APIs correctly" in {
      val fixture = buildFixture()
      val nextApiNumber = "API#101"

      when(fixture.summaryRepository.findWithFilters(any, any))
        .thenReturn(Future.successful(
          List(
            ApiDetailSummary(noRuleApi),
            ApiDetailSummary(hasApiNumberApi),
            ApiDetailSummary(updatedApi),
            ApiDetailSummary(updateErrorApi),
            ApiDetailSummary(updateNotFound)
          )))

      // General stubs
      when(fixture.apiNumberGenerator.generate(eqTo(HIP), eqTo(None)))
        .thenReturn(Future.successful(Some(nextApiNumber)))

      // Stubs for an API that has no platform rule
      when(fixture.apiNumberGenerator.generate(eqTo(noRuleApi.platform), any))
        .thenReturn(Future.successful(None))

      // Stubs for a HIP API that already has an API number
      when(fixture.apiNumberGenerator.generate(eqTo(HIP), eqTo(hasApiNumberApi.apiNumber)))
        .thenReturn(Future.successful(hasApiNumberApi.apiNumber))

      // Stubs for an API that gets updated
      when(fixture.integrationRepository.findByPublisherRef(eqTo(HIP), eqTo(updatedApi.publisherReference)))
        .thenReturn(Future.successful(Some(updatedApi)))

      when(fixture.integrationRepository.findByPublisherRef(eqTo(HIP), eqTo(updatedApi.publisherReference)))
        .thenReturn(Future.successful(Some(updatedApi)))

      val updateApiWithNumber = updatedApi.copy(apiNumber = Some(nextApiNumber))

      when(fixture.integrationRepository.findAndModify(eqTo(updateApiWithNumber)))
        .thenReturn(Future.successful(Right((updateApiWithNumber, true))))

      // Stubs for an API that results in update error
      when(fixture.integrationRepository.findByPublisherRef(eqTo(HIP), eqTo(updateErrorApi.publisherReference)))
        .thenReturn(Future.successful(Some(updateErrorApi)))

      when(fixture.integrationRepository.findByPublisherRef(eqTo(HIP), eqTo(updateErrorApi.publisherReference)))
        .thenReturn(Future.successful(Some(updateErrorApi)))

      val updateErrorApiWithNumber= updateErrorApi.copy(apiNumber = Some(nextApiNumber))

      when(fixture.integrationRepository.findAndModify(eqTo(updateErrorApiWithNumber)))
        .thenReturn(Future.successful(Left(new Exception())))

      // Stub for an API that can't be found while updating
      when(fixture.integrationRepository.findByPublisherRef(eqTo(HIP), eqTo(updateNotFound.publisherReference)))
        .thenReturn(Future.successful(None))

      fixture.job.processApis().map {
        result =>
          result mustBe MigrationSummary(5, 1, 1, 2)
      }
    }
  }

  private def buildFixture(): Fixture = {
    val summaryRepository = mock[ApiDetailSummaryRepository]
    val integrationRepository = mock[IntegrationRepository]
    val apiNumberGenerator = mock[ApiNumberGenerator]

    val job = new ApiNumberGenerationJob(
      summaryRepository,
      integrationRepository,
      apiNumberGenerator
    )

    Fixture(
      summaryRepository,
      integrationRepository,
      apiNumberGenerator,
      job
    )
  }

}

private object ApiNumberGenerationJobSpec {

  private def buildApi(index: Int, platform: PlatformType, apiNumber: Option[String] = None) = ApiDetail(
    id = IntegrationId(UUID.randomUUID()),
    publisherReference = s"test-publisher-ref-$index",
    title = s"test-title-$index",
    description = s"test-description-$index",
    lastUpdated = Instant.now(),
    platform = platform,
    maintainer = Maintainer(name = "test-maintainer", slackChannel = "test-slack-channel", contactInfo = List.empty),
    version = "1.0.0",
    specificationType = SpecificationType.OAS_V3,
    endpoints = List.empty,
    shortDescription = None,
    openApiSpecification = s"test-oas-$index",
    apiStatus = LIVE,
    reviewedDate = Instant.now(),
    scopes = Set.empty,
    apiNumber = apiNumber
  )

  val noRuleApi: ApiDetail = buildApi(1, CDS_CLASSIC)
  val hasApiNumberApi: ApiDetail = buildApi(2, HIP, Some("API#2"))
  val updatedApi: ApiDetail = buildApi(3, HIP)
  val updateErrorApi: ApiDetail = buildApi(4, HIP)
  val updateNotFound: ApiDetail = buildApi(5, HIP)

  case class Fixture(
    summaryRepository: ApiDetailSummaryRepository,
    integrationRepository: IntegrationRepository,
    apiNumberGenerator: ApiNumberGenerator,
    job: ApiNumberGenerationJob
  )

}
