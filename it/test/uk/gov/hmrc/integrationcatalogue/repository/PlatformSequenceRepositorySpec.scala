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

import org.mongodb.scala.ObservableFuture
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integrationcatalogue.config.PlatformNumbering
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.HIP
import uk.gov.hmrc.integrationcatalogue.models.PlatformSequence
import uk.gov.hmrc.integrationcatalogue.support.MdcTesting
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext

class PlatformSequenceRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[PlatformSequence]
    with OptionValues
    with MdcTesting {

  import PlatformSequenceRepositorySpec.*

  private lazy val playApplication = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent)
      )
      .build()
  }

  private implicit lazy val executionContext: ExecutionContext = {
    playApplication.injector.instanceOf[ExecutionContext]
  }

  override protected val repository: PlatformSequenceRepository = new PlatformSequenceRepository(mongoComponent)

  "PlatformSequenceRepository.nextValue" -{
    "must insert the sequence record when it does not exist and return the correct next value" in {
      setMdcData()

      val result = repository.nextValue(platformNumbering).map(ResultWithMdcData(_)).futureValue

      result.data mustBe platformNumbering.start
      result.mdcData mustBe testMdcData

      val expected = PlatformSequence(
        platform = platformNumbering.platformType.entryName,
        sequence = 1
      )

      repository.collection.find().toFuture().futureValue mustBe Seq(expected)
    }

    "must increment the sequence record when it does exist and return the correct next value" in {
      val startSequence = PlatformSequence(
        platform = platformNumbering.platformType.entryName,
        sequence = 2
      )

      repository.collection.insertOne(startSequence).toFuture().futureValue

      setMdcData()

      val result = repository.nextValue(platformNumbering).map(ResultWithMdcData(_)).futureValue

      result.data mustBe platformNumbering.start + startSequence.sequence
      result.mdcData mustBe testMdcData

      val expected = PlatformSequence(
        platform = platformNumbering.platformType.entryName,
        sequence = startSequence.sequence + 1
      )

      repository.collection.find().toFuture().futureValue mustBe Seq(expected)
    }
  }

}

private object PlatformSequenceRepositorySpec {

  val platformNumbering: PlatformNumbering = PlatformNumbering(
    platformType = HIP,
    start = 42
  )

}
