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
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.service.PublishService
import uk.gov.hmrc.integrationcatalogue.testdata.ApiTestData

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PublishControllerSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with ApiTestData {

  implicit def mat: akka.stream.Materializer = app.injector.instanceOf[akka.stream.Materializer]

  private val mockPublishService = mock[PublishService]

  private val controller = new PublishController(Helpers.stubControllerComponents(), mockPublishService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPublishService)
  }

  trait Setup {

    val fileContents = "{}"
    val uuid = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
    val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));
    val reviewedDate: DateTime = DateTime.parse("25/12/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));

    val apiPlatformMaintainer: Maintainer = Maintainer("API Platform Team", "#team-api-platform-sup")
    val coreIfMaintainer: Maintainer = Maintainer("IF Team", "N/A", List.empty)

    val jsonMediaType = "application/json"

    val exampleRequest1name = "example request 1"
    val exampleRequest1Body = "{\"someValue\": \"abcdefg\"}"


    val publishRequestObj = PublishRequest(
      publisherReference = Some(apiDetail0.publisherReference),
      platformType = apiDetail0.platform,
      contents = fileContents,
      specificationType = SpecificationType.OAS_V3
    )

    val fileTransferPublishRequestObj = FileTransferPublishRequest(
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
    val publishRequest = Json.toJson(publishRequestObj)
    val fileTransferPublishRequest = Json.toJson(fileTransferPublishRequestObj)
    val fakeApiPublishRequest = FakeRequest("PUT", s"/api/publish").withBody(publishRequest)
    val fakeFileTransferPublishRequest = FakeRequest("PUT", s"/filetransfer/publish").withBody(fileTransferPublishRequest)

    val publishRequestInvalidPlatform = {
      """{"publisherReference":"API1689","platform":"RUBBISH","fileName":"API1689_Get_Known_Facts_1.1.0.yaml","contents":"{}", "specificationType":"OAS_V3"}"""
    }


    val fileTransferPublishRequestInvalidPlatform = """{
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

  }

  "PUT /apis/publish " should {
    "return 200" in new Setup {
      when(mockPublishService.publishApi(*)(*)).thenReturn(Future.successful(PublishResult(isSuccess = true)))
      val result: Future[Result] = controller.publishApi()(fakeApiPublishRequest.withHeaders(HeaderNames.CONTENT_TYPE -> "application/json"))

      status(result) shouldBe Status.OK
      val publishResult = contentAsJson(result).as[PublishResult]
      publishResult.isSuccess shouldBe true

      verify(mockPublishService).publishApi(eqTo(publishRequestObj))(*)

    }

    "return 400 when platform in request is invalid" in new Setup {

      val result = controller.publishApi()(fakeApiPublishRequest
        .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
        .withBody(Json.parse(publishRequestInvalidPlatform)))

      status(result) shouldBe Status.BAD_REQUEST
    }
  }
  "PUT /filetransfer/publish" should {
    "return 200" in new Setup {
      when(mockPublishService.publishFileTransfer(*)(*)).thenReturn(Future.successful(PublishResult(isSuccess = true)))
      val result: Future[Result] = controller.publishFileTransfer()(fakeFileTransferPublishRequest.withHeaders(HeaderNames.CONTENT_TYPE -> "application/json"))

      status(result) shouldBe Status.OK

      val publishResult = contentAsJson(result).as[PublishResult]
      publishResult.isSuccess shouldBe true

      verify(mockPublishService).publishFileTransfer(eqTo(fileTransferPublishRequestObj))(*)

    }

    "return 400 when platform in request is invalid" in new Setup {

      val result = controller.publishFileTransfer()(fakeFileTransferPublishRequest
        .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
        .withBody(Json.parse(fileTransferPublishRequestInvalidPlatform)))

      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
