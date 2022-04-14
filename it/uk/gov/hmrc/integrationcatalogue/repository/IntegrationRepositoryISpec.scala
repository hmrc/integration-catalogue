package uk.gov.hmrc.integrationcatalogue.repository

import org.bson.BsonString
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes.ascending
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integrationcatalogue.models.Types.IsUpdate
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType.{API, FILE_TRANSFER}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{API_PLATFORM, CMA, CORE_IF, DES}
import uk.gov.hmrc.integrationcatalogue.support.{AwaitTestSupport, MongoApp}
import uk.gov.hmrc.integrationcatalogue.testdata.OasParsedItTestData
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class IntegrationRepositoryISpec
  extends AnyWordSpec
    with Matchers
    with MongoApp
    with GuiceOneAppPerSuite
    with ScalaFutures
    with BeforeAndAfterEach
    with OasParsedItTestData
    with AwaitTestSupport {

  override protected def repository: PlayMongoRepository[IntegrationDetail] = app.injector.instanceOf[IntegrationRepository]

  val indexNameToDrop = "please_delete_me__let_me_go"

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}",
        "mongodb.oldIndexesToDrop" -> Seq(indexNameToDrop, "text_index_1_0")
      )

  override implicit lazy val app: Application = appBuilder.build()

  def repo: IntegrationRepository =
    app.injector.instanceOf[IntegrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropMongoDb()
    await(repo.ensureIndexes)
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
      val combinedFuture = for {
        matchInTitle <- repo.findAndModify(exampleApiDetail)
        matchInDescription <- repo.findAndModify(exampleApiDetail2)
        matchInHods <- repo.findAndModify(exampleApiDetailForSearch1)
        matchOnDesPlatform <- repo.findAndModify(exampleApiDetailForSearch2)
        matchOnFileTransferPlatformOne <- repo.findAndModify(exampleFileTransfer)
        matchOnFileTransferPlatformTwo <- repo.findAndModify(exampleFileTransfer2)
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

    def setUpTest() {
      val result = findWithFilters(IntegrationFilter(List.empty, List.empty)).results
      result shouldBe List.empty

      createTestData()
      findWithFilters(IntegrationFilter(List.empty, List.empty)).results.size shouldBe 6
    }

    def validateResults(results: Seq[IntegrationDetail], expectedReferences: List[String]) {
      results.size shouldBe expectedReferences.size
      results.map(_.publisherReference) shouldBe expectedReferences
    }

    def validatePagedResults(integrationResponse: IntegrationResponse, expectedReferences: List[String], expectedCount: Int) {
      validateResults(integrationResponse.results, expectedReferences)

      integrationResponse.pagedCount.map(x => x shouldBe expectedReferences.size)
      integrationResponse.count shouldBe expectedCount
    }
  }

  "IntegrationRepository" when {

    "dropIndexes on Startup" should {

      def checkIndexExists(indexName: String) = {
        val indexes = await(repo.collection.listIndexes().toFuture()).toList
        val indexNames = indexes.flatMap((idx: Document) => {
          idx
            .toList
            .filter(_._1 == "name")
            .map(_._2.asInstanceOf[BsonString])
            .map(_.getValue)
        })
        indexNames.contains(indexName)
      }

      "create index that repo should delete when ensuring indexes " in {
        val createIndexResult = await(repo.collection.createIndex(ascending("version"), IndexOptions().name(indexNameToDrop).background(true).unique(true)).toFuture())
        createIndexResult shouldBe indexNameToDrop
        checkIndexExists(indexNameToDrop) shouldBe true

        await(repo.ensureIndexes)

        checkIndexExists(indexNameToDrop) shouldBe false
      }
    }

    "findByPublisherReference" should {

      "return an api when it has been created" in {
        val result = getAll
        result shouldBe List.empty

        val modifyResult = await(repo.findAndModify(exampleApiDetail))

        modifyResult match {
          case Right((integration, _)) => {
            val result2 = await(repo.findById(integration.id))
            validateApi(result2.get, exampleApiDetail)
          }
          case Left(_) => fail()
        }

      }

    }

    "findById" should {

      "return an integration when it has been created" in {
        val result = getAll
        result shouldBe List.empty

        val modifyResult = await(repo.findAndModify(exampleApiDetail))
        await(repo.findAndModify(exampleApiDetail2))
        modifyResult match {
          case Right((integration, _)) => {

            val result2 = await(repo.findById(integration.id))
            validateApi(result2.get, exampleApiDetail)
          }
          case Left(_) => fail()
        }

      }

      "return None when integrations exist but unknown id" in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(exampleApiDetail))
        await(repo.findAndModify(exampleApiDetail2))

        val result2 = await(repo.findById(IntegrationId(UUID.randomUUID())))
        result2 shouldBe None
      }

    }

    "findAndModify" should {

      "save api when no duplicate exists in collection" in {
        val result = getAll
        result shouldBe List.empty

        val insertResult: Either[Throwable, (IntegrationDetail, Types.IsUpdate)] = await(repo.findAndModify(exampleApiDetail))
        insertResult match {
          case Right((apiDetail: IntegrationDetail, isUpdate)) =>
            isUpdate shouldBe false
            validateApi(apiDetail, exampleApiDetail)
          case Right(_) => fail()
          case Left(_) => fail()
        }

        getAll.size shouldBe 1

      }

      "save file transfer details when no duplicate exists in collection" in {
        val result = getAll
        result shouldBe List.empty

        val insertResult: Either[Throwable, (IntegrationDetail, Types.IsUpdate)] = await(repo.findAndModify(exampleFileTransfer))
        insertResult match {
          case Right((details: FileTransferDetail, isUpdate)) =>
            isUpdate shouldBe false
            validateFileTransfer(details, exampleFileTransfer)
          case Right(_) => fail()
          case Left(_) => fail()
        }

        getAll.size shouldBe 1

      }

      "save api should handle duplicates " in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(exampleApiDetail))

        getAll.size shouldBe 1

        val savedValue = getAll.head
        savedValue.publisherReference shouldBe exampleApiDetail.publisherReference

        val duplicateApi = exampleApiDetail2.copy(publisherReference = exampleApiDetail.publisherReference)

        val updateResult = await(repo.findAndModify(duplicateApi))
        updateResult match {
          case Right((apiDetail: ApiDetail, isUpdate)) =>
            isUpdate shouldBe true
            apiDetail.id shouldBe savedValue.id
            apiDetail.publisherReference shouldBe exampleApiDetail.publisherReference
            apiDetail.title shouldBe exampleApiDetail2.title
            apiDetail.description shouldBe exampleApiDetail2.description
            apiDetail.shortDescription shouldBe exampleApiDetail2.shortDescription
            apiDetail.lastUpdated.toString shouldBe exampleApiDetail2.lastUpdated.toString
            apiDetail.platform shouldBe exampleApiDetail2.platform
            apiDetail.maintainer shouldBe exampleApiDetail2.maintainer
            apiDetail.version shouldBe exampleApiDetail2.version
            apiDetail.specificationType shouldBe exampleApiDetail2.specificationType
            apiDetail.hods shouldBe exampleApiDetail2.hods
            apiDetail.openApiSpecification shouldBe exampleApiDetail2.openApiSpecification
          case Right(_) => fail()
          case Left(_) => fail()
        }

        getAll.size shouldBe 1

      }
    }

    "deleteByPlatformType" should {
      "return 2 when delete by platformType called against collection with 2 matching apis" in {

        await(repo.findAndModify(exampleApiDetail))
        await(repo.findAndModify(exampleFileTransfer))
        await(repo.findAndModify(exampleApiDetailForSearch1))
        await(repo.findAndModify(exampleApiDetailForSearch2))

        val result2 = getAll
        result2.size shouldBe 4

        val result = await(repo.deleteByPlatform(CORE_IF))
        result shouldBe 3

        val result3 = getAll
        result3.size shouldBe 1
        result3.head.platform shouldBe DES

      }

      "return 0 when delete by platformType called against collection with zero matching apis" in {
        await(repo.findAndModify(exampleApiDetail))
        await(repo.findAndModify(exampleApiDetailForSearch1))
        await(repo.findAndModify(exampleApiDetailForSearch2))

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

        await(repo.findAndModify(exampleApiDetail2))
        val result = await(repo.findAndModify(exampleApiDetail))
        result match {
          case Left(_) => fail()
          case Right((integration: IntegrationDetail, _)) =>
            val result2 = getAll
            result2.size shouldBe 2

            deleteById(integration.id)

            val result3 = getAll
            result3.size shouldBe 1
            validateApi(result3.head, exampleApiDetail2)
        }

      }

      "not delete any apis if none with that integrationId exist" in {
        val result = getAll
        result shouldBe List.empty

        await(repo.findAndModify(exampleApiDetail))
        await(repo.findAndModify(exampleApiDetail2))

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
          List("API1007", "API1003"), 6)
        validatePagedResults(
          findWithFilters(IntegrationFilter(List.empty, List.empty, currentPage = Some(2), itemsPerPage = Some(2))),
          List("API1004", "API1001"), 6)
        validatePagedResults(
          findWithFilters(IntegrationFilter(List.empty, List.empty, currentPage = Some(3), itemsPerPage = Some(2))),
          List("API1002", "API1005"), 6)
      }

      "find 3 results when searching for text that exists in title, endpoint summary with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("BOOP"), List.empty)).results, List("API1005", "API1003", "API1004"))
      }

      "find 2 results when searching for text that existing in endpoint description with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("DEEPSEARCH"), List.empty)).results, List("API1003", "API1004"))

      }

      "find 2 results when searching for text that existing in endpoint description but uses stemming with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("DEEPSEARCHES"), List.empty)).results, List("API1003", "API1004"))
      }

      "find 5 results when searching for text that exists in all records with no platform filters" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("getKnownFactsDesc"), List.empty)).results, List("API1001", "API1005", "API1002", "API1003", "API1004"))
      }

      "find 1 result when searching for for text that exists in all records & DES platform" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(List("getKnownFactsDesc"), List(DES))).results, List("API1004"))
      }

      "find 5 results when searching for text that exists in all records & DES or CORE_IF platforms" in new FilterSetup {
        setUpTest()
        validateResults(
          findWithFilters(IntegrationFilter(List("getKnownFactsDesc"), List(DES, CORE_IF))).results,
          List("API1001", "API1005", "API1002", "API1003", "API1004")
        )
      }

      "find 2 results when searching for backend CUSTOMS" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS"))).results, List("API1003", "API1004"))
      }

      "find 3 result when searching for backend CUSTOMS and ETMP" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS", "ETMP"))).results, List("API1003", "API1004", "API1005"))
      }

      "find 1 result when searching for backend source " in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("source"))).results, List("API1002"))
      }

      "find 1 result when searching for backend target " in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("target"))).results, List("API1007", "API1002"))
      }

      "find 2 result when searching for backend source and target " in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("source", "target"))).results, List("API1007", "API1002"))
      }

      "find 4 result when searching for backend CUSTOMS, ETMP and source" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(backends = List("CUSTOMS", "ETMP", "source"))).results, List("API1003", "API1004", "API1002", "API1005"))
      }

      "find 2 file transfers when searching for type File Transfer" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(typeFilter = Some(FILE_TRANSFER))).results, List("API1007", "API1002"))
      }

      "find 4 apis when searching for type API" in new FilterSetup {
        setUpTest()
        validateResults(findWithFilters(IntegrationFilter(typeFilter = Some(API))).results, List("API1003", "API1004", "API1001", "API1005"))
      }

    }

    "getCatalogueReport" should {
      "return List of results when entries exist in the database" in new FilterSetup {
        setUpTest()
        val expectedList = List(
          IntegrationPlatformReport(API_PLATFORM, FILE_TRANSFER, 1),
          IntegrationPlatformReport(CORE_IF, API, 3),
          IntegrationPlatformReport(CORE_IF, FILE_TRANSFER, 1),
          IntegrationPlatformReport(DES, API, 1),
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
        await(repo.findAndModify(exampleFileTransfer)) // CORE_IF | source -> target | transports = UTM
        await(repo.findAndModify(exampleFileTransfer2)) // API_PLATFORM | someSource -> target | transports = S3
        await(repo.findAndModify(exampleFileTransfer3)) // API_PLATFORM | someSource -> target | transports = S3, WTM
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
        await(repo.findAndModify(exampleFileTransfer))

        await(repo.getTotalEndpointsCount()) shouldBe 0
      }
    }
  }
}
