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

package uk.gov.hmrc.integrationcatalogue.controllers

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import uk.gov.hmrc.integrationcatalogue.support.ServerBaseISpec
import uk.gov.hmrc.integrationcatalogue.support.AwaitTestSupport
import play.api.test.Helpers.*
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.IdentifierAction
import uk.gov.hmrc.integrationcatalogue.models.PlatformContactResponse
import uk.gov.hmrc.integrationcatalogue.models.common.ContactInformation
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.*
import uk.gov.hmrc.integrationcatalogue.testdata.FakeIdentifierAction
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

class PlatformControllerISpec extends ServerBaseISpec with AwaitTestSupport {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"   -> wireMockPort,
        "metrics.enabled"                   -> true,
        "auditing.enabled"                  -> false,
        "mongodb.uri"                       -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "auditing.consumer.baseUri.host"    -> wireMockHost,
        "auditing.consumer.baseUri.port"    -> wireMockPort,
        "platforms.SOMENEW.email"           -> "des@mail.com",
        "platforms.SOMENEW.name"            -> "DES Platform support hot line"
      )
      .overrides(
        bind[IdentifierAction].to(classOf[FakeIdentifierAction])
      )

  val url = s"http://localhost:$port/integration-catalogue"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def callGetEndpoint(url: String): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(FakeIdentifierAction.fakeAuthorizationHeader)
      .withFollowRedirects(false)
      .get()
      .futureValue

  "PlatformController" when {

    "GET /platform/contacts" should {
      "return platform with contacts when both email and name are in config" in {
        val result = callGetEndpoint(s"$url/platform/contacts")

        val platforms = Seq(
          PlatformContactResponse(
            platformType = API_PLATFORM,
            contactInfo = Some(ContactInformation(name = Some("API_Platform"), emailAddress = Some("API_Platform@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = CDS_CLASSIC,
            contactInfo = Some(ContactInformation(name = Some("CDS"), emailAddress = Some("CDS@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = CMA,
            contactInfo = Some(ContactInformation(name = Some("CMA"), emailAddress = Some("CMA@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = CORE_IF,
            contactInfo = Some(ContactInformation(name = Some("Integration_Framework"), emailAddress = Some("Integration_Framework@test.com"))),
            overrideOasContacts = true
          ),
          PlatformContactResponse(
            platformType = DAPI,
            contactInfo = Some(ContactInformation(name = Some("DAPI"), emailAddress = Some("DAPI@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = DES,
            contactInfo = Some(ContactInformation(name = Some("DES"), emailAddress = Some("DES@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = DIGI,
            contactInfo = Some(ContactInformation(name = Some("DIGI"), emailAddress = Some("DIGI@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = SDES,
            contactInfo = Some(ContactInformation(name = Some("SDES"), emailAddress = Some("SDES@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = TRANSACTION_ENGINE,
            contactInfo = Some(ContactInformation(name = Some("TRANSACTION_ENGINE"), emailAddress = Some("TRANSACTION_ENGINE@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = CIP,
            contactInfo = Some(ContactInformation(name = Some("CIP"), emailAddress = Some("CIP@test.com"))),
            overrideOasContacts = false
          ),
          PlatformContactResponse(
            platformType = HIP,
            contactInfo = Some(ContactInformation(name = Some("HIP"), emailAddress = Some("HIP@test.com"))),
            overrideOasContacts = false
          )
        )

        val expected = Json.toJson(platforms).toString

        result.status mustBe OK
        result.body mustBe expected
      }

    }

  }
}
