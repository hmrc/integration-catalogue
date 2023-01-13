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

import scala.concurrent.ExecutionContext.Implicits.global

import org.mockito.scalatest.MockitoSugar
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

class PlatformControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit def mat: akka.stream.Materializer = app.injector.instanceOf[akka.stream.Materializer]
  private val mockAppConfig                  = mock[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
  }

  trait Setup {
    val apiPlatformContact   = PlatformContactResponse(PlatformType.API_PLATFORM, Some(ContactInformation(Some("ApiPlatform"), Some("api.platform@email"))), true)
    val coreIfWithoutContact = PlatformContactResponse(PlatformType.CORE_IF, None, false)
    val controller           = new PlatformController(Helpers.stubControllerComponents(), mockAppConfig)
    val fakeRequest          = FakeRequest("GET", s"/platform/contacts")
  }

  "PlatformController" should {
    "return platform with contacts" in new Setup {
      val expectedResponse = List(apiPlatformContact)
      when(mockAppConfig.platformContacts).thenReturn(expectedResponse)
      val result           = controller.getPlatformContacts()(fakeRequest)
      contentAsString(result) shouldBe Json.toJson(expectedResponse).toString
      status(result) shouldBe OK
    }

    "return platform without contacts" in new Setup {
      val expectedResponse = List(coreIfWithoutContact)
      when(mockAppConfig.platformContacts).thenReturn(expectedResponse)
      val result           = controller.getPlatformContacts()(fakeRequest)
      contentAsString(result) shouldBe Json.toJson(expectedResponse).toString
      status(result) shouldBe OK
    }
  }
}
