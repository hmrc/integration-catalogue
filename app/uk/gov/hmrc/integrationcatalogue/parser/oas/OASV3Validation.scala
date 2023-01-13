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

package uk.gov.hmrc.integrationcatalogue.parser.oas

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.implicits._
import io.swagger.v3.oas.models.info.Info

trait OASV3Validation {

  def validateInfo(info: Info): ValidatedNel[String, List[Info]] = {

    def validateStringItem(info: Info, validationFunc: Info => String, errorMessage: String): ValidatedNel[String, Info] = {
      Option(validationFunc.apply(info)) match {
        case None => errorMessage.invalidNel[Info]
        case _    => Valid(info)
      }
    }

    val titleValidation   = validateStringItem(info, x => x.getTitle, "Invalid OAS, title missing from OAS specification")
    val versionValidation = validateStringItem(info, x => x.getVersion, "Invalid OAS, version missing from OAS specification")
    List(titleValidation, versionValidation).sequence
  }

}
