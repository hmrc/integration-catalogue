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

package uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders

import cats.data.Validated._
import cats.data._
import cats.implicits._
import play.api.mvc.{Headers, Request}
import uk.gov.hmrc.integrationcatalogue.models.common.{PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponseMessage, ExtractedHeaders, HeaderKeys}

import javax.inject.Singleton

@Singleton
class PublishHeaderValidator {
  type ValidationResult[A] = ValidatedNel[ErrorResponseMessage, A]

  def validateHeaders[A](request: Request[A]): ValidationResult[ExtractedHeaders] = {
    (validatePublisherReference(request), validatePlatformType(request), validateSpecificationType(request))
      .mapN(ExtractedHeaders.apply)
  }

  private def validatePlatformType[A](request: Request[A]): ValidationResult[PlatformType] = {
    validateHeaderItem[PlatformType](
      HeaderKeys.platformKey,
      "platform type header is missing or invalid",
      x => PlatformType.values.map(_.toString()).contains(x.toUpperCase),
      x => PlatformType.withNameInsensitive(x),
      request.headers
    )
  }

  private def validateSpecificationType[A](request: Request[A]): ValidationResult[SpecificationType] = {
    validateHeaderItem[SpecificationType](
      HeaderKeys.specificationTypeKey,
      "specification type header is missing or invalid",
      x => SpecificationType.values.map(_.toString()).contains(x.toUpperCase),
      x => SpecificationType.withNameInsensitive(x),
      request.headers
    )
  }

  private def validatePublisherReference[A](request: Request[A]): ValidationResult[Option[String]] = {
    request.headers.get(HeaderKeys.publisherRefKey).validNel
  }

  private def validateHeaderItem[A](
      headerKey: String,
      errorMessageStr: String,
      validateFunc: String => Boolean,
      extractFunc: String => A,
      headers: Headers
    ): ValidationResult[A] = {
    val errorMessage = ErrorResponseMessage(errorMessageStr)
    val headerString = headers.get(headerKey).getOrElse("")
    if (validateFunc.apply(headerString)) extractFunc.apply(headerString).validNel else errorMessage.invalidNel
  }

}
