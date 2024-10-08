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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.service.PublishService
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, FakeIdentifierAction}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class PublishControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with ApiTestData {

  implicit def mat: org.apache.pekko.stream.Materializer = app.injector.instanceOf[org.apache.pekko.stream.Materializer]

  private val mockPublishService = mock[PublishService]

  private val controller = new PublishController(
    Helpers.stubControllerComponents(),
    mockPublishService,
    new FakeIdentifierAction(Helpers.stubPlayBodyParsers(mat))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPublishService)
  }

  private val publishRequestObj = PublishRequest(
    publisherReference = Some(apiDetail0.publisherReference),
    platformType = apiDetail0.platform,
    contents = "{}",
    specificationType = SpecificationType.OAS_V3
  )

  private val fileTransferPublishRequestObj  = FileTransferPublishRequest(
    fileTransferSpecificationVersion = "1.0",
    publisherReference = "BVD-DPS-PCPMonthly-pull",
    title = "BVD-DPS-PCPMonthly-pull",
    description = "A file transfer from Birth Verification Data (BVD) to Data Provisioning Systems (DPS)",
    platformType = PlatformType.CORE_IF,
    lastUpdated = dateValue,
    reviewedDate = reviewedDate,
    contact = ContactInformation(Some("Core IF Team"), Some("example@gmail.com")),
    sourceSystem = List("BVD"),
    targetSystem = List("DPS"),
    transports = List("S3"),
    fileTransferPattern = "Corporate to corporate"
  )

  private val publishRequest                 = Json.toJson(publishRequestObj)
  private val fileTransferPublishRequest     = Json.toJson(fileTransferPublishRequestObj)
  private val fakeApiPublishRequest          = FakeRequest("PUT", s"/api/publish").withBody(publishRequest)
  private val fakeFileTransferPublishRequest = FakeRequest("PUT", s"/filetransfer/publish").withBody(fileTransferPublishRequest)

  private val publishRequestInvalidPlatform = {
    """{"publisherReference":"API1689","platform":"RUBBISH","fileName":"API1689_Get_Known_Facts_1.1.0.yaml","contents":"{}", "specificationType":"OAS_V3"}"""
  }

  private val fileTransferPublishRequestInvalidPlatform = """{
                                                    |    "publisherReference" : "BVD-DPS-PCPMonthly-pull",
                                                    |    "title" : "BVD-DPS-PCPMonthly-pull",
                                                    |    "description" : "A file transfer from Birth Verification Data (BVD) to Data Provisioning Systems (DPS)",
                                                    |    "platformType" : "INVALID_PLATFORM",
                                                    |    "maintainer" : {
                                                    |        "name" : "IF Team",
                                                    |        "slackChannel" : "N/A",
                                                    |        "contactInfo" : []
                                                    |    },
                                                    |    "flowId" : "BVD-DPS-PCPMonthly-pull",
                                                    |    "sourceSystem": "BVD",
                                                    |    "targetSystem": "DPS",
                                                    |    "fileTransferPattern": "Corporate to corporate"
                                                    |}""".stripMargin

  "PUT /apis/publish " should {
    "return 200" in {
      when(mockPublishService.publishApi(any())(any())).thenReturn(Future.successful(PublishResult(isSuccess = true)))
      val result: Future[Result] = controller.publishApi()(
        fakeApiPublishRequest
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            FakeIdentifierAction.fakeAuthorizationHeader
          )
      )

      status(result) shouldBe Status.OK
      val publishResult = contentAsJson(result).as[PublishResult]
      publishResult.isSuccess shouldBe true

      verify(mockPublishService).publishApi(eqTo(publishRequestObj))(any())

    }

    "return 400 when platform in request is invalid" in {

      val result = controller.publishApi()(
        fakeApiPublishRequest
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            FakeIdentifierAction.fakeAuthorizationHeader
          )
          .withBody(Json.parse(publishRequestInvalidPlatform))
      )

      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 401 when the request is not authenticated" in {
      val result = controller.publishApi()(
        fakeApiPublishRequest
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
          )
          .withBody(Json.parse(publishRequestInvalidPlatform))
      )

      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

  "POST /apis/team" should {
    "pass the request to the service and return NoContent on success" in {
      val apiTeam = ApiTeam("test-publisher-reference", "test-team-id")

      when(mockPublishService.linkApiToTeam(any())).thenReturn(Future.successful(()))

      val result = controller.linkApiToTeam()(
        FakeRequest(routes.PublishController.linkApiToTeam())
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            FakeIdentifierAction.fakeAuthorizationHeader
          )
          .withBody(Json.toJson(apiTeam))
      )

      status(result) shouldBe NO_CONTENT
      verify(mockPublishService).linkApiToTeam(eqTo(apiTeam))
    }

    "return 400 BadRequest when the request body is not a valid ApiTeam" in {
      val result = controller.linkApiToTeam()(
        FakeRequest(routes.PublishController.linkApiToTeam())
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            FakeIdentifierAction.fakeAuthorizationHeader
          )
          .withBody(Json.obj())
      )

      status(result) shouldBe BAD_REQUEST
    }

    "return 401 when the request is not authenticated" in {
      val result = controller.linkApiToTeam()(
        FakeRequest(routes.PublishController.linkApiToTeam())
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json"
          )
          .withBody(Json.obj())
      )

      status(result) shouldBe UNAUTHORIZED
    }
  }

  "PUT /filetransfer/publish" should {
    "return 200" in {
      when(mockPublishService.publishFileTransfer(any())(any())).thenReturn(Future.successful(PublishResult(isSuccess = true)))
      val result: Future[Result] = controller.publishFileTransfer()(
        fakeFileTransferPublishRequest
          .withHeaders(
            HeaderNames.CONTENT_TYPE -> "application/json",
            FakeIdentifierAction.fakeAuthorizationHeader
          )
      )

      status(result) shouldBe Status.OK

      val publishResult = contentAsJson(result).as[PublishResult]
      publishResult.isSuccess shouldBe true

      verify(mockPublishService).publishFileTransfer(eqTo(fileTransferPublishRequestObj))(any())

    }

    "return 400 when platform in request is invalid" in {

      val result = controller.publishFileTransfer()(fakeFileTransferPublishRequest
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json",
          FakeIdentifierAction.fakeAuthorizationHeader
        )
        .withBody(Json.parse(fileTransferPublishRequestInvalidPlatform)))

      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 401 when the request is not authenticated" in {
      val result = controller.publishFileTransfer()(fakeFileTransferPublishRequest
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> "application/json"
        )
        .withBody(Json.parse(fileTransferPublishRequestInvalidPlatform)))

      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

}
