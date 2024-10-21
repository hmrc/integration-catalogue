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

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Filters, Indexes, Projections, Sorts}
import uk.gov.hmrc.integrationcatalogue.models.ApiDetailSummary
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationType, PlatformType}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiDetailSummaryRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ApiDetailSummary](
    collectionName = "integrations",
    mongoComponent = mongoComponent,
    domainFormat = ApiDetailSummary.formatApiDetailSummary,
    indexes = Seq.empty
  ) {

  import ApiDetailSummaryRepository.*

  override lazy val requiresTtlIndex = false // There are no requirements to expire integrations

  def findWithFilters(searchTerms: List[String], platformTypes: List[PlatformType]): Future[Seq[ApiDetailSummary]] = {
    Mdc.preservingMdc {
      collection
        .find(filter(searchTerms, platformTypes))
        .projection(projection(searchTerms))
        .sort(sort(searchTerms))
        .toFuture()
    }
  }

  private def filter(searchTerms: List[String], platformTypes: List[PlatformType]): Bson = {
    Seq(
      Some(Filters.equal("_type", IntegrationType.API.integrationType)),
      searchTermFilter(searchTerms),
      platformTypeFilter(platformTypes)
    ).flatten match {
      case filter :: Nil => filter
      case filters => Filters.and(filters*)
    }
  }

  private def searchTermFilter(searchTerms: List[String]): Option[Bson] = {
    // This preserves the behaviour of IntegrationRepository.
    // Only the first search term is used.
    searchTerms
      .headOption
      .map(Filters.text)
  }

  private def platformTypeFilter(platformTypes: List[PlatformType]): Option[Bson] = {
    if (platformTypes.nonEmpty) {
      Some(Filters.in("platform", platformTypes.map(_.entryName)*))
    }
    else {
      None
    }
  }

  private def projection(searchTerms: List[String]) = {
    Projections.fields(
      Seq(
        Some(excludeOasProjection),
        if (searchTerms.nonEmpty) Some(scoreProjection) else None
      ).flatten*
    )
  }

  private def sort(searchTerm: List[String]): Bson = {
    if (searchTerm.nonEmpty) {
      scoreSort
    }
    else {
      titleSort
    }
  }

}

private object ApiDetailSummaryRepository {

  private val excludeOasProjection: Bson = Projections.exclude("openApiSpecification")
  private val scoreProjection: Bson = Projections.metaTextScore("score")
  private val titleSort: Bson = Indexes.ascending("title")
  private val scoreSort: Bson = Sorts.metaTextScore("score")

}
