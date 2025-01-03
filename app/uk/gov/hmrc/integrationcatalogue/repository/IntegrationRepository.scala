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

package uk.gov.hmrc.integrationcatalogue.repository

import com.mongodb.BasicDBObject
import org.bson.BsonValue
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Accumulators.*
import org.mongodb.scala.model.Aggregates.*
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Indexes.*
import org.mongodb.scala.model.Updates.{set, setOnInsert, unset}
import org.mongodb.scala.model.*
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.models.Types.IsUpdate
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, IntegrationType, PlatformType}
import uk.gov.hmrc.integrationcatalogue.repository.MongoFormatters.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//noinspection AccessorLikeMethodIsEmptyParen
@Singleton
class IntegrationRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[IntegrationDetail](
      collectionName = "integrations",
      mongoComponent = mongo,
      domainFormat = integrationDetailFormats,
      indexes = Seq(
        IndexModel(ascending("id"), IndexOptions().name("id_index").background(true).unique(true)),
        IndexModel(ascending("_type"), IndexOptions().name("type_index").background(true).unique(false)),
        IndexModel(ascending("hods"), IndexOptions().name("hods_index").background(true).unique(false)),
        IndexModel(ascending("sourceSystem"), IndexOptions().name("sourceSystem_index").background(true).unique(false)),
        IndexModel(ascending("targetSystem"), IndexOptions().name("targetSystem_index").background(true).unique(false)),
        IndexModel(ascending(List("platform", "publisherReference")*), IndexOptions().name("platform_pub_ref_idx").background(true).unique(true)),
        IndexModel(
          Indexes.text("$**"),
          IndexOptions().weights(new BasicDBObject().append("title", 50).append("description", 25))
            .name("text_index_1_1").background(true)
        ),
        IndexModel(ascending("teamId"), IndexOptions().name("teamId_index").background(true).unique(false).sparse(true))
      ),
      replaceIndexes = false
    )
    with Logging {

  override lazy val requiresTtlIndex = false // There are no requirements to expire applications

  // https://docs.mongodb.com/manual/core/index-text/#wildcard-text-indexes

  private def determineUpdateOp2(integrationDetail: IntegrationDetail) = {
    integrationDetail.integrationType match {
      case IntegrationType.API           =>
        val apiDetail = integrationDetail.asInstanceOf[ApiDetail]
        List(
          set("title", apiDetail.title),
          set("description", apiDetail.description),
          set("shortDescription", Codecs.toBson(apiDetail.shortDescription)),
          set("platform", Codecs.toBson(apiDetail.platform)),
          set("lastUpdated", Codecs.toBson(apiDetail.lastUpdated)),
          set("reviewedDate", Codecs.toBson(apiDetail.reviewedDate)),
          set("maintainer", Codecs.toBson(apiDetail.maintainer)),
          set("version", apiDetail.version),
          set("specificationType", Codecs.toBson(apiDetail.specificationType)),
          set("hods", apiDetail.hods),
          set("apiStatus", Codecs.toBson(apiDetail.apiStatus)),
          set("endpoints", apiDetail.endpoints.map(Codecs.toBson(_))),
          set("openApiSpecification", Codecs.toBson(apiDetail.openApiSpecification)),
          set("scopes", apiDetail.scopes.map(Codecs.toBson(_))),
          set("domain", Codecs.toBson(apiDetail.domain)),
          set("subDomain", Codecs.toBson(apiDetail.subDomain)),
          set("apiType", Codecs.toBson(apiDetail.apiType))
        )
      case IntegrationType.FILE_TRANSFER =>
        val fileTransferDetail: FileTransferDetail = integrationDetail.asInstanceOf[FileTransferDetail]
        List(
          set("fileTransferSpecificationVersion", fileTransferDetail.fileTransferSpecificationVersion),
          set("title", fileTransferDetail.title),
          set("description", fileTransferDetail.description),
          set("platform", Codecs.toBson(fileTransferDetail.platform)),
          set("lastUpdated", Codecs.toBson(fileTransferDetail.lastUpdated)),
          set("reviewedDate", Codecs.toBson(fileTransferDetail.reviewedDate)),
          set("maintainer", Codecs.toBson(fileTransferDetail.maintainer)),
          set("sourceSystem", fileTransferDetail.sourceSystem),
          set("targetSystem", fileTransferDetail.targetSystem),
          set("transports", fileTransferDetail.transports),
          set("fileTransferPattern", fileTransferDetail.fileTransferPattern)
        )
    }

  }

  def findAndModify(
    integrationDetail: IntegrationDetail,
    maybeApiTeam: Option[ApiTeam] = Option.empty
  ): Future[Either[Exception, (IntegrationDetail, IsUpdate)]] = {
    for {
      existingIntegration <- collection.find(and(
                    equal("publisherReference", integrationDetail.publisherReference),
                    equal("platform", Codecs.toBson(integrationDetail.platform))
                  ))
                    .toFuture().map(results => results.headOption)
      isUpdate = existingIntegration.isDefined
      result   <- findAndModify2(integrationDetail, isUpdate, maybeApiTeam, existingIntegration)
    } yield result
  }

  private def findAndModify2(
    integrationDetail: IntegrationDetail,
    isUpsert: Boolean,
    maybeApiTeam: Option[ApiTeam],
    existingIntegration: Option[IntegrationDetail],
  ): Future[Either[Exception, (IntegrationDetail, IsUpdate)]] = {

    val query = and(equal("platform", Codecs.toBson(integrationDetail.platform)), equal("publisherReference", integrationDetail.publisherReference))

    val updateOp = determineUpdateOp2(integrationDetail)

    val setOnInsertOperations = List(
      setOnInsert("id", Codecs.toBson(UUID.randomUUID())),
      setOnInsert("publisherReference", integrationDetail.publisherReference),
      setOnInsert("_type", integrationDetail.integrationType.integrationType)
    )

    val setTeamIdOperation = maybeApiTeam.map(_.teamId).map(teamId => List(setOnInsert("teamId", teamId))).getOrElse(List.empty)

    val setCreationDateOperation = existingIntegration.flatMap(_.createdDate).fold(set("createdDate", Instant.now()))(_ => empty())

    val allOps = setOnInsertOperations ++ updateOp ++ setTeamIdOperation :+ setCreationDateOperation

    collection.findOneAndUpdate(
      filter = query,
      update = Updates.combine(allOps*),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture()
      .map(x => Right((x, isUpsert)))// not sure how we get is upserted / updated now))
      .recover {
        case e: Exception =>
          logger.warn(s"IntegrationDetail find and modify error - ${e.getMessage}")
          Left(new Exception(s"IntegrationDetail upsert error.  attempted to be updated but no records were amended"))
      }
  }

  def findWithFilters(filter: IntegrationFilter): Future[IntegrationResponse] = {
    // total count, per page, current page
    def sendPagedResults(results: List[IntegrationDetail], perPageFilter: Option[Int]) = {
      if (perPageFilter.isDefined) Some(results.size) else None
    }

    val filters = buildFilters(filter)

    val sortByScore          = Sorts.metaTextScore("score")
    val mayBeScoreProjection = filter.searchText.headOption.map(_ => Projections.metaTextScore("score"))
    val sortOp               = if (filter.searchText.headOption.isEmpty) ascending("title") else sortByScore
    val perPage              = filter.itemsPerPage.getOrElse(0)
    val currentPage          = filter.currentPage.getOrElse(1)
    val skipAmount           = if (currentPage > 1) (currentPage - 1) * perPage else 0
    if (filters.isEmpty) {
      for {
        count   <- collection.countDocuments().toFuture()
        results <- collection.find()
                     .skip(skipAmount)
                     .limit(perPage)
                     .sort(ascending("title"))
                     .toFuture()
                     .map(_.toList)
      } yield IntegrationResponse(count.toInt, sendPagedResults(results, filter.itemsPerPage), results)
    } else {
      for {
        count   <- collection.countDocuments(and(filters*)).toFuture()
        results <-
          (mayBeScoreProjection match {
            case None          => collection.find(and(filters*))
            case Some(x: Bson) => collection.find(and(filters*)).projection(x)
          }).sort(sortOp)
            .skip(skipAmount)
            .limit(perPage)
            .toFuture()
            .map(_.toList)
      } yield IntegrationResponse(count.toInt, sendPagedResults(results, filter.itemsPerPage), results)
    }
  }

  private def buildFilters(filter: IntegrationFilter): Seq[Bson] = {
    val integrationTypeFilter    = filter.typeFilter.map(typeVal => Filters.equal("_type", typeVal.integrationType))
    val textFilter: Option[Bson] = filter.searchText.headOption.map(searchText => Filters.text(searchText))
    val platformFilter           = if (filter.platforms.nonEmpty) Some(Filters.in("platform", filter.platforms.map(Codecs.toBson(_))*)) else None
    val backendsFilter           = if (filter.backends.nonEmpty) Some(Filters.in("hods", filter.backends*)) else None
    val sourceFilter             = if (filter.backends.nonEmpty) Some(Filters.in("sourceSystem", filter.backends*)) else None
    val targetFilter             = if (filter.backends.nonEmpty) Some(Filters.in("targetSystem", filter.backends*)) else None
    val combinedHodsFilters      = Seq(backendsFilter, sourceFilter, targetFilter).flatten
    val hodsFilter               = if (combinedHodsFilters.isEmpty) None else Some(Filters.or(combinedHodsFilters*))
    val teamIdsFilter            = if (filter.teamIds.nonEmpty) Some(Filters.in("teamId", filter.teamIds.map(Codecs.toBson(_))*)) else None

    Seq(integrationTypeFilter, textFilter, platformFilter, hodsFilter, teamIdsFilter).flatten
  }

  def getCatalogueReport(): Future[List[IntegrationPlatformReport]] = {
    collection.aggregate[BsonValue](List(
      group(Document("platform" -> "$platform", "integrationType" -> "$_type"), sum("count", 1))
    )).toFuture()
      .map(_.toList.map(Codecs.fromBson[IntegrationCountResponse]))
      .map(items =>
        items.map(item => IntegrationPlatformReport(item._id.platform, IntegrationType.fromIntegrationTypeString(item._id.integrationType), item.count))
          .sortBy(x => (x.platformType.toString.replace("_", ""), x.integrationType.entryName))
      )
  }

  def getFileTransferTransportsByPlatform(source: Option[String], target: Option[String]): Future[List[FileTransferTransportsForPlatform]] = {

    val aggregatePipelineWithoutMatch: Seq[Bson] = List(
      unwind("$transports"),
      group(Document("platform" -> "$platform"), addToSet("transports", "$transports"))
    )

    val aggregatePipeline = (source, target) match {
      case (Some(s), Some(t)) => `match`(Document("sourceSystem" -> s, "targetSystem" -> t)) +: aggregatePipelineWithoutMatch
      case _                  => aggregatePipelineWithoutMatch
    }

    collection.aggregate[BsonValue](aggregatePipeline).toFuture()
      .map(_.toList.map(Codecs.fromBson[FileTransferTransportsResponse]))
      .map(items =>
        items.map(item => FileTransferTransportsForPlatform(item._id.platform, item.transports.sortWith(_ < _)))
          .sortBy(x => x.platform.toString.replace("_", ""))
      )
  }

  def findById(id: IntegrationId): Future[Option[IntegrationDetail]] = {
    collection.find(equal("id", Codecs.toBson(id))).toFuture().map(_.headOption)
      .recover {
        case e: Exception =>
          logger.warn(s"IntegrationDetail find error - ${e.getMessage}")
          None
      }
  }

  def findByPublisherRef(platformType: PlatformType, publisherReference: String): Future[Option[IntegrationDetail]] = {
    collection
      .find(
        and(
          equal("platform", Codecs.toBson(platformType)),
          equal("publisherReference", publisherReference)
        )
      )
      .headOption()
  }

  def exists(platformType: PlatformType, publisherReference: String):Future[Boolean] = {
    findByPublisherRef(platformType, publisherReference)
      .map(_.isDefined)
  }

  def deleteById(id: IntegrationId): Future[Boolean] = {
    collection.deleteOne(equal("id", Codecs.toBson(id))).toFuture().map(x => x.getDeletedCount == 1)
  }

  def deleteByPlatform(platform: PlatformType): Future[Int] = {
    collection.deleteMany(equal("platform", Codecs.toBson(platform))).toFuture().map(x => x.getDeletedCount.toInt)
  }

  def getTotalApisCount(): Future[Long] = {
    collection.countDocuments(equal("_type", Codecs.toBson("uk.gov.hmrc.integrationcatalogue.models.ApiDetail"))).toFuture()
  }

  //noinspection ScalaStyle
  def getTotalEndpointsCount(): Future[Int] = {
    val totalEndpointsCountAggregation = Seq(
      `match`(equal("_type", "uk.gov.hmrc.integrationcatalogue.models.ApiDetail")),
      unwind("$endpoints"),
      project(Document("""{_id: 0, endpointsCount: {$size : "$endpoints.methods"}}""")),
      group(null, sum("totalEndpointsCount", "$endpointsCount"))
    )

    collection.aggregate[BsonValue](totalEndpointsCountAggregation)
      .head()
      .map { result => result.asDocument().get("totalEndpointsCount").asNumber().intValue() }
      .recover { case _: Exception => 0 }
  }

  def updateTeamId(integrationId: IntegrationId, maybeTeamId: Option[String]): Future[Option[IntegrationDetail]] = {

    val update = maybeTeamId match {
      case Some(teamId) => set("teamId", teamId)
      case _ => unset("teamId")
    }

    collection.findOneAndUpdate(
      equal("id", Codecs.toBson(integrationId)),
      update,
      FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER)
    ).toFuture() map {
        case integrationDetail: IntegrationDetail => Some(integrationDetail)
        case null => None
    }
  }
}
