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

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MultipartFormData, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubBodyParserFactory}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes.API_UPSERT_ERROR
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.service.PublishService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MultipartPublishControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with StubBodyParserFactory {

  implicit lazy val mat: Materializer = app.materializer

  private val env                = Environment.simple()
  private val configuration      = Configuration.load(env)
  private val appConfig          = new AppConfig(configuration)
  private val encodedAuthHeader  = "dGVzdC1hdXRoLWtleQ==" // authorizationKey = test-auth-key
  private val publisherReference = "123456"

  val validHeaders = Seq(
    HeaderKeys.platformKey          -> PlatformType.CORE_IF.toString,
    HeaderKeys.specificationTypeKey -> "OAS_V3",
    HeaderKeys.publisherRefKey      -> publisherReference,
    HeaderNames.AUTHORIZATION       -> encodedAuthHeader
  )

  implicit class MyWrappedResult(result: Future[Result]) extends Matchers {

    def shouldBeResult(expectedStatus: Int): Unit = {
      status(result) should be(expectedStatus)
    }
  }

  trait Setup {
    val mockPublishService: PublishService = mock[PublishService]
    val validateApiPublishRequestAction    = new ValidateApiPublishRequestAction(new PublishHeaderValidator())
    val validateAuthorizationHeaderAction  = new ValidateAuthorizationHeaderAction(appConfig)

    val controller                         = new MultipartPublishController(
      stubMessagesControllerComponents(),
      mockPublishService,
      validateApiPublishRequestAction,
      validateAuthorizationHeaderAction,
      stubPlayBodyParsers(mat)
    )

    def callPublishWithFile(expectedServiceResponse: Option[PublishResult], headers: Seq[(String, String)], filePartKey: String, fileName: String): Future[Result] = {
      expectedServiceResponse.map(response => when(mockPublishService.publishApi(any())(any())).thenReturn(Future.successful(response)))
      val tempFile = SingletonTemporaryFileCreator.create("text", "txt")
      tempFile.deleteOnExit()

      val dataWithFile = new MultipartFormData[TemporaryFile](Map(), List(FilePart(filePartKey, fileName, Some("text/plain"), tempFile)), List())
      callPublishCommon(dataWithFile, headers)
    }

    def callPublishWithDataPart(expectedServiceResponse: Option[PublishResult], headers: Seq[(String, String)], dataPart: Seq[String]): Future[Result] = {
      if (dataPart.nonEmpty) {
        expectedServiceResponse.map(response => when(mockPublishService.publishApi(any())(any())).thenReturn(Future.successful(response)))
      }

      val dataWithDataParts = new MultipartFormData[TemporaryFile](Map("selectedFile" -> dataPart), List.empty, List())
      callPublishCommon(dataWithDataParts, headers)
    }

    private def callPublishCommon(publishBody: MultipartFormData[TemporaryFile], headers: Seq[(String, String)]) = {
      val publishRequest = FakeRequest.apply("PUT", "integration-catalogue/apis/multipart/publish")
        .withHeaders(headers: _*)
        .withBody(publishBody)

      controller.publishApi()(publishRequest)
    }

    def callPublishWithFileReturnError(expectedServiceResponse: PublishResult, headers: Seq[(String, String)], filePartKey: String, fileName: String): Future[Result] = {
      when(mockPublishService.publishApi(any())(any())).thenReturn(Future.successful(expectedServiceResponse))

      val tempFile = SingletonTemporaryFileCreator.create("text", "txt")
      tempFile.deleteOnExit()

      val data           = new MultipartFormData[TemporaryFile](Map(), List(FilePart(filePartKey, fileName, Some("text/plain"), tempFile)), List())
      val publishRequest = FakeRequest.apply("PUT", "integration-catalogue/apis/multipart/publish")
        .withHeaders(headers: _*)
        .withBody(data)

      controller.publishApi()(publishRequest)

    }

  }

  "POST /apis/multipart/publish" should {

    "return 201 when valid File payload is sent" in new Setup {

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithFile(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = false, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        "selectedFile",
        "text.txt"
      )

      result shouldBeResult CREATED
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 201 when valid Data part payload is sent" in new Setup {

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithDataPart(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = false, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        Seq("Data bytes")
      )

      result shouldBeResult CREATED
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 200 when valid File payload is sent" in new Setup {

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithFile(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        "selectedFile",
        "text.txt"
      )

      result shouldBeResult OK
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 200 when valid Data part payload is sent" in new Setup {

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithDataPart(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        Seq("Data bytes")
      )

      result shouldBeResult OK
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 400 when empty Data part payload is sent" in new Setup {

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithDataPart(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        Seq.empty
      )

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"selectedFile is missing from requestBody"}]}"""
    }

    "return 400 when service response has no details or error" in new Setup {

      val result: Future[Result] = callPublishWithFile(Some(PublishResult(isSuccess = true, None, List.empty)), validHeaders, "selectedFile", "text.txt")

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"Failed to publish API: Unexpected error"}]}"""
    }

    "return 400 when service returns a response with errors" in new Setup {
      val expectedResponse       = PublishResult(isSuccess = false, errors = List(PublishError(API_UPSERT_ERROR, "Unable to upsert api")))
      val result: Future[Result] = callPublishWithFileReturnError(expectedResponse, validHeaders, "selectedFile", "text.txt")

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"Unable to upsert api"}]}"""
    }

    "return 400 when valid payload is sent but publish fails" in new Setup {
      val result: Future[Result] =
        callPublishWithFile(
          Some(
            PublishResult(isSuccess = false, None, List(PublishError(123, "some message")))
          ),
          validHeaders,
          "selectedFile",
          "text.txt"
        )

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"some message"}]}"""
    }

    "return 400 and not publish when invalid file" in new Setup {

      val result: Future[Result] = callPublishWithFile(None, validHeaders, "CANT FIND ME", "text3.txt")

      contentAsString(result) shouldBe """{"errors":[{"message":"selectedFile is missing from requestBody"}]}"""
      result shouldBeResult BAD_REQUEST

      verifyZeroInteractions(mockPublishService)
    }

    "return 400 when plaform not set in header" in new Setup {
      val result: Future[Result] = callPublishWithFile(None, validHeaders.filterNot(_._1.equals(HeaderKeys.platformKey)), "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("platform type header is missing or invalid"))))
      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 400 when platform is invalid in header" in new Setup {
      val headers                = Seq(
        HeaderKeys.platformKey          -> "SOME_RUBBISH",
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey      -> "123456",
        HeaderNames.AUTHORIZATION       -> encodedAuthHeader
      )
      val result: Future[Result] = callPublishWithFile(None, headers, "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("platform type header is missing or invalid"))))
      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 400 when specType not set in header" in new Setup {
      val result: Future[Result] = callPublishWithFile(None, validHeaders.filterNot(_._1.equals(HeaderKeys.specificationTypeKey)), "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("specification type header is missing or invalid"))))
      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 400 when specType is invalid in header" in new Setup {
      val headers                = Seq(
        HeaderKeys.platformKey          -> "CORE_IF",
        HeaderKeys.specificationTypeKey -> "SOME_RUBBISH",
        HeaderKeys.publisherRefKey      -> "123456",
        HeaderNames.AUTHORIZATION       -> encodedAuthHeader
      )
      val result: Future[Result] = callPublishWithFile(None, headers, "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("specification type header is missing or invalid"))))

      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 200 when publisherRef not set in header" in new Setup {

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithFile(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders.filterNot(_._1.equals(HeaderKeys.publisherRefKey)),
        "selectedFile",
        "text.txt"
      )

      status(result) shouldBe OK

      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 200 when publisherRef is invalid in header" in new Setup {
      val invalidHeaders = Seq(
        HeaderKeys.platformKey          -> "CORE_IF",
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey      -> "",
        HeaderNames.AUTHORIZATION       -> encodedAuthHeader
      )

      val id: UUID               = UUID.randomUUID()
      val result: Future[Result] = callPublishWithFile(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        invalidHeaders,
        "selectedFile",
        "text.txt"
      )

      status(result) shouldBe OK
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 401 when Authorization not set in header" in new Setup {
      val result: Future[Result] = callPublishWithFile(None, validHeaders.filterNot(_._1.equals(HeaderNames.AUTHORIZATION)), "selectedFile", "text.txt")

      status(result) shouldBe UNAUTHORIZED

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("Authorisation failed"))))

      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 401 when Authorization is invalid in header" in new Setup {
      val invalidHeaders = Seq(
        HeaderKeys.platformKey          -> PlatformType.CORE_IF.toString,
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey      -> publisherReference,
        HeaderNames.AUTHORIZATION       -> "SOME_RUBBISH"
      )

      val result: Future[Result] = callPublishWithFile(None, invalidHeaders, "selectedFile", "text.txt")

      status(result) shouldBe UNAUTHORIZED

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("Authorisation failed"))))

      contentAsJson(result) shouldBe jsErrorResponse
    }
  }
}
