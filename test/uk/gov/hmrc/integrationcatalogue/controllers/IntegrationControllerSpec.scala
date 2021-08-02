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

package uk.gov.hmrc.integrationcatalogue.controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus._
import uk.gov.hmrc.integrationcatalogue.service.IntegrationService
import uk.gov.hmrc.integrationcatalogue.testdata.ApiTestData
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.ValidateQueryParamKeyAction

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationControllerSpec extends WordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with ApiTestData with BeforeAndAfterEach {

  private val fakeRequest = FakeRequest("GET", "/apis")

  private val mockApiService = mock[IntegrationService]
  private val validateQueryParamKeyAction = app.injector.instanceOf[ValidateQueryParamKeyAction]

  private val controller = new IntegrationController(Helpers.stubControllerComponents(), mockApiService, validateQueryParamKeyAction)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApiService)
  }

  trait Setup {

    val schema1 = DefaultSchema(
      name = Some("agentReferenceNumber"),
      not = None,
      `type` = Some("string"),
      pattern = Some("^[A-Z](ARN)[0-9]{7}$"),
      description = None,
      ref = None,
      properties = List.empty,
      `enum` = List.empty,
      required = List.empty,
      stringAttributes = None,
      numberAttributes = None,
      minProperties = None,
      maxProperties = None,
      format = None,
      default = None,
      example = None
    )

    val schema2 = DefaultSchema(
      name = Some("agentReferenceNumber"),
      not = None,
      `type` = Some("object"),
      pattern = None,
      description = None,
      ref = None,
      properties = List(schema1),
      `enum` = List.empty,
      required = List.empty,
      stringAttributes = None,
      numberAttributes = None,
      minProperties = None,
      maxProperties = None,
      format = None,
      default = None,
      example = None
    )

    val filename = "API1689_Get_Known_Facts_1.1.0.yaml"
    val fileContents = "{}"
    val uuid = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
    val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));
    val reviewedDate = DateTime.parse("25/12/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));

    val apiPlatformMaintainer: Maintainer = Maintainer("API Platform Team", "#team-api-platform-sup")
    val coreIfMaintainer: Maintainer = Maintainer("IF Team", "N/A", List.empty)

    val jsonMediaType = "application/json"

    val exampleRequest1name = "example request 1"
    val exampleRequest1Body = "{\"someValue\": \"abcdefg\"}"
    val exampleRequest1: Example = Example(exampleRequest1name, exampleRequest1Body)

    val exampleResponse1 = new Example("example response name", "example response body")

    val request = Request(description = Some("request"), schema = Some(schema1), mediaType = Some(jsonMediaType), examples = List(exampleRequest1))
    val response = Response(statusCode = "200", description = Some("response"), schema = Some(schema2), mediaType = Some("application/json"), examples = List(exampleResponse1))
    val endpointGetMethod = EndpointMethod("GET", Some("operationId"), Some("some summary"), Some("some description"), None, List(response))
    val endpointPutMethod = EndpointMethod("PUT", Some("operationId2"), Some("some summary"), Some("some description"), Some(request), List.empty)
    val endpoint1 = Endpoint("/some/url", List(endpointGetMethod, endpointPutMethod))

    val endpoints = List(endpoint1, Endpoint("/some/url", List.empty))

    val exampleApiDetail: ApiDetail = ApiDetail(
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
      components = Components(List.empty, List.empty),
      shortDescription = None,
      openApiSpecification = "OAS content",
      apiStatus = LIVE,
    reviewedDate = reviewedDate
    )

    val fakeDeleteRequest = FakeRequest("DELETE", s"/apis/${exampleApiDetail.publisherReference}")

  }

  "GET /integrations/search-with-filter" should {
    "return 200" in {
      when(mockApiService.findWithFilters(*)).thenReturn(Future.successful(IntegrationResponse(count = 1, results = apiList)))
      val result: Future[Result] = controller.findWithFilters(List(""), List(PlatformType.CORE_IF), List.empty, None, None)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 when no results returned" in {
      when(mockApiService.findWithFilters(*)).thenReturn(Future.successful(IntegrationResponse(count = 0, results = List.empty)))
      val result = controller.findWithFilters(List(""), List(PlatformType.CORE_IF), List.empty, None, None)(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "GET /integrations/:id" should {
    "return 200" in {
      when(mockApiService.findById(eqTo(apiDetail0.id))).thenReturn(Future.successful(Some(apiDetail0)))
      val result = controller.findById(apiDetail0.id)(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "DELETE /integrations/:publisherReference" should {
    "return 204 when valid publisherReference and delete is successful" in new Setup {
      when(mockApiService.deleteByIntegrationId(*[IntegrationId])(*)).thenReturn(Future.successful(NoContentDeleteApiResult))

      val result: Future[Result] = controller.deleteByIntegrationId(exampleApiDetail.id)(fakeDeleteRequest)
      status(result) shouldBe Status.NO_CONTENT

      verify(mockApiService).deleteByIntegrationId(exampleApiDetail.id)

    }

    "return 404 when invalid publisherReference and delete is unsuccessful" in new Setup {
      when(mockApiService.deleteByIntegrationId(*[IntegrationId])(*)).thenReturn(Future.successful(NotFoundDeleteApiResult))

      val result: Future[Result] = controller.deleteByIntegrationId(exampleApiDetail.id)(fakeDeleteRequest)
      status(result) shouldBe Status.NOT_FOUND

      verify(mockApiService).deleteByIntegrationId(exampleApiDetail.id)

    }
  }
}
