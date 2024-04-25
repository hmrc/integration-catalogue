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

package uk.gov.hmrc.integrationcatalogue.service

import cats.data.Validated._
import cats.data._
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.SpecificationType
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASParserService
import uk.gov.hmrc.integrationcatalogue.repository.{ApiTeamsRepository, IntegrationRepository}
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PublishServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ApiTestData with OasTestData {

  val mockOasParserService: OASParserService = mock[OASParserService]
  val mockApiRepo: IntegrationRepository     = mock[IntegrationRepository]
  val mockUuidService: UuidService           = mock[UuidService]
  val mockApiTeamsRepo: ApiTeamsRepository   = mock[ApiTeamsRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOasParserService, mockApiRepo)
  }

  trait Setup {

    val rawData = "rawOASData"

    val inTest = new PublishService(mockOasParserService, mockApiRepo, mockUuidService, mockApiTeamsRepo)

    val publishRequest: PublishRequest                                         =
      PublishRequest(publisherReference = Some(apiDetail0.publisherReference), platformType = apiDetail0.platform, specificationType = SpecificationType.OAS_V3, contents = rawData)
    val parseSuccess: ValidatedNel[List[String], ApiDetail]                    = valid(apiDetail0)
    val parseFailure: ValidatedNel[List[String], ApiDetail]                    = invalid(NonEmptyList[List[String]](List("Oas Parser returned null"), List()))
    val apiUpsertSuccessInsert: Either[Exception, (ApiDetail, Types.IsUpdate)] = Right((apiDetail0, false))
    val apiUpsertSuccessUpdate: Either[Exception, (ApiDetail, Types.IsUpdate)] = Right((apiDetail0, true))
    val apiUpsertFailure: Either[Exception, (ApiDetail, Types.IsUpdate)]       = Left(new Exception(s"ApiDetailParsed upsert error."))
  }

  "publish" should {
    "return successful publish result on insert" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertSuccessInsert))
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(Option.empty))
      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublisDetails: PublishDetails = PublishDetails(false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.isDefined shouldBe true
      result.publishDetails.get shouldBe expectedPublisDetails

      verify(mockOasParserService).parse(
        ArgumentMatchers.eq(publishRequest.publisherReference),
        ArgumentMatchers.eq(publishRequest.platformType),
        ArgumentMatchers.eq(publishRequest.specificationType),
        ArgumentMatchers.eq(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(ArgumentMatchers.eq(apiDetail0), ArgumentMatchers.eq(Option.empty))

    }

    "return successful publish result on update" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertSuccessUpdate))
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(Option.empty))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublisDetails: PublishDetails = PublishDetails(true, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.isDefined shouldBe true
      result.publishDetails.get shouldBe expectedPublisDetails

      verify(mockOasParserService).parse(
        ArgumentMatchers.eq(publishRequest.publisherReference),
        ArgumentMatchers.eq(publishRequest.platformType),
        ArgumentMatchers.eq(publishRequest.specificationType),
        ArgumentMatchers.eq(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(ArgumentMatchers.eq(apiDetail0), ArgumentMatchers.eq(Option.empty))
    }

    "return fail when oas parse fails" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseFailure)

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe false

      verify(mockOasParserService).parse(
        ArgumentMatchers.eq(publishRequest.publisherReference),
        ArgumentMatchers.eq(publishRequest.platformType),
        ArgumentMatchers.eq(publishRequest.specificationType),
        ArgumentMatchers.eq(publishRequest.contents)
      )
      verifyZeroInteractions(mockApiRepo)
    }

    "return fail when api upsert fails" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertFailure))
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertFailure))
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(Option.empty))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe false
      result.errors.head.code shouldBe API_UPSERT_ERROR

      verify(mockOasParserService).parse(
        ArgumentMatchers.eq(publishRequest.publisherReference),
        ArgumentMatchers.eq(publishRequest.platformType),
        ArgumentMatchers.eq(publishRequest.specificationType),
        ArgumentMatchers.eq(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(ArgumentMatchers.eq(apiDetail0), ArgumentMatchers.eq(Option.empty))

    }

  }

  "linkApiToTeam" should {
    "pass the request to the ApiTeam repository" in new Setup {
      val apiTeam: ApiTeam = ApiTeam("test-publisher-reference", "test-team-id")
      when(mockApiTeamsRepo.upsert(any())).thenReturn(Future.successful(()))

      Await.result(inTest.linkApiToTeam(apiTeam), Duration.apply(500, MILLISECONDS))

      verify(mockApiTeamsRepo).upsert(ArgumentMatchers.eq(apiTeam))
    }
  }

}
