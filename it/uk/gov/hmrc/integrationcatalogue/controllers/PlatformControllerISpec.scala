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
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "platforms.DES.email" -> "des@mail.com",
        "platforms.DES.name" -> "DES Platform support hot line",
        "platforms.SOMENEW.email" -> "des@mail.com",
        "platforms.SOMENEW.name" -> "DES Platform support hot line"
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
        val result = callGetEndpoint(s"$url/platform/contacts")

        println(s"****** ${result.body}")

        result.status mustBe OK
        result.body mustBe """[{"platformType":"DES","contactInfo":{"name":"DES Platform support hot line","emailAddress":"des@mail.com"}},{"platformType":"CORE_IF"},{"platformType":"API_PLATFORM"},{"platformType":"CMA"},{"platformType":"CDS_CLASSIC"},{"platformType":"DIGI_DAPI"},{"platformType":"SDES"},{"platformType":"TRANSACTION_ENGINE"}]"""
      }

    }

  }
}
