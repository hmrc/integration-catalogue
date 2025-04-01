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
import play.api.{Configuration, Environment}
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes.*
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.HIP
import uk.gov.hmrc.integrationcatalogue.models.common.SpecificationType
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASParserService
import uk.gov.hmrc.integrationcatalogue.repository.{ApiTeamsRepository, IntegrationRepository}
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}
import uk.gov.hmrc.integrationcatalogue.utils.{ApiNumberExtractor, ApiNumberGenerator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class PublishServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ApiTestData with OasTestData with OptionValues {

  trait Setup {
    val mockOasParserService: OASParserService = mock[OASParserService]
    val mockApiRepo: IntegrationRepository = mock[IntegrationRepository]
    val mockUuidService: UuidService = mock[UuidService]
    val mockApiTeamsRepo: ApiTeamsRepository = mock[ApiTeamsRepository]
    val mockApiNumberExtractor: ApiNumberExtractor = mock[ApiNumberExtractor]
    val mockApiNumberGenerator: ApiNumberGenerator = mock[ApiNumberGenerator]

    val rawData = "rawOASData"
    private val env = Environment.simple()
    private val configuration = Configuration.load(env)
    private val appConfig = new AppConfig(configuration)

    val inTest = new PublishService(mockOasParserService, mockApiRepo, mockUuidService, mockApiTeamsRepo, mockApiNumberExtractor, mockApiNumberGenerator)

    val publishRequest: PublishRequest = PublishRequest(
      publisherReference = Some(apiDetail0.publisherReference),
      platformType = apiDetail0.platform, specificationType = SpecificationType.OAS_V3, contents = rawData
    )

    val parseSuccess: ValidatedNel[List[String], ApiDetail]                    = valid(apiDetail0)
    val parseFailure: ValidatedNel[List[String], ApiDetail]                    = invalid(NonEmptyList[List[String]](List("Oas Parser returned null"), List()))
    val apiUpsertSuccessInsert: Either[Exception, (ApiDetail, Types.IsUpdate)] = Right((apiDetail0, false))
    val apiUpsertSuccessUpdate: Either[Exception, (ApiDetail, Types.IsUpdate)] = Right((apiDetail0, true))
    val apiUpsertFailure: Either[Exception, (ApiDetail, Types.IsUpdate)]       = Left(new Exception(s"ApiDetailParsed upsert error."))

    val generatedApiNumber = "generatedApiNumber"
    val existingApiNumber = "existingApiNumber"
    val extractedApiNumber = "extractedApiNumber"
    val titleAfterApiNumberExtracted = "titleAfterApiNumberExtracted"
    val existingTeamId = "existingTeamId"
    val existingApiDetail = apiDetail0.copy()

    def givenOasParserFindsShortDescription(shortDesc: Option[String]): Unit = {
      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(valid(apiDetail0.copy(shortDescription = shortDesc)))
    }

    def givenApiDoesNotAlreadyExist(): Unit = {
      when(mockApiRepo.findByPublisherRef(any(), any())).thenReturn(Future.successful(None))
    }

    def givenApiAlreadyExistsWithApiNumber(): Unit = {
      when(mockApiRepo.findByPublisherRef(any(), any())).thenReturn(Future.successful(Some(existingApiDetail.copy(apiNumber = Some(existingApiNumber)))))
    }

    def givenApiAlreadyExistsWithApiNumberAndShortDescription(shortDesc: Option[String]): Unit = {
      when(mockApiRepo.findByPublisherRef(any(), any())).thenReturn(Future.successful(Some(existingApiDetail.copy(apiNumber = Some(existingApiNumber), shortDescription = shortDesc))))
    }

    def givenApiAlreadyExistsWithATeamId(): Unit = {
      when(mockApiRepo.findByPublisherRef(any(), any())).thenReturn(Future.successful(Some(existingApiDetail.copy(teamId = Some(existingTeamId)))))
    }

    def givenApiAlreadyExistsWithoutATeamId(): Unit = {
      when(mockApiRepo.findByPublisherRef(any(), any())).thenReturn(Future.successful(Some(existingApiDetail.copy(teamId = None))))
    }

    def givenATeamExistsForTheApiPublisherReference(): Unit = {
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(Some(ApiTeam(apiDetail0.publisherReference, existingTeamId))))
    }

    def givenNoTeamExistsForTheApiPublisherReference(): Unit = {
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(None))
    }

    def givenApiAlreadyExistsWithoutAnApiNumber(): Unit = {
      when(mockApiRepo.findByPublisherRef(any(), any())).thenReturn(Future.successful(Some(existingApiDetail.copy(apiNumber = None))))
    }

    def givenApiNumberGeneratorReturns(returnedApiNumber: Option[String]): Unit = {
      when(mockApiNumberGenerator.generate(any(), any())).thenReturn(Future.successful(returnedApiNumber))
    }

    def givenApiNumberExtractorFindsANumberInTheTitle(extractedApiNumber: String, newTitle: String): Unit = {
      when(mockApiNumberExtractor.extract(any())).thenAnswer(i => i.getArgument(0).asInstanceOf[ApiDetail].copy(apiNumber = Some(extractedApiNumber), title = newTitle))
    }

    def givenApiNumberExtractorDoesNotFindANumberInTheTitle(): Unit = {
      when(mockApiNumberExtractor.extract(any())).thenAnswer(i => i.getArgument(0).asInstanceOf[ApiDetail])
    }

    def thenRepoStoresCorrectApiDetails(expectedApiNumber: Option[String], expectedTeamId: Option[String] = None, apiTitle: String = apiDetail0.title, shortDescription: Option[String] = None): Unit = {
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0.copy(apiNumber = expectedApiNumber, teamId = expectedTeamId, title = apiTitle, shortDescription = shortDescription)))
    }

    when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(None))
    when(mockApiRepo.findAndModify(any())).thenReturn(Future.successful(apiUpsertSuccessInsert))
    when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(parseSuccess)
  }

  "publish" should {
    "set correct values when new HIP API is published" in new Setup {
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(Some(generatedApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(generatedApiNumber))
    }

    "set correct values when existing HIP API with API number is re-published" in new Setup {
      private val shortDescription: Some[String] = Some(s"API#$extractedApiNumber")
      givenApiAlreadyExistsWithApiNumberAndShortDescription(shortDescription)
      givenApiNumberGeneratorReturns(Some(existingApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()
      givenOasParserFindsShortDescription(Some(s"API#$existingApiNumber"))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(expectedApiNumber = Some(existingApiNumber), shortDescription = Some(s"API#$existingApiNumber"))
    }

    "set correct values when existing HIP API without an API number is re-published" in new Setup {
      givenApiAlreadyExistsWithoutAnApiNumber()
      givenApiNumberGeneratorReturns(Some(existingApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(existingApiNumber))
    }

    "set correct values when HIP API with an API number in the title is published" in new Setup {
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(Some(generatedApiNumber))
      givenApiNumberExtractorFindsANumberInTheTitle(extractedApiNumber, titleAfterApiNumberExtracted)
      givenOasParserFindsShortDescription(Some("a short description."))

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(generatedApiNumber), None, titleAfterApiNumberExtracted, Some(s"a short description. API#$extractedApiNumber"))
    }

    "set correct values when non-HIP API without an API number in the title is published" in new Setup {
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(None)
    }

    "set correct values when non-HIP API with an API number in the title is published" in new Setup {
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)
      givenApiNumberExtractorFindsANumberInTheTitle(extractedApiNumber, titleAfterApiNumberExtracted)

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(extractedApiNumber), None, titleAfterApiNumberExtracted, Some(s"API#$extractedApiNumber"))
    }

    "set correct values when an existing non-HIP API with an API number in the title is re-published with a new number in the title" in new Setup {
      givenApiAlreadyExistsWithApiNumber()
      givenApiNumberGeneratorReturns(None)
      givenApiNumberExtractorFindsANumberInTheTitle(extractedApiNumber, titleAfterApiNumberExtracted)

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(extractedApiNumber), None, titleAfterApiNumberExtracted, Some(s"API#$extractedApiNumber"))
    }

    "set correct values when an existing non-HIP API with an API number in the title is re-published with a new number in the title and no prior short description" in new Setup {
      givenApiAlreadyExistsWithApiNumberAndShortDescription(None)
      givenApiNumberGeneratorReturns(None)
      givenApiNumberExtractorFindsANumberInTheTitle(extractedApiNumber, titleAfterApiNumberExtracted)
      givenOasParserFindsShortDescription(None)
      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(extractedApiNumber), None, titleAfterApiNumberExtracted, Some(s"API#$extractedApiNumber"))
    }

    "set correct values when an existing non-HIP API with an API number in the title is re-published with a new number in the title and a prior API number in the short description" in new Setup {
      private val shortDescription: Some[String] = Some("cheesecake API#oldnumber")
      givenApiAlreadyExistsWithApiNumberAndShortDescription(shortDescription)
      givenApiNumberGeneratorReturns(None)
      givenApiNumberExtractorFindsANumberInTheTitle(extractedApiNumber, titleAfterApiNumberExtracted)
      givenOasParserFindsShortDescription(shortDescription)
      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(extractedApiNumber), None, titleAfterApiNumberExtracted, Some(s"cheesecake API#oldnumber API#$extractedApiNumber"))
    }

    "set correct values when an existing API has a teamId" in new Setup {
      givenApiAlreadyExistsWithATeamId()
      givenATeamExistsForTheApiPublisherReference()
      givenApiNumberGeneratorReturns(Some(generatedApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(generatedApiNumber), Some(existingTeamId))
    }

    "set correct values when an existing API has no teamId" in new Setup {
      givenApiAlreadyExistsWithoutATeamId()
      givenATeamExistsForTheApiPublisherReference()
      givenApiNumberGeneratorReturns(Some(generatedApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(generatedApiNumber), None)
    }

    "set correct values when an existing API has no teamId but the publisher reference matches an existing team" in new Setup {
      givenApiAlreadyExistsWithATeamId()
      givenApiNumberGeneratorReturns(Some(generatedApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(generatedApiNumber), Some(existingTeamId))
    }

    "set correct values when an existing API has no teamId and there is no team linked with the publisher reference" in new Setup {
      givenApiAlreadyExistsWithoutATeamId()
      givenNoTeamExistsForTheApiPublisherReference()
      givenApiNumberGeneratorReturns(Some(generatedApiNumber))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()

      val result: PublishResult = Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      thenRepoStoresCorrectApiDetails(Some(generatedApiNumber))
    }

    "return successful publish result on insert" in new Setup {
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)
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
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0))
    }

    "return successful publish result on update" in new Setup {
      when(mockApiRepo.findAndModify(any())).thenReturn(Future.successful(apiUpsertSuccessUpdate))
      givenApiNumberExtractorDoesNotFindANumberInTheTitle()
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)

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
      verify(mockApiRepo).findAndModify(eqTo(apiDetail0))
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
      when(mockApiNumberExtractor.extract(any())).thenReturn(apiDetail0)
      when(mockApiRepo.findAndModify(any())).thenReturn(Future.successful(apiUpsertFailure))
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)

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

    "return success when auto-publishing a new API and the team link exists" in new Setup {
      val request: PublishRequest = publishRequest.copy(autopublish = true)
      val apiTeam: ApiTeam = ApiTeam(request.publisherReference.value, "test-team-id")

      when(mockApiNumberExtractor.extract(any())).thenReturn(apiDetail0)
      when(mockApiTeamsRepo.findByPublisherReference(any())).thenReturn(Future.successful(Some(apiTeam)))
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)

      val result: PublishResult = Await.result(inTest.publishApi(request), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublishDetails: PublishDetails = PublishDetails(isUpdate = false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.value shouldBe expectedPublishDetails

      verify(mockApiTeamsRepo).findByPublisherReference(eqTo(apiTeam.publisherReference))
    }

    "return success when auto-publishing a new API and the team link does not exist" in new Setup {
      val request: PublishRequest = publishRequest.copy(autopublish = true)

      when(mockApiNumberExtractor.extract(any())).thenReturn(apiDetail0)
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)

      val result: PublishResult = Await.result(inTest.publishApi(request), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublishDetails: PublishDetails = PublishDetails(isUpdate = false, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.value shouldBe expectedPublishDetails

      verify(mockApiTeamsRepo).findByPublisherReference(eqTo(request.publisherReference.value))
    }

    "return success when auto-publishing an existing API and ignore the existence of a team link" in new Setup {
      val request: PublishRequest = publishRequest.copy(autopublish = true)

      when(mockApiNumberExtractor.extract(any())).thenReturn(apiDetail0)
      when(mockApiRepo.exists(any(), any())).thenReturn(Future.successful(true))
      when(mockApiRepo.findAndModify(any())).thenReturn(Future.successful(apiUpsertSuccessUpdate))
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)

      val result: PublishResult = Await.result(inTest.publishApi(request), Duration.apply(500, MILLISECONDS))
      result.isSuccess shouldBe true

      val expectedPublishDetails: PublishDetails = PublishDetails(isUpdate = true, apiDetail0.id, apiDetail0.publisherReference, apiDetail0.platform)
      result.publishDetails.value shouldBe expectedPublishDetails
    }

    "extract valid API numbers when they appear in the API title" in new Setup {
      val apiDetailWithExtractedNumber = apiDetail0.copy(apiNumber = Some("API123"), title = "my api title", shortDescription = Some("API#API123"))
      when(mockApiNumberExtractor.extract(eqTo(apiDetail0))).thenReturn(apiDetailWithExtractedNumber)

      when(mockOasParserService.parse(any(), any(), any(), any())).thenReturn(valid(apiDetail0))
      when(mockApiRepo.exists(any(), any())).thenReturn(Future.successful(false))
      givenApiDoesNotAlreadyExist()
      givenApiNumberGeneratorReturns(None)

      Await.result(inTest.publishApi(publishRequest), Duration.apply(500, MILLISECONDS))

      verify(mockApiRepo).findAndModify(apiDetailWithExtractedNumber)
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
