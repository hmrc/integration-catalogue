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

package uk.gov.hmrc.integrationcatalogue.testdata

import uk.gov.hmrc.integrationcatalogue.config.{ApiNumbering, PlatformNumbering}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.HIP

object FakeApiNumbering extends ApiNumbering {

  val hip: PlatformNumbering = PlatformNumbering(
    platformType = HIP,
    start = 5000
  )

  override val prefix: String = "API#"

  override val platforms: Seq[PlatformNumbering] = Seq(hip)

}
