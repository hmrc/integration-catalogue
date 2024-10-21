/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}
import uk.gov.hmrc.integrationcatalogue.repository.{ApiDetailSummaryRepository, IntegrationRepository}
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ApiTestData with OasTestData {

  val mockIntegrationRepo: IntegrationRepository = mock[IntegrationRepository]
  val mockSummaryRepository: ApiDetailSummaryRepository = mock[ApiDetailSummaryRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIntegrationRepo)
    reset(mockSummaryRepository)
  }

  trait Setup {

    val uuid: UUID = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
    val rawData    = "rawOASData"

    val inTest = new IntegrationService(mockIntegrationRepo, mockSummaryRepository)
    val expectedList: List[IntegrationDetail] = List(apiDetail1, apiDetail2)

  }

  "findWithFilters" should {
    "return list of integration details" in new Setup {

      when(mockIntegrationRepo.findWithFilters(any)).thenReturn(Future.successful(IntegrationResponse(count = expectedList.size, results = expectedList)))

      val response: IntegrationResponse = await(inTest.findWithFilters(any))
      response.results shouldBe expectedList

      verify(mockIntegrationRepo).findWithFilters(any)
    }
  }

  "findSummariesWithFilters" should {
    "return list of API Detail summaries" in new Setup {
      val summaries: Seq[ApiDetailSummary] = Seq(apiDetail1, apiDetail2).map(ApiDetailSummary(_))

      when(mockSummaryRepository.findWithFilters(any, any)).thenReturn(Future.successful(summaries))

      val response: Seq[ApiDetailSummary] = await(inTest.findSummariesWithFilters(any, any))
      response should contain theSameElementsAs summaries

      verify(mockSummaryRepository).findWithFilters(any, any)
    }
  }

  "deleteByPlatform" should {
    "delete the integration if the platform matches" in new Setup {
      when(mockIntegrationRepo.deleteByPlatform(any)).thenReturn(Future.successful(1))

      val result: Int = await(inTest.deleteByPlatform(PlatformType.CORE_IF))
      result shouldBe 1

    }
  }

  "deleteById" should {
    "delete the integration if the integration is found" in new Setup {
      when(mockIntegrationRepo.findById(apiDetail0.id)).thenReturn(Future.successful(Some(apiDetail0)))
      when(mockIntegrationRepo.deleteById(apiDetail0.id)).thenReturn(Future.successful(true))

      val result: DeleteApiResult = await(inTest.deleteByIntegrationId(apiDetail0.id))
      result shouldBe NoContentDeleteApiResult

      verify(mockIntegrationRepo).findById(apiDetail0.id)
      verify(mockIntegrationRepo).deleteById(apiDetail0.id)

    }

    "not delete the api and file for a publisher reference if the apidetail is not found" in new Setup {
      when(mockIntegrationRepo.findById(apiDetail0.id)).thenReturn(Future.successful(None))

      val result: DeleteApiResult = await(inTest.deleteByIntegrationId(apiDetail0.id))
      result shouldBe NotFoundDeleteApiResult

      verify(mockIntegrationRepo).findById(apiDetail0.id)
      verifyNoMoreInteractions(mockIntegrationRepo)

    }
  }

  "findByIntegrationId" should {
    "returns integration detail" in new Setup {
      when(mockIntegrationRepo.findById(any[IntegrationId])).thenReturn(Future.successful(Some(apiDetail1)))

      val result: Option[IntegrationDetail] = await(inTest.findById(apiDetail1.id))
      result shouldBe Some(apiDetail1)
      verify(mockIntegrationRepo).findById(apiDetail1.id)
    }
  }

  "getCatalogueReport" should {
    "returns count" in new Setup {
      when(mockIntegrationRepo.getCatalogueReport()).thenReturn(Future.successful(List.empty))
      val result: Seq[IntegrationPlatformReport] = await(inTest.getCatalogueReport())
      result shouldBe List.empty
      verify(mockIntegrationRepo).getCatalogueReport()
    }
  }

  "getFileTransferTransportsByPlatform" should {
    "returns no transports" in new Setup {
      when(mockIntegrationRepo.getFileTransferTransportsByPlatform(Some("a source"), Some("a target"))).thenReturn(Future.successful(List.empty))
      val result: Seq[FileTransferTransportsForPlatform] = await(inTest.getFileTransferTransportsByPlatform(Some("a source"), Some("a target")))
      result shouldBe List.empty
      verify(mockIntegrationRepo).getFileTransferTransportsByPlatform(Some("a source"), Some("a target"))
    }
  }

  "updateApiTeam" should {
    "perform an update on the repository" in new Setup {
      val teamId = "a team id"
      val apiId: IntegrationId = IntegrationId(UUID.randomUUID())
      when(mockIntegrationRepo.updateTeamId(apiId, Some(teamId))).thenReturn(Future.successful(Some(apiDetail1)))

      val result: Option[IntegrationDetail] = await(inTest.updateApiTeam(apiId, Some(teamId)))

      result shouldBe Some(apiDetail1)
      verify(mockIntegrationRepo).updateTeamId(apiId, Some(teamId))
    }
  }

}
