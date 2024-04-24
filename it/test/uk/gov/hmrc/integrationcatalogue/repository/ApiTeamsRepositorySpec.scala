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

import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integrationcatalogue.models.ApiTeam
import uk.gov.hmrc.integrationcatalogue.repository.ApiTeamsRepository.MongoApiTeam
import uk.gov.hmrc.integrationcatalogue.support.MdcTesting
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext

class ApiTeamsRepositorySpec
  extends AnyWordSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[MongoApiTeam]
    with OptionValues
    with MdcTesting {

  import ApiTeamsRepositorySpec._

  private lazy val playApplication = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent),
        bind[Clock].toInstance(clock)
      )
      .build()
  }

  private implicit lazy val executionContext: ExecutionContext = {
    playApplication.injector.instanceOf[ExecutionContext]
  }

  override protected val repository: ApiTeamsRepository = {
    playApplication.injector.instanceOf[ApiTeamsRepository]
  }

  "ApiTeamsRepository" when {
    "upsert" should {
      "insert a record when none exists for the publisher reference" in {
        setMdcData()

        val apiTeam = ApiTeam(publisherReference, teamId1)
        val result = repository.upsert(apiTeam).map(ResultWithMdcData(_)).futureValue

        find() shouldBe MongoApiTeam(apiTeam, lastUpdated)

        result.mdcData shouldBe testMdcData
      }

      "update a record when one exists for the publisher reference" in {
        setMdcData()

        val mongoApiTeam = MongoApiTeam(publisherReference, teamId1, lastUpdated.minusSeconds(1))
        insert(mongoApiTeam).futureValue

        val updated = mongoApiTeam.copy(
          teamId = teamId2,
          lastUpdated = lastUpdated
        )

        val result = repository.upsert(updated.toApiTeam).map(ResultWithMdcData(_)).futureValue

        find() shouldBe updated

        result.mdcData shouldBe testMdcData
      }
    }

    "findByPublisherReference" should {
      "return a record when one exists for the publisher reference" in {
        setMdcData()

        val mongoApiTeam = MongoApiTeam(publisherReference, teamId1, lastUpdated)
        insert(mongoApiTeam).futureValue

        val found = repository.findByPublisherReference(publisherReference).map(ResultWithMdcData(_)).futureValue

        found.data shouldBe Some(mongoApiTeam.toApiTeam)
        found.mdcData shouldBe testMdcData
      }

      "return None when none exists for the publisher reference" in {
        setMdcData()

        val found = repository.findByPublisherReference(publisherReference).map(ResultWithMdcData(_)).futureValue

        found.data shouldBe None
        found.mdcData shouldBe testMdcData
      }
    }
  }

  private def find(): MongoApiTeam = {
    find(Filters.equal("publisherReference", publisherReference))
      .futureValue
      .headOption
      .value
  }

}

object ApiTeamsRepositorySpec {

  val publisherReference = "test-publisher-reference"
  val teamId1 = "test-team-id-1"
  val teamId2 = "test-team-id-2"
  val clock: Clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneId.of("UTC"))
  val lastUpdated: Instant = Instant.now(clock) //.truncatedTo(ChronoUnit.MILLIS) // Micros is beyond the JSON resolution

}
