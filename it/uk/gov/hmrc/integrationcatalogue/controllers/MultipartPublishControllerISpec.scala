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

import org.apache.commons.io.IOUtils
import play.api.http.{HeaderNames, Writeable}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers.{BAD_REQUEST, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.support.{MultipartFormDataWriteable, ServerBaseISpec}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.io.{FileOutputStream, InputStream}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MultipartPublishControllerISpec extends ServerBaseISpec with DefaultPlayMongoRepositorySupport[IntegrationDetail] {

  val appConfig = app.injector.instanceOf[AppConfig]

  override protected lazy val repository: IntegrationRepository = {
    new IntegrationRepository(appConfig, mongoComponent)
  }

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(bind[IntegrationRepository].toInstance(repository))
      .configure(
        "metrics.enabled"  -> false,
        "auditing.enabled" -> false
      )

  val url = s"http://localhost:$port/integration-catalogue"

  private val encodedMasterAuthKey      = "dGVzdC1hdXRoLWtleQ=="
  private val encodedCoreIfAuthKey      = "c29tZUtleTM="
  private val encodedApiPlatformAuthKey = "c29tZUtleTI="

  trait Setup {

    val basePublishHeaders = List(
      HeaderKeys.publisherRefKey      -> "1234",
      HeaderKeys.specificationTypeKey -> "OAS_V3"
    )

    val coreIfAuthHeader              = List(HeaderNames.AUTHORIZATION -> encodedCoreIfAuthKey)
    val coreIfPlatformTypeHeader      = List(HeaderKeys.platformKey -> "CORE_IF")
    val apiPlatformPlatformTypeHeader = List(HeaderKeys.platformKey -> "API_PLATFORM")
    val apiPlatformAuthHeader         = List(HeaderNames.AUTHORIZATION -> encodedApiPlatformAuthKey)
    val masterKeyHeader               = List(HeaderNames.AUTHORIZATION -> encodedMasterAuthKey)

    val headersWithMasterAuthKey: Headers = Headers(
      HeaderKeys.platformKey          -> "CORE_IF",
      HeaderKeys.publisherRefKey      -> "1234",
      HeaderKeys.specificationTypeKey -> "OAS_V3",
      HeaderNames.AUTHORIZATION       -> encodedMasterAuthKey
    )

    implicit val writer: Writeable[MultipartFormData[TemporaryFile]] = MultipartFormDataWriteable.writeable

    val filePart =
      new MultipartFormData.FilePart[TemporaryFile](
        key = "selectedFile",
        filename = "text-to-upload.txt",
        None,
        ref = createTempFileFromResource("/multipart/text-to-upload.txt")
      )

    val multipartBody: MultipartFormData[TemporaryFile] = MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(filePart), badParts = Nil)

    val validApiPublishRequest: FakeRequest[MultipartFormData[TemporaryFile]] =
      FakeRequest(Helpers.PUT, "/integration-catalogue/apis/multipart/publish", Headers(basePublishHeaders: _*), multipartBody)

    val invalidFilePart =
      new MultipartFormData.FilePart[TemporaryFile](
        key = "selectedFile",
        filename = "empty.txt",
        None,
        ref = createTempFileFromResource("/multipart/empty.txt")
      )

    val invalidMultipartBody: MultipartFormData[TemporaryFile] =
      MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(invalidFilePart), badParts = Nil)

    val invalidPublishRequest: FakeRequest[MultipartFormData[TemporaryFile]] =
      FakeRequest(Helpers.PUT, "/integration-catalogue/apis/multipart/publish", headersWithMasterAuthKey, invalidMultipartBody)

    def createBackendPublishResponse(isSuccess: Boolean, isUpdate: Boolean): PublishResult = {
      val publishDetails = if (isSuccess) Some(PublishDetails(isUpdate, IntegrationId(UUID.randomUUID()), "", PlatformType.CORE_IF)) else None
      val publishErrors  = if (isSuccess) List.empty else List(PublishError(10000, "Some Error Message"))
      PublishResult(isSuccess, publishDetails, publishErrors)
    }

    def createTempFileFromResource(path: String): TemporaryFile = {
      val testResource: InputStream = getClass.getResourceAsStream(path)
      val tempFile                  = SingletonTemporaryFileCreator.create("file", "tmp")
      IOUtils.copy(testResource, new FileOutputStream(tempFile))
      tempFile
    }
  }

  "PublishController" when {

    "PUT /services/api/publish" should {

      "respond with 201 when using master auth key and valid request then do a create" in new Setup {
        val response        = route(app, validApiPublishRequest.withHeaders(masterKeyHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(response) mustBe CREATED
        val publishResponse = Json.parse(contentAsString(response)).as[MultipartPublishResponse]
        publishResponse.platformType mustBe PlatformType.CORE_IF
        publishResponse.publisherReference mustBe "1234"
      }

      "respond with 201 when using CORE_IF platform auth key and valid request then do a create" in new Setup {
        val response        = route(app, validApiPublishRequest.withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(response) mustBe CREATED
        val publishResponse = Json.parse(contentAsString(response)).as[MultipartPublishResponse]
        publishResponse.platformType mustBe PlatformType.CORE_IF
        publishResponse.publisherReference mustBe "1234"
      }

      "respond with 400 when platform auth key is provided but platform type header is missing" in new Setup {
        val response: Future[Result] = route(app, validApiPublishRequest.withHeaders(coreIfAuthHeader: _*)).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"platform type header is missing or invalid"}]}"""
      }

      "respond with 400 when platform auth key is provided but platform type header is invalid" in new Setup {
        val response: Future[Result] =
          route(app, validApiPublishRequest.withHeaders(coreIfAuthHeader ++ List(HeaderKeys.platformKey -> "SOMEINVALIDPLATFORM"): _*)).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"platform type header is missing or invalid"}]}"""
      }

      "respond with 200 when valid request and an update" in new Setup {
        val createResponse: Future[Result] = route(app, validApiPublishRequest.withHeaders(masterKeyHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(createResponse) mustBe CREATED

        val okResponse: Future[Result] = route(app, validApiPublishRequest.withHeaders(masterKeyHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(okResponse) mustBe OK
      }

      "respond with 400 from BodyParser when invalid body is sent" in new Setup {
        val response: Future[Result] = route(app, invalidPublishRequest).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"selectedFile is missing from requestBody"}]}"""
      }

      "respond with 400 when invalid platform header" in new Setup {
        val invalidHeaders: Headers                                = Headers(
          HeaderKeys.platformKey          -> "SOME_RUBBISH",
          HeaderKeys.specificationTypeKey -> "OAS_V3",
          HeaderKeys.publisherRefKey      -> "123456",
          HeaderNames.AUTHORIZATION       -> encodedMasterAuthKey
        )
        val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"platform type header is missing or invalid"}]}"""

      }

      "respond with 400 when invalid specification type header" in new Setup {
        val invalidHeaders: Headers                                = Headers(
          HeaderKeys.platformKey          -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "SOME_RUBBISH",
          HeaderKeys.publisherRefKey      -> "123456",
          HeaderNames.AUTHORIZATION       -> encodedMasterAuthKey
        )
        val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"specification type header is missing or invalid"}]}"""
      }

      "respond with 201 when invalid publisher ref header" in new Setup {
        val invalidHeaders: Headers                                = Headers(
          HeaderKeys.platformKey          -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "OAS_V3",
          HeaderKeys.publisherRefKey      -> "",
          HeaderNames.AUTHORIZATION       -> encodedMasterAuthKey
        )
        val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe CREATED
      }

      "respond with 401 when invalid Authorization header" in new Setup {
        val invalidHeaders: Headers                                = Headers(
          HeaderKeys.platformKey          -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "OAS_V3",
          HeaderKeys.publisherRefKey      -> "123456",
          HeaderNames.AUTHORIZATION       -> "SOME_RUBBISH"
        )
        val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe UNAUTHORIZED
        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed"}]}"""
      }
    }
  }
}
