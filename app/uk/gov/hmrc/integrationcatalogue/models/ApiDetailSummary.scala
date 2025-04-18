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

package uk.gov.hmrc.integrationcatalogue.models

import org.bson.types.ObjectId
import play.api.libs.json.{Format, JsObject, Json, OWrites, Reads, Writes}
import uk.gov.hmrc.integrationcatalogue.models.common.{ApiGeneration, ApiType, IntegrationId, PlatformType}
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats.Implicits.objectIdFormat

import java.time.Instant

case class ApiDetailSummary(
  _id: ObjectId = new ObjectId,
  id: IntegrationId,
  publisherReference: String,
  title: String,
  shortDescription: Option[String],
  apiStatus: ApiStatus,
  domain: Option[String],
  subDomain: Option[String],
  hods: Seq[String],
  platform: PlatformType,
  apiType: Option[ApiType],
  teamId: Option[String],
  apiNumber: Option[String],
  apiGeneration: Option[ApiGeneration]
) {

  def created: Instant = {
    Instant.ofEpochSecond(_id.getTimestamp)
  }

}

object ApiDetailSummary {

  def apply(apiDetail: ApiDetail): ApiDetailSummary = {
    ApiDetailSummary(
      _id = apiDetail._id,
      id = apiDetail.id,
      publisherReference = apiDetail.publisherReference,
      title = apiDetail.title,
      shortDescription = apiDetail.shortDescription,
      apiStatus = apiDetail.apiStatus,
      domain = apiDetail.domain,
      subDomain = apiDetail.subDomain,
      hods = apiDetail.hods,
      platform = apiDetail.platform,
      apiType = apiDetail.apiType,
      teamId = apiDetail.teamId,
      apiNumber = apiDetail.apiNumber,
      apiGeneration= apiDetail.apiGeneration,
    )
  }

  private val readsApiDetailSummary: Reads[ApiDetailSummary] = Json.reads[ApiDetailSummary]

  private val writeApiDetailSummary: Writes[ApiDetailSummary] = new OWrites[ApiDetailSummary] {
    private val oWritesApiDetailSummary = Json.writes[ApiDetailSummary]

    override def writes(apiDetailSummary: ApiDetailSummary): JsObject = {
      oWritesApiDetailSummary.writes(apiDetailSummary) + ("created", Json.toJson(apiDetailSummary.created)) - "_id"
    }
  }

  implicit val formatApiDetailSummary: Format[ApiDetailSummary] = Format(readsApiDetailSummary, writeApiDetailSummary)

}
