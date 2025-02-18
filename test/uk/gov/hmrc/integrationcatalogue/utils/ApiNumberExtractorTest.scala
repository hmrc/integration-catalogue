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

package uk.gov.hmrc.integrationcatalogue.utils

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters.instantReads
import uk.gov.hmrc.integrationcatalogue.testdata.ApiTestData

import java.time.{Instant, OffsetDateTime, ZoneOffset}

class ApiNumberExtractorTest extends AnyWordSpec with MockitoSugar with Matchers with ApiTestData with TableDrivenPropertyChecks {
  private val appConfig = mock[AppConfig]
  private val apiNumberExtractor = new ApiNumberExtractor(appConfig)
  
  "ApiNumberExtractor" should {
    "successfully extract valid API numbers from titles" in {
      val testCases = Table(
        ("title", "expectedApiNumber", "expectedTitle"),
        ("acc07 - EIS API Integration (Sync)", Some("acc07"), "EIS API Integration (Sync)"),
        ("API#1001 - Get Payment Details for CY-2", Some("API#1001"), "Get Payment Details for CY-2"),
        ("API#1687 Individual Tax Credits", Some("API#1687"), "Individual Tax Credits"),
        ("API-1703 Retrieve debt details", Some("API-1703"), "Retrieve debt details"),
        ("dct102d - Sends request ID value for Subscription Display - API", Some("dct102d"), "Sends request ID value for Subscription Display - API"),
        ("DCT59J    EIS API Integration (Sync)", Some("DCT59J"), "EIS API Integration (Sync)"),
        ("acc1 - one number is ok", Some("acc1"), "one number is ok"),
      )
      when(appConfig.publishApiNumberIgnoreList).thenReturn(Set.empty)
      
      forAll(testCases) { (title, expectedApiNumber, expectedTitle) =>
        val originalApiDetail = apiDetail0.copy(title = title)
        val updatedApiDetail = apiNumberExtractor.extract(originalApiDetail)
        val expectedApiDetail = originalApiDetail.copy(apiNumber = expectedApiNumber, title = expectedTitle)
        updatedApiDetail shouldBe expectedApiDetail
      }
    }

    "does not extract anything from title if no valid API number found" in {
      val testCases = Table(
        "title",
        "Some API without a number",
        "AB123 - does not match number format",
        "ABCDE123 - does not match number format",
        "API#123-need_a_space_somewhere"
      )
      when(appConfig.publishApiNumberIgnoreList).thenReturn(Set.empty)

      forAll(testCases) { (title) =>
        val originalApiDetail = apiDetail0.copy(title = title)
        val updatedApiDetail = apiNumberExtractor.extract(originalApiDetail)
        updatedApiDetail shouldBe originalApiDetail
      }
    }

    "does not extract API numbers that are present on the ignore list" in {
      val ignore1 = "IGN#001"
      val ignore2 = "ign-002"
      
      val testCases = Table(
        ("title", "expectedApiNumber", "expectedTitle"),
        (s"$ignore1 - ignore this api number", None, s"$ignore1 - ignore this api number"),
        (s"$ignore2 ignore this api number", None, s"$ignore2 ignore this api number"),
        (s"${ignore1}0 - dont ignore this api number", Some(s"${ignore1}0"), "dont ignore this api number"),
        (s"${ignore1.toLowerCase()} - dont ignore this api number", Some(ignore1.toLowerCase()), "dont ignore this api number"),
      )
      when(appConfig.publishApiNumberIgnoreList).thenReturn(Set.from(List(ignore1, ignore2)))

      forAll(testCases) { (title, expectedApiNumber, expectedTitle) =>
        val originalApiDetail = apiDetail0.copy(title = title)
        val updatedApiDetail = apiNumberExtractor.extract(originalApiDetail)
        val expectedApiDetail = originalApiDetail.copy(apiNumber = expectedApiNumber, title = expectedTitle)
        updatedApiDetail shouldBe expectedApiDetail
      }
    }
  }
}