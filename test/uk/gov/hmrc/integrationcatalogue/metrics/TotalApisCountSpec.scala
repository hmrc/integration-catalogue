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

package uk.gov.hmrc.integrationcatalogue.metrics

import org.mockito.MockitoSugar.{mock, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TotalApisCountSpec extends AnyWordSpec with Matchers {

  val mockIntegrationRepository: IntegrationRepository = mock[IntegrationRepository]
  val underTest                                        = new TotalApisCount(mockIntegrationRepository)

  "metric refresh" should {

    "produce a metric of total number of apis in database" in {
      val expectedCount = 1
      when(mockIntegrationRepository.getTotalApisCount()).thenReturn(Future.successful(expectedCount))

      await(underTest.metrics) shouldBe Map("totalApisCount" -> expectedCount)
      verify(mockIntegrationRepository).getTotalApisCount()
    }
  }

}
