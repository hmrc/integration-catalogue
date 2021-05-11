/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.scalatest.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.testdata.OasTestData
import uk.gov.hmrc.integrationcatalogue.testdata.ApiTestData

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationServiceSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ApiTestData with OasTestData {

  val mockIntegrationRepo: IntegrationRepository = mock[IntegrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIntegrationRepo)
  }

  trait Setup {

    val uuid: UUID = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
    val rawData = "rawOASData"


    val inTest = new IntegrationService(mockIntegrationRepo)
    val expectedList = List(apiDetail1, apiDetail2)

  }


"findWithFilters" should {
    "return list of integration details" in new Setup {

      when(mockIntegrationRepo.findWithFilters(*)).thenReturn(Future.successful(IntegrationResponse(count = expectedList.size, results = expectedList)))

     val response: IntegrationResponse =  await(inTest.findWithFilters(*))
     response.results shouldBe expectedList

      verify(mockIntegrationRepo).findWithFilters(*)
    }
  }

    "deleteByPlatform" should {
    "delete the integration if the platform matches" in new Setup {
      when(mockIntegrationRepo.deleteByPlatform(*)).thenReturn(Future.successful(1))
      
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
      verifyZeroInteractions(mockIntegrationRepo)

    }
  }

  "findByIntegrationId" should {
    "returns integration detail" in new Setup {

      when(mockIntegrationRepo.findById(eqTo(apiDetail1.id))).thenReturn(Future.successful(Some(apiDetail1)))

     val result: Option[IntegrationDetail] = await(inTest.findById(apiDetail1.id))
      result shouldBe Some(apiDetail1)
      verify(mockIntegrationRepo).findById(eqTo(apiDetail1.id))
    }
  }

}
