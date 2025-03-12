/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.*
import uk.gov.hmrc.integrationcatalogue.config.PlatformNumbering
import uk.gov.hmrc.integrationcatalogue.models.PlatformSequence
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlatformSequenceRepository @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext) extends PlayMongoRepository[PlatformSequence](
  collectionName = "platform-sequence",
  mongoComponent = mongoComponent,
  domainFormat = PlatformSequence.formatPlatformSequence,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("platform"),
      IndexOptions()
        .name("platformIdx")
        .unique(true)
    )
  )
) {

  override lazy val requiresTtlIndex = false // There are no requirements to expire data

  def nextValue(platformNumbering: PlatformNumbering): Future[Int] = {
    Mdc.preservingMdc {
      collection.findOneAndUpdate(
        filter = Filters.equal("platform", platformNumbering.platformType.entryName),
        update = Updates.combine(
          Updates.set("platform", platformNumbering.platformType.entryName),
          Updates.inc("sequence", 1)
        ),
        options = FindOneAndUpdateOptions()
          .upsert(true)
          .returnDocument(ReturnDocument.AFTER)
      ).toFuture()
    } map {
      platformSequence =>
        platformNumbering.start + platformSequence.sequence - 1
    }
  }

}
