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

package uk.gov.hmrc.integrationcatalogue.utils

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.integrationcatalogue.config.ApiNumbering
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{CMA, HIP}
import uk.gov.hmrc.integrationcatalogue.repository.PlatformSequenceRepository
import uk.gov.hmrc.integrationcatalogue.testdata.FakeApiNumbering

import scala.concurrent.Future

class ApiNumberGeneratorSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  import ApiNumberGeneratorSpec.*

  "generate" - {
    "must return None when the platform has no rules" in {
      val fixture = buildFixture()

      fixture.apiNumberGenerator.generate(CMA, None).map {
        result =>
          verifyNoInteractions(fixture.repository)
          result mustBe None
      }
    }

    "must return a new API number when the platform has rules and there is no existing value" in {
      val fixture = buildFixture()

      when(fixture.repository.nextValue(eqTo(FakeApiNumbering.hip))).thenReturn(Future.successful(42))

      fixture.apiNumberGenerator.generate(HIP, None).map {
        result =>
          result mustBe Some(fixture.apiNumbering.buildApiNumber(42))
      }
    }

    "must return the existing API number when the platform has rules and there is an existing value" in {
      val fixture = buildFixture()
      val apiNumber = FakeApiNumbering.buildApiNumber(101)

      fixture.apiNumberGenerator.generate(HIP, Some(apiNumber)).map {
        result =>
          verifyNoInteractions(fixture.repository)
          result mustBe Some(apiNumber)
      }
    }
  }

  private def buildFixture(): Fixture = {
    val repository = mock[PlatformSequenceRepository]
    val apiNumberGenerator = new ApiNumberGenerator(FakeApiNumbering, repository)

    Fixture(FakeApiNumbering, repository, apiNumberGenerator)
  }

}

private object ApiNumberGeneratorSpec {

  case class Fixture(
    apiNumbering: ApiNumbering,
    repository: PlatformSequenceRepository,
    apiNumberGenerator: ApiNumberGenerator
  )

}
