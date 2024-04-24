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
import org.mongodb.scala.model._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.ApiTeam
import uk.gov.hmrc.integrationcatalogue.repository.ApiTeamsRepository.MongoApiTeam
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.play.http.logging.Mdc

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiTeamsRepository @Inject()(
  appConfig: AppConfig,
  mongoComponent: MongoComponent,
  clock: Clock
)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[MongoApiTeam](
    collectionName = "api-teams",
    mongoComponent = mongoComponent,
    domainFormat = MongoApiTeam.formatMongoApiTeam,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("publisherReference"),
        IndexOptions()
          .name("publisherReference")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedTtl")
          .expireAfter(appConfig.apiTeamsTtlDays, TimeUnit.DAYS)
      )
    )
  ) {

  def upsert(apiTeam: ApiTeam): Future[Unit] = {
    Mdc.preservingMdc {
      collection
        .replaceOne(
          filter = Filters.equal("publisherReference", apiTeam.publisherReference),
          replacement = MongoApiTeam(apiTeam, Instant.now(clock)),
          options = ReplaceOptions().upsert(true)
        )
        .toFuture()
    } map(_ => ())
  }

  def findByPublisherReference(publisherReference: String): Future[Option[ApiTeam]] = {
    Mdc.preservingMdc {
      collection
        .find(
          filter = Filters.equal("publisherReference", publisherReference)
        )
        .toFuture()
    } map(_.headOption.map(_.toApiTeam))
  }

}

object ApiTeamsRepository {

  case class MongoApiTeam(publisherReference: String, teamId: String, lastUpdated: Instant) {

    def toApiTeam: ApiTeam = {
      ApiTeam(publisherReference, teamId)
    }

  }

  object MongoApiTeam {

    def apply(apiTeam: ApiTeam, lastUpdated: Instant): MongoApiTeam = {
      MongoApiTeam(apiTeam.publisherReference, apiTeam.teamId, lastUpdated)
    }

    private implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val formatMongoApiTeam: Format[MongoApiTeam] = Json.format[MongoApiTeam]

  }

}
