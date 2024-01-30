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

import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.PlatformContactResponse
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, PlatformType}
import uk.gov.hmrc.integrationcatalogue.testdata.FakeIdentifierAction

class PlatformControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit def mat: org.apache.pekko.stream.Materializer = app.injector.instanceOf[org.apache.pekko.stream.Materializer]
  private val mockAppConfig                  = mock[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
  }

  private val apiPlatformContact   = PlatformContactResponse(
    PlatformType.API_PLATFORM,
    Some(ContactInformation(Some("ApiPlatform"), Some("api.platform@email"))),
    overrideOasContacts = true
  )

  private val coreIfWithoutContact = PlatformContactResponse(PlatformType.CORE_IF, None, overrideOasContacts = false)

  private val controller           = new PlatformController(
    Helpers.stubControllerComponents(),
    mockAppConfig,
    new FakeIdentifierAction(Helpers.stubPlayBodyParsers(NoMaterializer))
  )

  private val fakeRequest          = FakeRequest("GET", s"/platform/contacts")
  private val fakeAuthenticatedRequest = fakeRequest.withHeaders(FakeIdentifierAction.fakeAuthorizationHeader)

  "PlatformController" should {
    "return platform with contacts" in {
      val expectedResponse = List(apiPlatformContact)
      when(mockAppConfig.platformContacts).thenReturn(expectedResponse)
      val result           = controller.getPlatformContacts()(fakeAuthenticatedRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe Json.toJson(expectedResponse).toString
    }

    "return platform without contacts" in {
      val expectedResponse = List(coreIfWithoutContact)
      when(mockAppConfig.platformContacts).thenReturn(expectedResponse)
      val result           = controller.getPlatformContacts()(fakeAuthenticatedRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe Json.toJson(expectedResponse).toString
    }

    "return unauthorised if the request is not authenticated" in {
      val result = controller.getPlatformContacts()(fakeRequest)
      status(result) shouldBe UNAUTHORIZED
    }
  }

}
