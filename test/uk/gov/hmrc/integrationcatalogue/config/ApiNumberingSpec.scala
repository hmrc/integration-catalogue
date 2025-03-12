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

package uk.gov.hmrc.integrationcatalogue.config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{CMA, HIP}

class ApiNumberingSpec extends AnyFreeSpec with Matchers {

  "ApiNumberingImpl" - {
    "must load the configuration correctly" in {
      val prefix = "API#"
      val hipStart = 5000
      val cmaStart = 101

      val configuration = Configuration.from(
        Map(
          "apiNumbering.prefix" -> prefix,
          "apiNumbering.platforms.hip.platform" -> HIP.entryName,
          "apiNumbering.platforms.hip.start" -> hipStart,
          "apiNumbering.platforms.cma.platform" -> CMA.entryName,
          "apiNumbering.platforms.cma.start" -> cmaStart
        )
      )

      val expectedPlatforms = Seq(
        PlatformNumbering(
          platformType = HIP,
          start = hipStart
        ),
        PlatformNumbering(
          platformType = CMA,
          start = cmaStart
        )
      )

      val apiNumbering = new ApiNumberingImpl(configuration)

      apiNumbering.prefix mustBe prefix
      apiNumbering.platforms must contain theSameElementsAs expectedPlatforms
    }
  }

}
