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


import cats.data.*
import cats.data.Validated.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes.*
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.HIP
import uk.gov.hmrc.integrationcatalogue.models.common.SpecificationType
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASParserService
import uk.gov.hmrc.integrationcatalogue.repository.{ApiTeamsRepository, IntegrationRepository}
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class PublishServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ApiTestData with OasTestData with OptionValues {

  val mockOasParserService: OASParserService = mock[OASParserService]
  val mockApiRepo: IntegrationRepository     = mock[IntegrationRepository]
  val mockUuidService: UuidService           = mock[UuidService]
  val mockApiTeamsRepo: ApiTeamsRepository   = mock[ApiTeamsRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOasParserService, mockApiRepo, mockApiTeamsRepo)
  }

  trait Setup {

    val rawData = "rawOASData"

    val inTest = new PublishService(mockOasParserService, mockApiRepo, mockUuidService, mockApiTeamsRepo)

    val publishRequest: PublishRequest = PublishRequest(
      publisherReference = Some(apiDetail0.publisherReference),
      platformType = apiDetail0.platform, specificationType = SpecificationType.OAS_V3, contents = rawData
    )

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
      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublisDetails: PublishDetails = PublishDetails(isUpdate = false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.isDefined shouldBe true
      result.publishDetails.get shouldBe expectedPublisDetails

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0), eqTo(Option.empty))
      verify(mockApiTeamsRepo, never).findByPublisherReference(any())
    }

    "return successful publish result on update" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertSuccessUpdate))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublisDetails: PublishDetails = PublishDetails(isUpdate =  true, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.isDefined shouldBe true
      result.publishDetails.get shouldBe expectedPublisDetails

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0), eqTo(Option.empty))
    }

    "return fail when oas parse fails" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseFailure)

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe false

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verifyNoInteractions(mockApiRepo)
    }

    "return fail when api upsert fails" in new Setup {

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertFailure))
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertFailure))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe false
      result.errors.head.code shouldBe API_UPSERT_ERROR

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0), eqTo(Option.empty))

    }

    "return success when auto-publishing a new API and the team link exists" in new Setup {
      val request: PublishRequest = publishRequest.copy(autopublish = true)
      val apiTeam: ApiTeam = ApiTeam(request.publisherReference.value, "test-team-id")

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.exists(any(), any())).thenReturn(Future.successful(false))
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertSuccessInsert))
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(Some(apiTeam)))

      val result: PublishResult = Await.result(inTest.publishApi(request), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublishDetails: PublishDetails = PublishDetails(isUpdate = false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.value shouldBe expectedPublishDetails

      verify(mockApiRepo).exists(
        eqTo(request.platformType),
        eqTo(request.publisherReference.value)
      )
      verify(mockApiTeamsRepo).findByPublisherReference(eqTo(apiTeam.publisherReference))
    }

    "return success when auto-publishing a new API and the team link does not exist" in new Setup {
      val request: PublishRequest = publishRequest.copy(autopublish = true)

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.exists(any(), any())).thenReturn(Future.successful(false))
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertSuccessInsert))
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(None))

      val result: PublishResult = Await.result(inTest.publishApi(request), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublishDetails: PublishDetails = PublishDetails(isUpdate = false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.value shouldBe expectedPublishDetails

      verify(mockApiRepo).exists(
        eqTo(request.platformType),
        eqTo(request.publisherReference.value)
      )
      verify(mockApiTeamsRepo).findByPublisherReference(eqTo(request.publisherReference.value))
    }

    "return success when auto-publishing an existing API and ignore the existence of a team link" in new Setup {
      val request: PublishRequest = publishRequest.copy(autopublish = true)

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
      when(mockApiRepo.exists(any(), any())).thenReturn(Future.successful(true))
      when(mockApiRepo.findAndModify(any(), any())).thenReturn(Future.successful(apiUpsertSuccessUpdate))

      val result: PublishResult = Await.result(inTest.publishApi(request), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublishDetails: PublishDetails = PublishDetails(isUpdate = true, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.value shouldBe expectedPublishDetails

      verify(mockApiTeamsRepo, never).findByPublisherReference(any())
    }
  }

  "linkApiToTeam" should {
    "pass the request to the ApiTeam repository" in new Setup {
      val apiTeam: ApiTeam = ApiTeam("test-publisher-reference", "test-team-id")

      when(mockApiRepo.findByPublisherRef(eqTo(HIP), eqTo(apiTeam.publisherReference)))
        .thenReturn(Future.successful(None))

      when(mockApiTeamsRepo.upsert(any())).thenReturn(Future.successful(()))

      Await.result(inTest.linkApiToTeam(apiTeam), Duration.apply(500, MILLISECONDS))

      verify(mockApiTeamsRepo).upsert(eqTo(apiTeam))
    }

    "update the API's team if it already exists" in new Setup {
      val apiTeam: ApiTeam = ApiTeam("test-publisher-reference", "test-team-id")

      when(mockApiRepo.findByPublisherRef(eqTo(HIP), eqTo(apiTeam.publisherReference)))
        .thenReturn(Future.successful(Some(apiDetail0)))

      when(mockApiRepo.updateTeamId(any, any)).thenReturn(Future.successful(Some(apiDetail0)))

      when(mockApiTeamsRepo.upsert(any())).thenReturn(Future.successful(()))

      Await.result(inTest.linkApiToTeam(apiTeam), Duration.apply(500, MILLISECONDS))

      // Not using matchers here as they don't work with IntegrationId
      verify(mockApiRepo).updateTeamId(apiDetail0.id, Some(apiTeam.teamId))

      verify(mockApiTeamsRepo).upsert(eqTo(apiTeam))
    }
  }

}
