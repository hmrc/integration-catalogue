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

package uk.gov.hmrc.integrationcatalogue.controllers

import org.apache.pekko.stream.testkit.NoMaterializer
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.{ValidateFileTransferWizardQueryParamKeyAction, ValidateQueryParamKeyAction}
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.service.IntegrationService
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, FakeIdentifierAction}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with ApiTestData with BeforeAndAfterEach {

  private val fakeRequest = FakeRequest("GET", "/apis")
  private val fakeAuthenticatedRequest = fakeRequest.withHeaders(FakeIdentifierAction.fakeAuthorizationHeader)

  private val mockApiService                                = mock[IntegrationService]
  private val validateQueryParamKeyAction                   = app.injector.instanceOf[ValidateQueryParamKeyAction]
  private val validateFileTransferWizardQueryParamKeyAction = app.injector.instanceOf[ValidateFileTransferWizardQueryParamKeyAction]

  private val controller = new IntegrationController(
    Helpers.stubControllerComponents(),
    mockApiService,
    validateQueryParamKeyAction,
    validateFileTransferWizardQueryParamKeyAction,
    new FakeIdentifierAction(Helpers.stubPlayBodyParsers(NoMaterializer))
  )

  private val exampleApiDetail: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("e2e4ce48-29b0-11eb-adc1-0242ac120002")),
    publisherReference = "API1689",
    title = "getKnownFactsName",
    description = "getKnownFactsDesc",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints,
    shortDescription = None,
    openApiSpecification = "OAS content",
    apiStatus = LIVE,
    reviewedDate = reviewedDate,
    scopes = Set.empty
  )

  private val fakeDeleteRequest = FakeRequest("DELETE", s"/apis/${exampleApiDetail.publisherReference}")
  private val fakeAuthenticatedDeleteRequest = fakeDeleteRequest.withHeaders(FakeIdentifierAction.fakeAuthorizationHeader)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApiService)
  }

  "GET /integrations" should {
    "return 200" in {
      when(mockApiService.findWithFilters(any())).thenReturn(Future.successful(IntegrationResponse(count = 1, results = apiList)))
      val result: Future[Result] = controller.findWithFilters(List(""), List(PlatformType.CORE_IF), List.empty, None, None, None)(fakeAuthenticatedRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 when no results returned" in {
      when(mockApiService.findWithFilters(any())).thenReturn(Future.successful(IntegrationResponse(count = 0, results = List.empty)))
      val result = controller.findWithFilters(List(""), List(PlatformType.CORE_IF), List.empty, None, None, None)(fakeAuthenticatedRequest)
      status(result) shouldBe Status.OK
    }

    "return 401 when the request is not authenticated" in {
      val result = controller.findWithFilters(List(""), List(PlatformType.CORE_IF), List.empty, None, None, None)(fakeRequest)
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

  "GET /integrations/:id" should {
    "return 200" in {
      when(mockApiService.findById(apiDetail0.id)).thenReturn(Future.successful(Some(apiDetail0)))
      val result = controller.findById(apiDetail0.id)(fakeAuthenticatedRequest)
      status(result) shouldBe Status.OK

    }

    "return 401 when the request is not authenticated" in {
      val result = controller.findById(apiDetail0.id)(fakeRequest)
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

  "DELETE /integrations/:id" should {
//    "return 204 when valid publisherReference and delete is successful" in {
//      when(mockApiService.deleteByIntegrationId(any[IntegrationId])(any())).thenReturn(Future.successful(NoContentDeleteApiResult))
//
//      val result: Future[Result] = controller.deleteByIntegrationId(exampleApiDetail.id)(fakeAuthenticatedDeleteRequest)
//      status(result) shouldBe Status.NO_CONTENT
//
//      verify(mockApiService).deleteByIntegrationId(exampleApiDetail.id)
//
//    }

//    "return 404 when invalid publisherReference and delete is unsuccessful" in {
//      when(mockApiService.deleteByIntegrationId(any[IntegrationId])(any())).thenReturn(Future.successful(NotFoundDeleteApiResult))
//
//      val result: Future[Result] = controller.deleteByIntegrationId(exampleApiDetail.id)(fakeAuthenticatedDeleteRequest)
//      status(result) shouldBe Status.NOT_FOUND
//
//      verify(mockApiService).deleteByIntegrationId(exampleApiDetail.id)
//
//    }

    "return 401 when the request is not authenticated" in {
      val result: Future[Result] = controller.deleteByIntegrationId(exampleApiDetail.id)(fakeDeleteRequest)
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

  "DELETE /integrations" should {
    "return 401 when the request is not authenticated" in {
      val result = controller.deleteWithFilters(List.empty)(FakeRequest())
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

}
