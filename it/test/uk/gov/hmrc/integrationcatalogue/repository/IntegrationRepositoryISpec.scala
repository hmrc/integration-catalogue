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

package uk.gov.hmrc.integrationcatalogue.repository

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType.{API, FILE_TRANSFER}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{API_PLATFORM, CMA, CORE_IF, DES}
import uk.gov.hmrc.integrationcatalogue.support.{AwaitTestSupport, MongoApp}
import uk.gov.hmrc.integrationcatalogue.testdata.OasParsedItTestData
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}

class IntegrationRepositoryISpec
    extends AnyWordSpec
    with Matchers
    with MongoApp
    with GuiceOneAppPerSuite
    with ScalaFutures
    with BeforeAndAfterEach
    with OasParsedItTestData
    with AwaitTestSupport
    with OptionValues {

  override val repository: PlayMongoRepository[IntegrationDetail] = app.injector.instanceOf[IntegrationRepository]
  lazy val indexNameToDrop = "please_delete_me__let_me_go"

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "mongodb.oldIndexesToDrop" -> Seq(indexNameToDrop, "text_index_1_0")
      )
  }

  override implicit lazy val app: Application = appBuilder.build()

  def repo: IntegrationRepository =
    app.injector.instanceOf[IntegrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropMongoDb()
    await(repo.ensureIndexes(), Duration.apply(10, SECONDS))
  }

  def getAll: List[IntegrationDetail] = {
    await(repo.findWithFilters(IntegrationFilter())).results
  }

  def findWithFilters(integrationFilter: IntegrationFilter): IntegrationResponse = {
    await(repo.findWithFilters(integrationFilter))
  }

  def deleteById(id: IntegrationId): Boolean = {
    await(repo.deleteById(id))
  }

  private def validateCommonFields(testItem: IntegrationDetail, itemToValidate: IntegrationDetail): Assertion = {
    //    testItem.id shouldBe itemToValidate.id
    testItem.title shouldBe itemToValidate.title
    testItem.description shouldBe itemToValidate.description
    testItem.integrationType shouldBe itemToValidate.integrationType
    testItem.publisherReference shouldBe itemToValidate.publisherReference
    testItem.platform shouldBe itemToValidate.platform
  }

  def validateApi(testItem: IntegrationDetail, itemToValidate: ApiDetail): Assertion = {
    validateCommonFields(testItem, itemToValidate)
    val item = testItem.asInstanceOf[ApiDetail]
    item.version shouldBe itemToValidate.version
    item.hods shouldBe itemToValidate.hods
    item.endpoints shouldBe itemToValidate.endpoints
    item.openApiSpecification shouldBe itemToValidate.openApiSpecification
  }

  def validateFileTransfer(testItem: IntegrationDetail, itemToValidate: FileTransferDetail): Assertion = {
    validateCommonFields(testItem, itemToValidate)
    val item = testItem.asInstanceOf[FileTransferDetail]
    item.fileTransferPattern shouldBe itemToValidate.fileTransferPattern
    item.sourceSystem shouldBe itemToValidate.sourceSystem
    item.targetSystem shouldBe itemToValidate.targetSystem
  }

  trait FilterSetup {

    def createTestData(): Unit = {
      val combinedFuture =
        for {
          matchInTitle <- repo.findAndModify(apiDetail1)
          matchInDescription <- repo.findAndModify(apiDetail5)
          matchInHods <- repo.findAndModify(apiDetail3.copy(teamId = Some("team_id_1")))
          matchOnDesPlatform <- repo.findAndModify(apiDetail4.copy(teamId = Some("team_id_2")))
          matchOnFileTransferPlatformOne <- repo.findAndModify(fileTransfer2)
          matchOnFileTransferPlatformTwo <- repo.findAndModify(fileTransfer7)
        } yield (
          matchInTitle,
          matchInDescription,
          matchInHods,
          matchOnDesPlatform,
          matchOnFileTransferPlatformOne,
          matchOnFileTransferPlatformTwo
        )
      Await.result(combinedFuture, Duration.Inf)
    }

    def setUpTest(): Unit = {
      val result = findWithFilters(IntegrationFilter(List.empty, List.empty)).results
      result shouldBe List.empty

      createTestData()
      val x = findWithFilters(IntegrationFilter(List.empty, List.empty))
      x.results.size shouldBe 6
    }

    def validateResults(results: Seq[IntegrationDetail], expectedReferences: List[String]): Unit = {
      results.size shouldBe expectedReferences.size
      results.map(_.publisherReference) shouldBe (expectedReferences)
    }

    def validatePagedResults(integrationResponse: IntegrationResponse, expectedReferences: List[String], expectedCount: Int): Unit = {
      validateResults(integrationResponse.results, expectedReferences)

      integrationResponse.pagedCount.map(x => x shouldBe expectedReferences.size)
      integrationResponse.count shouldBe expectedCount
    }
  }

  "IntegrationRepository" when {

    "findByPublisherReference" should {

      "return an api when it has been created" in {
        val result = getAll
        result shouldBe List.empty

        val modifyResult = await(repo.findAndModify(apiDetail1))

        modifyResult match {
          case Right((integration, _)) => {
            val result2 = await(repo.findById(integration.id))
            validateApi(result2.get, apiDetail1)
          }
          case Left(_) => fail()
        }

      }

    }

    "findById" should {

      "return an integration when it has been created" in {
        val result = getAll
        result shouldBe List.empty

        val modifyResult = await(repo.findAndModify(apiDetail1))
        await(repo.findAndModify(apiDetail5))
        modifyResult match {
          case Right((integration, _)) => {

            val result2 = await(repo.findById(integration.id))
            validateApi(result2.get, apiDetail1)
          }
          case Left(_) => fail()
        }

      }

      "return None when integrations exist but unknown id" in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(apiDetail1))
        await(repo.findAndModify(apiDetail5))

        val result2 = await(repo.findById(IntegrationId(UUID.randomUUID())))
        result2 shouldBe None
      }

    }

    "findByPublisherRef" should {
      "return an integration when it exists" in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(apiDetail1))
        await(repo.findAndModify(apiDetail5))

        val result2 = await(repo.findByPublisherRef(apiDetail1.platform, apiDetail1.publisherReference))
        validateApi(result2.get, apiDetail1)
      }

      "return None when the integration does not exist" in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(apiDetail5))

        val result2 = await(repo.findByPublisherRef(apiDetail1.platform, apiDetail1.publisherReference))
        result2 shouldBe None
      }
    }

    "findAndModify" should {

      "save api when no duplicate exists in collection" in {
        val result = getAll
        result shouldBe List.empty

        val insertResult: Either[Throwable, (IntegrationDetail, Types.IsUpdate)] = await(repo.findAndModify(apiDetail1))
        insertResult match {
          case Right((apiDetail: IntegrationDetail, isUpdate)) =>
            isUpdate shouldBe false
            validateApi(apiDetail, apiDetail1)
          case Right(_) => fail()
          case Left(_) => fail()
        }

        getAll.size shouldBe 1

      }

      "created date is not modified after an update" in {
        val apiDetailModify: ApiDetail = await(repo.findAndModify(apiDetail1)).toOption.collect {
          case (apiDetail: ApiDetail, _) => apiDetail
        }.getOrElse(fail())


        val apiDetailModifyAgain: ApiDetail = await(repo.findAndModify(apiDetailModify.copy(title = "new title"))).toOption.collect {
          case (apiDetail: ApiDetail, _) => apiDetail
        }.getOrElse(fail())

        apiDetailModify.created shouldBe apiDetailModifyAgain.created
      }

      "save file transfer details when no duplicate exists in collection" in {
        val result = getAll
        result shouldBe List.empty

        val insertResult: Either[Throwable, (IntegrationDetail, Types.IsUpdate)] = await(repo.findAndModify(fileTransfer2))
        insertResult match {
          case Right((details: FileTransferDetail, isUpdate)) =>
            isUpdate shouldBe false
            validateFileTransfer(details, fileTransfer2)
          case Right(_) => fail()
          case Left(_) => fail()
        }

        getAll.size shouldBe 1

      }

      "save api should handle duplicates " in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(apiDetail1))

        getAll.size shouldBe 1

        val savedValue = getAll.head
        savedValue.publisherReference shouldBe apiDetail1.publisherReference

        val duplicateApi = apiDetail5.copy(publisherReference = apiDetail1.publisherReference)

        val updateResult = await(repo.findAndModify(duplicateApi))

        updateResult match {

          case Right((apiDetail: ApiDetail, isUpdate)) =>
            isUpdate shouldBe true
            apiDetail.id shouldBe savedValue.id
            apiDetail.publisherReference shouldBe apiDetail1.publisherReference
            apiDetail.title shouldBe apiDetail5.title
            apiDetail.description shouldBe apiDetail5.description
            apiDetail.shortDescription shouldBe apiDetail5.shortDescription
            apiDetail.lastUpdated.toString shouldBe apiDetail5.lastUpdated.toString
            apiDetail.platform shouldBe apiDetail5.platform
            apiDetail.maintainer shouldBe apiDetail5.maintainer
            apiDetail.version shouldBe apiDetail5.version
            apiDetail.specificationType shouldBe apiDetail5.specificationType
            apiDetail.hods shouldBe apiDetail5.hods
            apiDetail.openApiSpecification shouldBe apiDetail5.openApiSpecification
            apiDetail.domain shouldBe apiDetail5.domain
            apiDetail.subDomain shouldBe apiDetail5.subDomain
            apiDetail.apiType shouldBe apiDetail5.apiType
            apiDetail.apiGeneration shouldBe apiDetail5.apiGeneration
          case Right(_) => fail()
          case Left(_) => fail()
        }

        getAll.size shouldBe 1

      }

      "Save API should serialize all fields in APIDetail except the primary key '_id'" in {
        await(repo.findAndModify(apiDetail1)) match {
          case Right((persisted: ApiDetail, _)) => {
            val id = UUID.randomUUID()
            val now = Instant.now
            val actual = persisted.copy(id = IntegrationId(id), lastUpdated = now, reviewedDate = now)
            val reference = apiDetail1.copy(id = IntegrationId(id), lastUpdated = now, reviewedDate = now)

            classOf[ApiDetail].getDeclaredFields foreach (f => {
              if (!f.getName.equals("_id")) {
                f.setAccessible(true)
                f.get(actual) shouldBe (f.get(reference))
              }
            })
          }
          case _ => fail()
        }

        getAll.size shouldBe 1
      }
    }

    "deleteByPlatformType" should {
      "return 2 when delete by platformType called against collection with 2 matching apis" in {

        await(repo.findAndModify(apiDetail1))
        await(repo.findAndModify(fileTransfer2))
        await(repo.findAndModify(apiDetail3))
        await(repo.findAndModify(apiDetail4))

        val result2 = getAll
        result2.size shouldBe 4

        val result = await(repo.deleteByPlatform(CORE_IF))
        result shouldBe 3

        val result3 = getAll
        result3.size shouldBe 1
        result3.head.platform shouldBe DES

      }

      "return 0 when delete by platformType called against collection with zero matching apis" in {
        await(repo.findAndModify(apiDetail1))
        await(repo.findAndModify(apiDetail3))
        await(repo.findAndModify(apiDetail4))

        val result2 = getAll
        result2.size shouldBe 3

        val result = await(repo.deleteByPlatform(CMA))
        result shouldBe 0

        val result3 = getAll
        result3.size shouldBe 3

      }
    }

    "deleteByIntegrationId" should {

      "delete the api with the given integrationId if it exists" in {

        await(repo.findAndModify(apiDetail5))
        val result = await(repo.findAndModify(apiDetail1))
        result match {
          case Left(_) => fail()
          case Right((integration: IntegrationDetail, _)) =>
            val result2 = getAll
            result2.size shouldBe 2

            deleteById(integration.id)

            val result3 = getAll
            result3.size shouldBe 1
            validateApi(result3.head, apiDetail5)
        }

      }

      "not delete any apis if none with that integrationId exist" in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(apiDetail1))
        await(repo.findAndModify(apiDetail5))

        val result2 = getAll
        result2.size shouldBe 2

        deleteById(IntegrationId(UUID.randomUUID()))

        val result3 = getAll
        result3.size shouldBe 2
      }
    }

    "findWithFilters" should {

      "find 2 results when no search term or platform filters and currentPage = 1, perPage = 2" in new FilterSetup {
        setUpTest()

        validatePagedResults(
          findWithFilters(IntegrationFilter(List.empty, List.empty, currentPage = Some(1), itemsPerPage = Some(2))),
          List("API1002", "API1007"),
          6
        )
        validatePagedResults(
          findWithFilters(IntegrationFilter(List.empty, List.empty, currentPage = Some(2), itemsPerPage = Some(2))),
          List("API1001", "API1003"),
          6
        )
        validatePagedResults(
          findWithFilters(IntegrationFilter(List.empty, List.empty, currentPage = Some(3), itemsPerPage = Some(2))),
          List("API1004", "API1005"),
          6
        )
      }

      "find 3 results when searching for text that exists in title, endpoint summary with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("BOOP"), List.empty)).results, List("API1005", "API1003", "API1004"))
      }

      "find 1 result when searching for text that exists in endpoint description with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("DEEPSEARCH"), List.empty)).results, List("API1003"))

      }

      "find 1 result when searching for text that exists in endpoint description but uses stemming with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("DEEPSEARCHES"), List.empty)).results, List("API1003"))
      }

      "find 2 results when searching for text getKnownFactsDesc with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("getKnownFactsDesc"), List.empty)).results, List("API1001", "API1004"))
      }

      "find 1 result when searching for for text that exists in all records & DES platform" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("getKnownFactsDesc"), List(DES))).results, List("API1004"))
      }

      "find 2 results when searching for text getKnownFactsDesc & DES or CORE_IF platforms" in new FilterSetup {
        setUpTest()
        validateResults(
          findWithFilters(IntegrationFilter(List("getKnownFactsDesc"), List(DES, CORE_IF))).results,
          List("API1001", "API1004")
        )
      }

      "find 1 result when searching for quoted text containing a specific API number" in  new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List(s""""${apiDetail1.apiNumber.value}""""))).results, List("API1001"))
      }

      "find multiple results when searching for text containing the API number for API1001, with that API first" in  new FilterSetup {
        setUpTest()
        val results: Seq[IntegrationDetail] = findWithFilters(IntegrationFilter(List(apiDetail1.apiNumber.value))).results
        val topResult: Seq[IntegrationDetail] = Seq(results.head)
        results.size should be > 1
        validateResults(topResult, List("API1001"))
      }

      "find multiple results when searching for text containing the API number for API1005, with that API first" in  new FilterSetup {
        setUpTest()
        val results: Seq[IntegrationDetail] = findWithFilters(IntegrationFilter(List(apiDetail5.apiNumber.value))).results
        val topResult: Seq[IntegrationDetail] = Seq(results.head)
        results.size should be > 1
        validateResults(topResult, List("API1005"))
      }

      "find 2 results when searching for backend CUSTOMS" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS"))).results, List("API1003", "API1004"))
      }

      "find 3 result when searching for backend CUSTOMS and ETMP" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS", "ETMP"))).results, List("API1003", "API1004", "API1005"))
      }

      "find 2 results when searching for backend CUSTOMS and ETMP with multiple team ids" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS", "ETMP"), teamIds = List("team_id_1", "team_id_2", "team_id_404"))).results, List("API1003", "API1004"))
      }

      "find 1 result when searching for backend source " in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("source"))).results, List("API1002"))
      }

      "find 1 result when searching for backend target " in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("target"))).results, List("API1002", "API1007"))
      }

      "find 2 result when searching for backend source and target " in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("source", "target"))).results, List("API1002", "API1007"))
      }

      "find 4 result when searching for backend CUSTOMS, ETMP and source" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS", "ETMP", "source"))).results, List("API1002", "API1003", "API1004", "API1005"))
      }

      "find 2 file transfers when searching for type File Transfer" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(typeFilter = Some(FILE_TRANSFER))).results, List("API1002", "API1007"))
      }

      "find 4 apis when searching for type API" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(typeFilter = Some(API))).results, List("API1001", "API1003", "API1004", "API1005"))
      }
    }

    "getCatalogueReport" should {
      "return List of results when entries exist in the database" in new FilterSetup {
        setUpTest()
        val expectedList = List(
          IntegrationPlatformReport(API_PLATFORM, FILE_TRANSFER, 1),
          IntegrationPlatformReport(CORE_IF, API, 3),
          IntegrationPlatformReport(CORE_IF, FILE_TRANSFER, 1),
          IntegrationPlatformReport(DES, API, 1)
        )
        val result: List[IntegrationPlatformReport] = await(repo.getCatalogueReport())
        result.sortBy(_.integrationType.toString).sortBy(_.platformType.toString) shouldBe expectedList

      }

      "return empty List when no results in database" in new FilterSetup {
        await(repo.getCatalogueReport()) shouldBe List.empty
      }
    }

    "getFileTransferTransportsByPlatform" should {
      def setupFileTransfers() = {
        await(repo.findAndModify(fileTransfer2)) // CORE_IF | source -> target | transports = UTM
        await(repo.findAndModify(fileTransfer7)) // API_PLATFORM | someSource -> target | transports = S3
        await(repo.findAndModify(fileTransfer8)) // API_PLATFORM | someSource -> target | transports = S3, WTM
      }

      "return all transports when no source and target are provided" in {
        setupFileTransfers()
        val expectedResults = List(
          FileTransferTransportsForPlatform(API_PLATFORM, List("AB", "S3", "WTM")),
          FileTransferTransportsForPlatform(CORE_IF, List("UTM"))
        )
        val result: List[FileTransferTransportsForPlatform] = await(repo.getFileTransferTransportsByPlatform(None, None))
        result shouldBe expectedResults
      }

      "return all transports when only source is provided" in {
        setupFileTransfers()
        val expectedResults = List(
          FileTransferTransportsForPlatform(API_PLATFORM, List("AB", "S3", "WTM")),
          FileTransferTransportsForPlatform(CORE_IF, List("UTM"))
        )
        val result: List[FileTransferTransportsForPlatform] = await(repo.getFileTransferTransportsByPlatform(Some("someSource"), None))
        result shouldBe expectedResults
      }

      "return all transports when only target is provided" in {
        setupFileTransfers()
        val expectedResults = List(
          FileTransferTransportsForPlatform(API_PLATFORM, List("AB", "S3", "WTM")),
          FileTransferTransportsForPlatform(CORE_IF, List("UTM"))
        )
        val result: List[FileTransferTransportsForPlatform] = await(repo.getFileTransferTransportsByPlatform(None, Some("target")))
        result shouldBe expectedResults
      }

      "return CORE_IF transports when source=source and target=target" in {
        setupFileTransfers()
        val expectedResults = List(
          FileTransferTransportsForPlatform(CORE_IF, List("UTM"))
        )
        val result: List[FileTransferTransportsForPlatform] = await(repo.getFileTransferTransportsByPlatform(Some("source"), Some("target")))
        result shouldBe expectedResults
      }
    }

    "getTotalApisCount" should {
      "return 4 when there are 4 APIs in the DB" in new FilterSetup {
        setUpTest()
        await(repo.getTotalApisCount()) shouldBe 4
      }

      "return 0 when there are none APIs in the DB" in new FilterSetup {
        await(repo.getTotalApisCount()) shouldBe 0
      }
    }

    "getTotalEndpointsCount" should {
      "return 8 when there are 8 API endpoints in the DB" in new FilterSetup {
        setUpTest()

        await(repo.getTotalEndpointsCount()) shouldBe 8
      }

      "return 0 when there are none APIs in the DB" in new FilterSetup {
        await(repo.findAndModify(fileTransfer2))

        await(repo.getTotalEndpointsCount()) shouldBe 0
      }
    }

    "updateTeamId" should {
      val teamId = "myTeam123"

      "return updated API if it already exists" in new FilterSetup {
        val (apiDetail, _) = await(repo.findAndModify(apiDetail1)).toOption.get

        await(repo.updateTeamId(apiDetail.id, Some(teamId))) match {
          case Some(updatedApi: ApiDetail) =>
            updatedApi.teamId shouldBe Some(teamId)
            updatedApi.id shouldBe apiDetail.id
          case _ => fail()
        }
      }

      "return None when specified API does not exist" in new FilterSetup {
        await(repo.updateTeamId(IntegrationId(UUID.randomUUID()), Some(teamId))) shouldBe None
      }
    }
  }
}