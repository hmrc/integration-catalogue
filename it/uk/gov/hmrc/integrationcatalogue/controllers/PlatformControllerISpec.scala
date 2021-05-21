package uk.gov.hmrc.integrationcatalogue.controllers

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.integrationcatalogue.support.ServerBaseISpec
import uk.gov.hmrc.integrationcatalogue.support.AwaitTestSupport
import play.api.test.Helpers._

class PlatformControllerISpec extends ServerBaseISpec with AwaitTestSupport {
  
  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )

  val url = s"http://localhost:$port/integration-catalogue"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def callGetEndpoint(url: String): WSResponse =
    wsClient
      .url(url)
      .withFollowRedirects(false)
      .get()
      .futureValue


  "PlatformController" when {

    "GET /platform/contacts" should {
      "return platform with contacts when both email and name are in config" in {
           val result =  callGetEndpoint(s"$url/platform/contacts")

            result.status mustBe OK
            result.body mustBe """[{"platformType":"DES","contactInfo":{"name":"DES support hot line","emailAddress":"des@mail.com"}},{"platformType":"CORE_IF","contactInfo":{"name":"CoreIf support hot line","emailAddress":"coreIf@mail.com"}},{"platformType":"API_PLATFORM","contactInfo":{"name":"Api Platform support hot line","emailAddress":"api_platform@mail.com"}},{"platformType":"CMA","contactInfo":{"name":"CMA support hot line","emailAddress":"cma@mail.com"}},{"platformType":"CDS_CLASSIC","contactInfo":{"name":"CDS Platform support hot line","emailAddress":"cds@mail.com"}}]"""
      }

    }

  }
}
