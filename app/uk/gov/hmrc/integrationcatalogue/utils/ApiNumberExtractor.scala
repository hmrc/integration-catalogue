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

import com.google.inject.Singleton
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.ApiDetail

import javax.inject.Inject

@Singleton
class ApiNumberExtractor @Inject(appConfig: AppConfig) {
  private val threeOrFourLetters = "[a-zA-Z]{3,4}"
  private val optionallyAHashOrHyphen = "[-#]?"
  private val atLeastOneDigit = "[0-9]+"
  private val zeroOrMoreNonSpaceCharacters = "[^ ]*"
  private val atLeastOneSpaceAndMaybeAHyphen = " +-? *"
  private val everythingElse = ".*$"
  
  private val apiNumberGroup = threeOrFourLetters + optionallyAHashOrHyphen + atLeastOneDigit + zeroOrMoreNonSpaceCharacters
  private val separator = atLeastOneSpaceAndMaybeAHyphen
  private val apiTitleGroup = everythingElse
  
  private val apiNumberRegex = s"^($apiNumberGroup)$separator($apiTitleGroup)".r
  
  def extract(apiDetail: ApiDetail) = {
    apiNumberRegex.findFirstMatchIn(apiDetail.title) match
      case Some(m) => {
        val apiNumber = m.group(1)
        val apiTitleWithoutNumber = m.group(2)
        if (isApiNumberInIgnoreList(apiNumber)) {
          apiDetail
        } else {
          apiDetail.copy(apiNumber = Some(apiNumber), title = apiTitleWithoutNumber)
        }
      }
      case _ => apiDetail
  }
  
  private def isApiNumberInIgnoreList(apiNumber: String) = {
    appConfig.publishApiNumberIgnoreList.contains(apiNumber)
  }
}
