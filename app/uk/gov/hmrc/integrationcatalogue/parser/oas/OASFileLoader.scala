/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.parser.oas

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions

// Turned off coverage for this as it was just one line and no need to test OpenApiV3Parser code as it is not
// ours. However if this ever expands in future, tests will be needed and scoverage ignore removed.
// $COVERAGE-OFF$
class OASFileLoader {
  def parseOasSpec(fileContents: String) = {

    val options : ParseOptions = new ParseOptions()

    options.setResolve(false)
    new OpenAPIV3Parser().readContents(fileContents, null, options)
  }
}
// $COVERAGE-ON$
