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

import cats.data.Validated._
import cats.data._
import cats.implicits._
import io.swagger.v3.oas.models.OpenAPI
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.models.ApiDetail
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter

import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class OASParserService @Inject() (oasParser: OASFileLoader, oasV3Service: OASV3Adapter) extends Logging {

  def parse(
      publisherReference: Option[String],
      platformType: PlatformType,
      specificationType: SpecificationType,
      fileContents: String,
      autopublish: Boolean,
    ): ValidatedNel[List[String], ApiDetail] = {

    val maybeSwaggerParseResult = Option(oasParser.parseOasSpec(fileContents))

    def getListSafe(list: java.util.List[String]): List[String] = {
      Option(list)
        .map(e => e.asScala.toList)
        .getOrElse(List.empty)
    }

    // $COVERAGE-OFF$
    def logAnyWarnings(messages: List[String]): Unit = {
      if (messages.nonEmpty) {
        logger.warn(s"Warnings parsing for platform $platformType OAS:\n${messages.mkString("\n")}")
      }
    }
    // $COVERAGE-ON$

    val oasSpecification = maybeSwaggerParseResult
      .map(swaggerParseResult => {
        val eitherOpenApi: ValidatedNel[List[String], OpenAPI] =
          (Option(swaggerParseResult.getOpenAPI), getListSafe(swaggerParseResult.getMessages)) match {
            case (Some(openApi), messages)         =>
              logAnyWarnings(messages)
              Validated.valid(openApi)
            case (None, errors) if errors.nonEmpty => Invalid(NonEmptyList.of(errors))
            case _                                 => List(s"Error loading OAS specification for platform $platformType OAS").invalidNel[OpenAPI]
          }
        eitherOpenApi
      }).getOrElse(List(s"Oas Parser returned null").invalidNel[OpenAPI])

    oasSpecification match {
      case Valid(openApi)                         => handleOpenApi(publisherReference, platformType, specificationType, openApi, fileContents, autopublish)
      case e: Invalid[NonEmptyList[List[String]]] => e
    }

  }

  private def handleOpenApi(
      publisherReference: Option[String],
      platformType: PlatformType,
      specificationType: SpecificationType,
      openApi: OpenAPI,
      fileContents: String,
      autopublish: Boolean,
    ): ValidatedNel[List[String], ApiDetail] = {

    openApi.getOpenapi.headOption match {
      case Some('3') => oasV3Service.extractOpenApi(publisherReference, platformType, specificationType, openApi, fileContents, autopublish)
      case _         => List(s"Unhandled OAS specification version for platform $platformType OAS").invalidNel[ApiDetail]
    }
  }
}
