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

package uk.gov.hmrc.integrationcatalogue.repository

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.*

import java.time.Instant
import scala.quoted.*

object MongoFormatters {
  implicit val instantReads: Reads[Instant] =
    Reads.at[String](__ \ "$date" \ "$numberLong")
      .map(instant => Instant.ofEpochMilli(instant.toLong))

  implicit val instantWrites: Writes[Instant] =
    Writes.at[String](__ \ "$date" \ "$numberLong")
      .contramap[Instant](_.toEpochMilli.toString)

  implicit val integrationIdFormatter: Format[IntegrationId] = Json.valueFormat[IntegrationId]
  implicit val instantFormats: Format[Instant] = Format(instantReads, instantWrites)

  implicit val contactInformationFormats: OFormat[ContactInformation] = Json.format[ContactInformation]
  implicit val maintainerFormats: OFormat[Maintainer]                 = Json.format[Maintainer]
  implicit val exampleFormats: OFormat[Example]                       = Json.format[Example]

  implicit val stringattributesFormats: OFormat[StringAttributes] = Json.format[StringAttributes]
  implicit val numberattributesFormats: OFormat[NumberAttributes] = Json.format[NumberAttributes]
  implicit val schemaFormats: OFormat[Schema]                     = Json.format[Schema]
  implicit val defaultSchemaFormats: Format[DefaultSchema]        = Json.format[DefaultSchema]
  implicit val composedSchemaFormats: Format[ComposedSchema]      = Json.format[ComposedSchema]
  implicit val arraySchemaFormats: Format[ArraySchema]            = Json.format[ArraySchema]
  implicit val headerFormats: Format[Header]                      = Json.format[Header]
  implicit val parameterFormats: Format[Parameter]                = Json.format[Parameter]

  implicit val requestFormats: OFormat[Request]   = Json.format[Request]
  implicit val responseFormats: OFormat[Response] = Json.format[Response]

  private val endpointMethodReads: Reads[EndpointMethod] = (
    (JsPath \ "httpMethod").read[String] and
      (JsPath \ "summary").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "scopes").readWithDefault[List[String]](List.empty)
    )(EndpointMethod.apply)


  private val endpointMethodWrites: Writes[EndpointMethod] = Json.writes[EndpointMethod]
  implicit val endpointMethodFormats: Format[EndpointMethod] = Format(endpointMethodReads, endpointMethodWrites)

  implicit val endpointFormats: OFormat[Endpoint]             = Json.format[Endpoint]

  implicit val scopeFormat: Format[Scope] = Json.format[Scope]

  private val apiDetailReads: Reads[ApiDetail] = (
    (JsPath \ "id").read[IntegrationId] and
      (JsPath \ "publisherReference").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "platform").read[PlatformType] and
      (JsPath \ "hods").read[List[String]] and
      (JsPath \ "lastUpdated").read[Instant] and
      (JsPath \ "reviewedDate").read[Instant] and
      (JsPath \ "maintainer").read[Maintainer] and
      (JsPath \ "score").readNullable[Double] and
      (JsPath \ "version").read[String] and
      (JsPath \ "specificationType").read[SpecificationType] and
      (JsPath \ "endpoints").read[List[Endpoint]] and
      (JsPath \ "shortDescription").readNullable[String] and
      (JsPath \ "openApiSpecification").read[String] and
      (JsPath \ "apiStatus").read[ApiStatus] and
      (JsPath \ "scopes").readWithDefault[Set[Scope]](Set.empty) and
      (JsPath \ "teamId").readNullable[String] and
      (JsPath \ "domain").readNullable[String] and
      (JsPath \ "subDomain").readNullable[String] and
      (JsPath \ "apiType").readNullable[ApiType] and
      (JsPath \ "apiNumber").readNullable[String]
    )(ApiDetail.apply)

  private val apiDetailWrites: Writes[ApiDetail] = Json.writes[ApiDetail]
  implicit val apiDetailParsedFormats: Format[ApiDetail] = Format(apiDetailReads, apiDetailWrites)

  implicit val integrationDetailFormats: OFormat[IntegrationDetail]                           = Json.format[IntegrationDetail]

  implicit val fileTransferDetailFormats: OFormat[FileTransferDetail]                         = Json.format[FileTransferDetail]
  implicit val integrationCountFormats: OFormat[IntegrationCount]                             = Json.format[IntegrationCount]
  implicit val integrationCountResponseFormats: OFormat[IntegrationCountResponse]             = Json.format[IntegrationCountResponse]
  implicit val fileTransferPlatformFormats: OFormat[FileTransferPlatform]                     = Json.format[FileTransferPlatform]
  implicit val fileTransferTransportsResponseFormats: OFormat[FileTransferTransportsResponse] = Json.format[FileTransferTransportsResponse]

}
