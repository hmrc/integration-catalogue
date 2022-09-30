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

package uk.gov.hmrc.integrationcatalogue.repository

import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import play.api.libs.json._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._

object MongoFormatters extends MongoJodaFormats {
  implicit val integrationIdFormatter: Format[IntegrationId] = Json.valueFormat[IntegrationId]
  implicit val dateTimeFormats                               = MongoJodaFormats.dateTimeFormat

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
  implicit val componentsFormats: Format[Components]              = Json.format[Components]

  implicit val requestFormats: OFormat[Request]   = Json.format[Request]
  implicit val responseFormats: OFormat[Response] = Json.format[Response]

  implicit val endpointMethodFormats: OFormat[EndpointMethod] = Json.format[EndpointMethod]
  implicit val endpointFormats: OFormat[Endpoint]             = Json.format[Endpoint]

  implicit val integrationDetailFormats: OFormat[IntegrationDetail]                           = Json.format[IntegrationDetail]
  implicit val apiDetailParsedFormats: OFormat[ApiDetail]                                     = Json.format[ApiDetail]
  implicit val fileTransferDetailFormats: OFormat[FileTransferDetail]                         = Json.format[FileTransferDetail]
  implicit val integrationCountFormats: OFormat[IntegrationCount]                             = Json.format[IntegrationCount]
  implicit val integrationCountResponseFormats: OFormat[IntegrationCountResponse]             = Json.format[IntegrationCountResponse]
  implicit val fileTransferPlatformFormats: OFormat[FileTransferPlatform]                     = Json.format[FileTransferPlatform]
  implicit val fileTransferTransportsResponseFormats: OFormat[FileTransferTransportsResponse] = Json.format[FileTransferTransportsResponse]

}
