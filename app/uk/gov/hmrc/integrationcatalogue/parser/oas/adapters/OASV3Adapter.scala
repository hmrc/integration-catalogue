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

package uk.gov.hmrc.integrationcatalogue.parser.oas.adapters

import cats.data.Validated._
import cats.data._
import cats.implicits._
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.PathItem.HttpMethod
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem}
import org.joda.time.DateTime
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASV3Validation
import uk.gov.hmrc.integrationcatalogue.service.{AcronymHelper, UuidService}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.LinkedHashSet
import scala.jdk.CollectionConverters._

@Singleton
class OASV3Adapter @Inject() (uuidService: UuidService, appConfig: AppConfig)
    extends Logging with AcronymHelper with OASV3Validation with OASExtensionsAdapter
    with OASV3SchemaAdapter with OASV3HeaderAdapter with OASV3ParameterAdapter {

  def extractOpenApi(
      publisherRef: Option[String],
      platformType: PlatformType,
      specType: SpecificationType,
      openApi: OpenAPI,
      openApiSpecificationContent: String
    ): ValidatedNel[List[String], ApiDetail] = {

    Option(openApi.getInfo) match {
      case Some(info) =>
        validateInfo(info) match {
          case Invalid(errors) => errors.toList.invalidNel[ApiDetail]
          case Valid(_)        =>
            val mayBePaths   = Option(openApi.getPaths)
            val pathNames    = mayBePaths.map(_.keySet().asScala.to(LinkedHashSet).toList).getOrElse(List.empty)
            val allEndpoints = pathNames.flatMap(pathName => {
              mayBePaths.map(path => {
                val pathItem = path.get(pathName)
                extractEndpoints(pathName, pathItem)
              }).getOrElse(List.empty)
            })

            // What to do about errors parsing extensions????
            parseExtensions(info, publisherRef, appConfig) match {
              case Right(extensions: IntegrationCatalogueExtensions) =>
                val hods                  = extensions.backends
                val status                = extensions.status
                val extensionReviewedDate = extensions.reviewedDate

                Valid(ApiDetail(
                  id = IntegrationId(uuidService.newUuid()),
                  publisherReference = extensions.publisherReference,
                  title = getStringSafe(info.getTitle),
                  description = getStringSafe(info.getDescription),
                  version = getStringSafe(info.getVersion),
                  lastUpdated = DateTime.now,
                  endpoints = allEndpoints,
                  maintainer = extractMaintainer(info.getContact),
                  specificationType = specType,
                  platform = platformType,
                  hods = hods.toList,
                  shortDescription = extensions.shortDescription,
                  openApiSpecification = openApiSpecificationContent,
                  apiStatus = status,
                  reviewedDate = extensionReviewedDate
                ))
              case Left(x)                                           => x.toList.invalidNel[ApiDetail]
            }
        }
      case None       => List("Invalid OAS, info item missing from OAS specification").invalidNel[ApiDetail]
    }

  }

  private def getStringSafe(value: java.lang.String): String = {
    Option(value).getOrElse("")
  }

  private def extractMaintainer(contact: Contact) =
    Maintainer(name = "", slackChannel = "", contactInfo = extractContact(contact).map(List(_)).getOrElse(List.empty))

  private def extractContact(contact: Contact): Option[ContactInformation] = {
    Option(contact).map(x => ContactInformation(handleNullAsString(Option(x.getName)), handleNullAsString(Option(x.getEmail))))
  }

  private def handleNullAsString(value: Option[String]) = {
    value match {
      case Some("null") => None
      case _            => value
    }
  }

  private def extractEndpoints(path: String, item: PathItem): List[Endpoint] = {
    val endpointMethods = item.readOperationsMap().asScala.toMap
      .map {
        case (m: HttpMethod, operation: Operation) =>
          val method              = Option(m).map(_.toString).getOrElse("")

          EndpointMethod(
            httpMethod = method,
            summary = Option(operation).flatMap(x => Option(x.getSummary)),
            description = Option(operation.getDescription)
          )
      }.toList
    List(Endpoint(path, endpointMethods))
  }

  def getExampleText(maybeObject: Option[Object]): String = {
    maybeObject.map { (o: Object) =>
      {
        o match {
          case js: JsonNode => js.toPrettyString
          case x: Object    => x.toString
        }
      }
    }.getOrElse("")
  }

}
