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
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.integrationcatalogue.support.ServerBaseISpec
import uk.gov.hmrc.integrationcatalogue.support.AwaitTestSupport
import play.api.test.Helpers._
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.IdentifierAction
import uk.gov.hmrc.integrationcatalogue.testdata.FakeIdentifierAction

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
        "platforms.DES.email"               -> "des@mail.com",
        "platforms.DES.name"                -> "DES Platform support hot line",
        "platforms.DES.overrideOasContacts" -> true,
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

        result.status mustBe OK
        result.body mustBe """[{"platformType":"API_PLATFORM","overrideOasContacts":false},{"platformType":"CDS_CLASSIC","overrideOasContacts":false},{"platformType":"CMA","overrideOasContacts":false},{"platformType":"CORE_IF","overrideOasContacts":false},{"platformType":"DAPI","overrideOasContacts":false},{"platformType":"DES","contactInfo":{"name":"DES Platform support hot line","emailAddress":"des@mail.com"},"overrideOasContacts":true},{"platformType":"DIGI","overrideOasContacts":false},{"platformType":"SDES","overrideOasContacts":false},{"platformType":"TRANSACTION_ENGINE","overrideOasContacts":false},{"platformType":"CIP","overrideOasContacts":false},{"platformType":"HIP","overrideOasContacts":false}]"""
      }

    }

  }
}
