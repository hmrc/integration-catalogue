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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import cats.data.Validated._
import cats.data._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.SpecificationType
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASParserService
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

class PublishServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ApiTestData with OasTestData {

  val mockOasParserService: OASParserService = mock[OASParserService]
  val mockApiRepo: IntegrationRepository     = mock[IntegrationRepository]
  val mockUuidService                        = mock[UuidService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOasParserService, mockApiRepo)
  }

  trait Setup {

    val rawData = "rawOASData"

    val inTest = new PublishService(mockOasParserService, mockApiRepo, mockUuidService)

    val publishRequest: PublishRequest                                         =
      PublishRequest(publisherReference = Some(apiDetail0.publisherReference), platformType = apiDetail0.platform, specificationType = SpecificationType.OAS_V3, contents = rawData)
    val parseSuccess: ValidatedNel[List[String], ApiDetail]                    = valid(apiDetail0)
    val parseFailure: ValidatedNel[List[String], ApiDetail]                    = invalid(NonEmptyList[List[String]](List("Oas Parser returned null"), List()))
    val apiUpsertSuccessInsert: Either[Exception, (ApiDetail, Types.IsUpdate)] = Right(apiDetail0, false)
    val apiUpsertSuccessUpdate: Either[Exception, (ApiDetail, Types.IsUpdate)] = Right(apiDetail0, true)
    val apiUpsertFailure: Either[Exception, (ApiDetail, Types.IsUpdate)]       = Left(new Exception(s"ApiDetailParsed upsert error."))
  }

  "publish" should {
    "return successful publish result on insert" in new Setup {

      when(mockOasParserService.parse(*, *, *, *)).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(*)).thenReturn(Future.successful(apiUpsertSuccessInsert))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublisDetails: PublishDetails = PublishDetails(false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.isDefined shouldBe true
      result.publishDetails.get shouldBe expectedPublisDetails

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0))

    }

    "return successful publish result on update" in new Setup {

      when(mockOasParserService.parse(*, *, *, *)).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(*)).thenReturn(Future.successful(apiUpsertSuccessUpdate))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublisDetails: PublishDetails = PublishDetails(true, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.isDefined shouldBe true
      result.publishDetails.get shouldBe expectedPublisDetails

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0))
    }

    "return fail when oas parse fails" in new Setup {

      when(mockOasParserService.parse(*, *, *, *)).thenReturn(parseFailure)

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe false

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verifyZeroInteractions(mockApiRepo)
    }

    "return fail when api upsert fails" in new Setup {

      when(mockOasParserService.parse(*, *, *, *)).thenReturn(parseSuccess)
      when(mockApiRepo.findAndModify(*)).thenReturn(Future.successful(apiUpsertFailure))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe false
      result.errors.head.code shouldBe API_UPSERT_ERROR

      verify(mockOasParserService).parse(
        eqTo(publishRequest.publisherReference),
        eqTo(publishRequest.platformType),
        eqTo(publishRequest.specificationType),
        eqTo(publishRequest.contents)
      )
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0))

    }

  }

}
