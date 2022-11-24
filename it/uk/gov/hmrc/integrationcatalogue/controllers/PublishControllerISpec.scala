package uk.gov.hmrc.integrationcatalogue.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{BAD_REQUEST, NOT_FOUND, OK}
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, IntegrationId, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.support.{AwaitTestSupport, MongoApp, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogue.testdata.{OasParsedItTestData, OasTestData}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

class PublishControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with MongoApp with OasTestData with OasParsedItTestData with AwaitTestSupport {

  override protected def repository: PlayMongoRepository[IntegrationDetail] = app.injector.instanceOf[IntegrationRepository]

  val apiRepo = repository.asInstanceOf[IntegrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropMongoDb()
    await(repository.ensureIndexes)
  }

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled"                 -> true,
        "auditing.enabled"                -> false,
        "mongodb.uri"                     -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "auditing.consumer.baseUri.host"  -> wireMockHost,
        "auditing.consumer.baseUri.port"  -> wireMockPort
      )

  val url = s"http://localhost:$port/integration-catalogue"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val validHeaders = List(CONTENT_TYPE -> "application/json")

  def callPutEndpoint(url: String, body: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withFollowRedirects(false)
      .put(body)
      .futureValue

  def callDeleteEndpoint(url: String): WSResponse =
    wsClient
      .url(url)
      .withFollowRedirects(false)
      .delete
      .futureValue

  trait Setup {

    val publishRequest: PublishRequest = PublishRequest(
      publisherReference = Some(exampleApiDetail.publisherReference),
      platformType = exampleApiDetail.platform,
      specificationType = SpecificationType.OAS_V3,
      contents = fileContents
    )

    def validPublishRequest: PublishRequest = {
      publishRequest.copy(contents = rawOASData(oasContactName))
    }

    val fileTransferPublishRequestObj: FileTransferPublishRequest = FileTransferPublishRequest(
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

    def callApiPublishEndpoint(): WSResponse = {
      callPutEndpoint(s"$url/apis/publish", Json.toJson(validPublishRequest).toString, validHeaders)
    }

    def callFileTransferPublishEndpoint(): WSResponse = {
      callPutEndpoint(s"$url/filetransfers/publish", Json.toJson(fileTransferPublishRequestObj).toString, validHeaders)
    }
  }

  "PublishController" when {

    "PUT /" should {
      "respond with 200 when content field is parseable by the OAS parser" in new Setup {

        val result: WSResponse           = callApiPublishEndpoint()
        result.status mustBe OK
        val publishResult: PublishResult = Json.parse(result.body).as[PublishResult]
        publishResult.isSuccess mustBe true
        publishResult.errors.size mustBe 0

        val apis: Seq[IntegrationDetail] = Await.result(apiRepo.findWithFilters(IntegrationFilter()), 500 millis).results
        apis.size mustBe 1
        apis.head.publisherReference mustBe validPublishRequest.publisherReference.getOrElse("")

      }

      "respond with 200 when content field is not parseable by the OAS parser" in new Setup {

        val result: WSResponse           = callPutEndpoint(s"$url/apis/publish", Json.toJson(publishRequest).toString, validHeaders)
        result.status mustBe OK
        val publishResult: PublishResult = Json.parse(result.body).as[PublishResult]
        publishResult.isSuccess mustBe false
        publishResult.errors.head mustBe PublishError(OAS_PARSE_ERROR, "attribute openapi is missing")
      }

      "respond with 400 from BodyParser when invalid body is sent" in {

        val result = callPutEndpoint(s"$url/apis/publish", "{}", validHeaders)
        result.status mustBe BAD_REQUEST
      }

      "respond with 400 from Play when body is invalid" in {

        val result = callPutEndpoint(s"$url/apis/publish", "", validHeaders)
        result.status mustBe BAD_REQUEST
      }

      "respond with 404 when path invalid" in {

        val result = callPutEndpoint(s"$url/unknown-path", "{}", validHeaders)
        result.status mustBe NOT_FOUND

      }
    }

    "PUT /filetransfer/publish" should {
      "respond with 200 when body is valid" in new Setup {
        val result: WSResponse           = callFileTransferPublishEndpoint()
        result.status mustBe OK
        val publishResult: PublishResult = Json.parse(result.body).as[PublishResult]
        publishResult.isSuccess mustBe true
        publishResult.errors.size mustBe 0

        val apis: immutable.Seq[IntegrationDetail] = Await.result(apiRepo.findWithFilters(IntegrationFilter()), 500 millis).results
        apis.size mustBe 1

        apis.head match {
          case ft: FileTransferDetail =>
            ft.publisherReference mustBe fileTransferPublishRequestObj.publisherReference
            ft.transports mustBe List("S3")
          case _                      => fail()
        }
      }
    }

    "DELETE /integrations/:id" should {
      "find and remove the integration with passed in id" in new Setup {

        val publishResponse = callApiPublishEndpoint()
        val result          = Json.parse(publishResponse.body).as[PublishResult]
        result.isSuccess mustBe true

        val savedId: IntegrationId = Await.result(apiRepo.findWithFilters(IntegrationFilter()), 500 millis).results.map(_.id).head
        Await.result(apiRepo.findById(savedId), 500 millis).size mustBe 1
        callDeleteEndpoint(s"$url/integrations/${savedId.value}")

        Await.result(apiRepo.findById(savedId), 500 millis).size mustBe 0
      }
    }
  }

}
