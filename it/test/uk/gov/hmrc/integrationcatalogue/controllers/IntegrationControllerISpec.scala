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

import org.scalatest.BeforeAndAfterEach
import play.api.http.ContentTypes.JSON
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.test.Helpers.{ACCEPT, BAD_REQUEST, NOT_FOUND, OK}
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.IdentifierAction
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters.*
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType.{API, FILE_TRANSFER}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{API_PLATFORM, CDS_CLASSIC, CORE_IF, DES}
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, ApiDetailSummary, ApiTeam, FileTransferTransportsForPlatform, IntegrationDetail, IntegrationPlatformReport, IntegrationResponse}
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.support.{AwaitTestSupport, MongoApp, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogue.testdata.{FakeIdentifierAction, OasParsedItTestData}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.{DefaultPlayMongoRepositorySupport, MongoSupport}

import java.util.UUID
import scala.concurrent.duration.{Duration, SECONDS}

class IntegrationControllerISpec
  extends ServerBaseISpec
    with BeforeAndAfterEach
    with MongoApp
    with OasParsedItTestData
    with AwaitTestSupport
    with DefaultPlayMongoRepositorySupport[IntegrationDetail]
    with MongoSupport {

  override val repository: PlayMongoRepository[IntegrationDetail] = app.injector.instanceOf[IntegrationRepository]
  val apiRepo: IntegrationRepository = repository.asInstanceOf[IntegrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropMongoDb()
    prepareDatabase()
    await(apiRepo.ensureIndexes(), Duration.apply(10, SECONDS))
  }

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled"                 -> true,
        "auditing.enabled"                -> false,
        "mongodb.uri"                     -> s"$mongoUri",
        "auditing.consumer.baseUri.host"  -> wireMockHost,
        "auditing.consumer.baseUri.port"  -> wireMockPort
      )
      .overrides(
        bind[IdentifierAction].to(classOf[FakeIdentifierAction])
      )

  val url = s"http://localhost:$port/integration-catalogue"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def callGetEndpoint(url: String): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(FakeIdentifierAction.fakeAuthorizationHeader, ACCEPT -> JSON)
      .withFollowRedirects(false)
      .get()
      .futureValue

  def callDeleteEndpoint(url: String): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(FakeIdentifierAction.fakeAuthorizationHeader)
      .withFollowRedirects(false)
      .delete()
      .futureValue

  "IntegrationController" when {

    "GET /integrations" should {

      def setupFilterTestDataAndRunTest(searchTerm: String, expectedResult: Int, expectedReferences: List[String], expectedCount: Option[Int] = None): Unit = {
        await(apiRepo.findAndModify(apiDetail1))
        await(apiRepo.findAndModify(apiDetail5))
        await(apiRepo.findAndModify(apiDetail4))
        await(apiRepo.findAndModify(fileTransfer2))

        val result = callGetEndpoint(s"$url/integrations?$searchTerm")
        result.status mustBe expectedResult

        result.status match {
          case OK        =>
            val response = Json.parse(result.body).as[IntegrationResponse]
            if (searchTerm.contains("itemsPerPage")) {
              response.pagedCount.map(x => x mustBe expectedReferences.size)
              expectedCount.map(x => x mustBe response.count)
            } else {
              response.count mustBe expectedReferences.size
            }
            response.results.map(_.publisherReference) mustBe expectedReferences
          case NOT_FOUND =>
          case _         => fail() // at present we are only returning 200 or 404 for searching
        }

      }

      "respond with 200 and return 2 Apis for the first page when using no search term or filter" in {
        setupFilterTestDataAndRunTest("itemsPerPage=2&currentPage=1", OK, List(fileTransfer2.publisherReference, apiDetail1.publisherReference), Some(4))
      }

      "respond with 200 and return 2 Apis for the second page when using no search term or filter" in {
        setupFilterTestDataAndRunTest("itemsPerPage=2&currentPage=2", OK, List(apiDetail4.publisherReference, apiDetail5.publisherReference), Some(4))
      }

      "respond with 200 and return 0 Apis for the third page when using no search term or filter" in {
        setupFilterTestDataAndRunTest("itemsPerPage=2&currentPage=3", OK, List.empty, Some(4))
      }

      "respond with 200 and return 2 Apis for the first page when no currentPage is passed in with no search term or filter" in {
        setupFilterTestDataAndRunTest("itemsPerPage=2", OK, List(fileTransfer2.publisherReference, apiDetail1.publisherReference), Some(4))
      }

      "respond with 200 and return all 4 Apis and ignore paging when passing in currentPage=1 with no search term or filter" in {
        setupFilterTestDataAndRunTest(
          "currentPage=1",
          OK,
          List(
            fileTransfer2.publisherReference,
            apiDetail1.publisherReference,
            apiDetail4.publisherReference,
            apiDetail5.publisherReference
          ),
          Some(4)
        )
      }

      "respond with 200 and return matching Api when searching by searchTerm" in {
        setupFilterTestDataAndRunTest("searchTerm=API1001", OK, List(apiDetail1.publisherReference))
      }

      "respond with 200 and return no matching integrations when searching by searchTerm" in {

        setupFilterTestDataAndRunTest("searchTerm=cantfindme", OK, List.empty)

      }

      "respond with 200 and return DES api when searching by searchTerm=getKnownFactsName, platformFilter=DES and backendsFilter=CUSTOMS" in {
        setupFilterTestDataAndRunTest("searchTerm=getKnownFactsName&platformFilter=DES&backendsFilter=CUSTOMS", OK, List(apiDetail4.publisherReference))
      }

      "respond with 200 and return 2 CORE_IF apis and 1 file transfer when filtering by CORE_IF platform" in {
        setupFilterTestDataAndRunTest(
          "platformFilter=CORE_IF",
          OK,
          List(fileTransfer2.publisherReference, apiDetail1.publisherReference, apiDetail5.publisherReference)
        )
      }

      "respond with 200 and return 1 Api for the first page when no currentPage is passed in with teamIds filter" in {
        setupFilterTestDataAndRunTest("teamIds=team1&teamIds=team2", OK, List(apiDetail1.publisherReference), Some(1))
      }

      "respond with 400 when invalid platform param sent" in {
        val result = callGetEndpoint(s"$url/integrations?platformFilter=UNKNOWN")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Cannot accept UNKNOWN as PlatformType\"}]}"
      }

      "respond with 400 when invalid param key sent" in {
        val result = callGetEndpoint(s"$url/integrations?unknownFilter=api_platform")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Invalid query parameter key provided. It is case sensitive\"}]}"
      }

      "respond with 200 and return all integrations when no searchTerm" in {
        setupFilterTestDataAndRunTest(
          "",
          OK,
          List(
            fileTransfer2.publisherReference,
            apiDetail1.publisherReference,
            apiDetail4.publisherReference,
            apiDetail5.publisherReference
          )
        )
      }

      "respond with 200 and return 1 api when filtering by CUSTOMS hod" in {
        setupFilterTestDataAndRunTest(
          "backendsFilter=CUSTOMS",
          OK,
          List(apiDetail4.publisherReference)
        )
      }

      "respond with 200 and return 1 api and 1 file transfer when filtering by 'CUSTOMS' hod and 'source' sourceSystem" in {
        setupFilterTestDataAndRunTest(
          "backendsFilter=CUSTOMS&backendsFilter=source",
          OK,
          List(fileTransfer2.publisherReference, apiDetail4.publisherReference)
        )
      }

      "respond with 200 and return all Apis when filtering by API type" in {
        setupFilterTestDataAndRunTest(
          "integrationType=API",
          OK,
          List(
            apiDetail1.publisherReference,
            apiDetail4.publisherReference,
            apiDetail5.publisherReference
          )
        )
      }

      "respond with 200 and return all File Transfers when filtering by FILE_TRANSFER type" in {
        setupFilterTestDataAndRunTest("integrationType=FILE_TRANSFER", OK, List(fileTransfer2.publisherReference))
      }

    }

    "GET /integrations/summaries" should {
      def setupFilterTestDataAndRunTest(searchTerm: List[String], platformFilter: List[PlatformType], expectedReferences: List[String]): Unit = {
        await(apiRepo.findAndModify(apiDetail1))
        await(apiRepo.findAndModify(apiDetail5))
        await(apiRepo.findAndModify(apiDetail4))

        val query = Seq(
          searchTerm.map(searchTerm => s"searchTerm=$searchTerm"),
          platformFilter.map(platformFilter => s"platformFilter=$platformFilter")
        ).flatten.mkString("&")

        val result = callGetEndpoint(s"$url/integrations/summaries?$query")
        result.status mustBe OK

        val summaries = Json.parse(result.body).as[Seq[ApiDetailSummary]]

        summaries.map(_.publisherReference) must contain theSameElementsAs expectedReferences
      }

      "respond with all API summaries when no filters specified" in {
        setupFilterTestDataAndRunTest(
          List.empty,
          List.empty,
          List(
            apiDetail1.publisherReference,
            apiDetail4.publisherReference,
            apiDetail5.publisherReference
          )
        )
      }

      "respond with matching summary when searching by searchTerm" in {
        setupFilterTestDataAndRunTest(
          List("API1001"),
          List.empty,
          List(apiDetail1.publisherReference)
        )
      }

      "respond with no matching summaries when searching by searchTerm" in {
        setupFilterTestDataAndRunTest(
          List("cantfindme"),
          List.empty,
          List.empty
        )
      }

      "respond with DES api when searching by searchTerm getKnownFactsName and platformFilter DES" in {
        setupFilterTestDataAndRunTest(
          List("getKnownFactsName"),
          List(PlatformType.DES),
          List(apiDetail4.publisherReference)
        )
      }

      "respond with 200 and return 2 summaries when filtering by CORE_IF platform" in {
        setupFilterTestDataAndRunTest(
          List.empty,
          List(PlatformType.CORE_IF),
          List(apiDetail1.publisherReference, apiDetail5.publisherReference)
        )
      }

      "respond with 400 when invalid platform param sent" in {
        val result = callGetEndpoint(s"$url/integrations/summaries?platformFilter=UNKNOWN")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Cannot accept UNKNOWN as PlatformType\"}]}"
      }

      "respond with 400 when invalid param key sent" in {
        val result = callGetEndpoint(s"$url/integrations/summaries?unknownFilter=api_platform")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Invalid query parameter key provided. It is case sensitive\"}]}"
      }
    }

    "GET /integrations/:id" should {
      "respond with integration when search for existing CDS_CLASSIC integration " in {

        await(apiRepo.findAndModify(apiDetail5))
        val result1 = await(apiRepo.findAndModify(apiDetail6))

        result1 match {
          case Left(_)                                    => fail()
          case Right((integration: IntegrationDetail, _)) =>
            val result = callGetEndpoint(s"$url/integrations/${integration.id.value.toString}")
            result.status mustBe OK

            val response = Json.parse(result.body).as[IntegrationDetail]
            response.publisherReference mustBe apiDetail6.publisherReference
            response.platform mustBe apiDetail6.platform
        }

      }

      "respond with 400 when invalid UUID param sent" in {
        val result = callGetEndpoint(s"$url/integrations/2233-3322-2222")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Cannot accept 2233-3322-2222 as IntegrationsId\"}]}"
      }

      "respond with 404 when integration not found" in {

        await(apiRepo.findAndModify(apiDetail1))
        await(apiRepo.findAndModify(apiDetail5))

        val result = callGetEndpoint(s"$url/integrations/${UUID.randomUUID}")
        result.status mustBe NOT_FOUND

      }

      "respond with integration having scopes" in {
        await(apiRepo.findAndModify(apiDetail9)) match {
          case Right((integration, _)) =>
            val result = callGetEndpoint(s"$url/integrations/${integration.id.value.toString}")
            result.status mustBe OK

            val apiDetail = Json.parse(result.body).as[ApiDetail]
            apiDetail.endpoints.foreach(
              endpoint =>
                endpoint.methods.foreach(
                  method =>
                    method.scopes must not be empty
                )
            )
          case Left(error) =>
            throw error
        }
      }
    }

    "GET /integrations/publisher-reference/:publisherReference" should {
      "respond with integration when the publisher reference exists" in {
        await(apiRepo.findAndModify(apiDetail9)) match {
          case Left(_)                                    => fail()
          case Right((integration: IntegrationDetail, _)) =>
            val result = callGetEndpoint(s"$url/integrations/publisher-reference/${integration.publisherReference}")
            result.status mustBe OK

            val response = Json.parse(result.body).as[IntegrationDetail]
            response.publisherReference mustBe integration.publisherReference
            response.platform mustBe integration.platform
        }
      }

      "respond with 404 when integration not found" in {
        await(apiRepo.findAndModify(apiDetail1)) match {
          case Left(_) => fail()
          case Right((integration: IntegrationDetail, _)) =>
            val result = callGetEndpoint(s"$url/integrations/publisher-reference/${integration.publisherReference}")
            result.status mustBe NOT_FOUND
        }
      }
    }

    "GET  /report " should {

      "respond with correct report when integrations exist" in {
        await(apiRepo.findAndModify(apiDetail1))    // CORE_IF - API
        await(apiRepo.findAndModify(apiDetail3))    // CORE_IF - API
        await(apiRepo.findAndModify(apiDetail4))    // DES - API
        await(apiRepo.findAndModify(apiDetail6))    // CDS_CLASSIC - API
        await(apiRepo.findAndModify(fileTransfer2)) // CORE_IF - FILE_TRANSFER
        await(apiRepo.findAndModify(fileTransfer7)) // API_PLATFORM - FILE_TRANSFER

        val expectedResults = List(
          IntegrationPlatformReport(API_PLATFORM, FILE_TRANSFER, 1),
          IntegrationPlatformReport(CDS_CLASSIC, API, 1),
          IntegrationPlatformReport(CORE_IF, API, 2),
          IntegrationPlatformReport(CORE_IF, FILE_TRANSFER, 1),
          IntegrationPlatformReport(DES, API, 1)
        )

        val result = callGetEndpoint(s"$url/report")
        result.body mustBe Json.toJson(expectedResults).toString()
      }
    }

    "GET /filetransfers/platform/transports" should {
      def setupFileTransfers(): Unit = {

        await(apiRepo.findAndModify(fileTransfer2)) // CORE_IF | source -> target | transports = UTM
        await(apiRepo.findAndModify(fileTransfer7)) // API_PLATFORM | someSource -> target | transports = S3
        await(apiRepo.findAndModify(fileTransfer8)) // API_PLATFORM | someSource -> target | transports = S3, WTM
      }
      "return all transports when called without query params" in {
        setupFileTransfers()
        val expectedResults = List(
          FileTransferTransportsForPlatform(API_PLATFORM, List("AB", "S3", "WTM")),
          FileTransferTransportsForPlatform(CORE_IF, List("UTM"))
        )

        val result = callGetEndpoint(s"$url/filetransfers/platform/transports")
        result.body mustBe Json.toJson(expectedResults).toString

      }

      "return CORE_IF transports when source=source and target=target" in {
        setupFileTransfers()
        val expectedResults = List(
          FileTransferTransportsForPlatform(CORE_IF, List("UTM"))
        )

        val result = callGetEndpoint(s"$url/filetransfers/platform/transports?source=source&target=target")
        result.body mustBe Json.toJson(expectedResults).toString

      }

      "return 400 when only source query param is provided" in {
        val result = callGetEndpoint(s"$url/filetransfers/platform/transports?source=someSource")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"You must either provide both source and target or no query parameters\"}]}"

      }

      "return 400 when only target query param is provided" in {
        val result = callGetEndpoint(s"$url/filetransfers/platform/transports?target=target")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"You must either provide both source and target or no query parameters\"}]}"

      }

      "return 400 when invalid query param key is provided" in {
        val result = callGetEndpoint(s"$url/filetransfers/platform/transports?invalidkey=invalidvalue")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Invalid query parameter key provided. It is case sensitive\"}]}"

      }
    }

    "DELETE /integrations/:integrationId" should {
      "not remove integration when integrationId does not exist" in {

        val result = callDeleteEndpoint(s"$url/integrations/${apiDetail1.id.value}")
        result.status mustBe NOT_FOUND

      }
    }

    "DELETE /integrations?platform=CORE_IF" should {
      "return 400 when no platform type filter is provided" in {

        val result = callDeleteEndpoint(s"$url/integrations")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"DeleteWithFilters no platformtype passed as filter\"}]}"

      }
      "return 400 when platform type filter is provided empty" in {

        val result = callDeleteEndpoint(s"$url/integrations?platformFilter=")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Cannot accept  as PlatformType\"}]}"

      }
      "return 400 when platform type filter is invalid" in {

        val result = callDeleteEndpoint(s"$url/integrations?platformFilter=UNKNOWN")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"Cannot accept UNKNOWN as PlatformType\"}]}"
      }
      "return 400 when multiple platform type filters are provided" in {

        val result = callDeleteEndpoint(s"$url/integrations?platformFilter=CORE_IF&platformFilter=DES")
        result.status mustBe BAD_REQUEST
        result.body mustBe "{\"errors\":[{\"message\":\"only one platform can be deleted at a time\"}]}"

      }
      "return 204 when single platform type filters is provided" in {
        await(apiRepo.findAndModify(apiDetail1))
        val result = callDeleteEndpoint(s"$url/integrations?platformFilter=CORE_IF")
        result.status mustBe OK
        result.body mustBe "{\"numberOfIntegrationsDeleted\":1}"
      }
    }
  }

}
