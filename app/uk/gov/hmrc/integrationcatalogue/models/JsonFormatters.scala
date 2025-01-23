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

package uk.gov.hmrc.integrationcatalogue.models

import play.api.libs.json._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.utils.DateTimeFormatters

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}

object JsonFormatters extends DateTimeFormatters {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  val zonedDateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat)
  implicit val instantWrites: Writes[Instant] = new Writes[Instant] {
    override def writes(i: Instant): JsValue = JsString(ZonedDateTime.ofInstant(i, ZoneId.of("Z")).format(zonedDateTimeFormatter))
  }
  implicit val instantReads: Reads[Instant] = new Reads[Instant] {
    override def reads(json: JsValue): JsResult[Instant] =
      json match {
        case JsString(s) => parseDate(s) match {
          case Some(i: Instant) => JsSuccess(i)
          case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError(s"Could not interpret date[/time] as one of the supported ISO formats: $json"))))
        }
        case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError(s"Could not interpret date[/time] as one of the supported ISO formats: $json"))))
      }

    private def parseDate(input: String): Option[Instant] =
      scala.util.control.Exception.nonFatalCatch[Instant].opt(Instant.from(ZonedDateTime.parse(input).toInstant)) orElse
        scala.util.control.Exception.nonFatalCatch[Instant].opt(Instant.from(dateAndOptionalTimeFormatter.parse(input)))
  }

  implicit val formatContactInformation: Format[ContactInformation] = Json.format[ContactInformation]

  implicit val formatMaintainer: Format[Maintainer] = Json.format[Maintainer]

  implicit val formatIntegrationDetail: OFormat[IntegrationDetail] = Json.format[IntegrationDetail]

  implicit val formatExample: OFormat[Example] = Json.format[Example]

  implicit val formatStringAttributes: Format[StringAttributes] = Json.format[StringAttributes]
  implicit val formatNumberAttributes: Format[NumberAttributes] = Json.format[NumberAttributes]

  implicit val formatSchema: OFormat[Schema]                = Json.format[Schema]
  implicit val formatDefaultSchema: Format[DefaultSchema]   = Json.format[DefaultSchema]
  implicit val formatComposedSchema: Format[ComposedSchema] = Json.format[ComposedSchema]
  implicit val formatArraySchema: Format[ArraySchema]       = Json.format[ArraySchema]
  implicit val formatHeader: Format[Header]                 = Json.format[Header]
  implicit val formatParameter: Format[Parameter]           = Json.format[Parameter]

  implicit val formatRequest: OFormat[Request]    = Json.format[Request]
  implicit val formatsResponse: OFormat[Response] = Json.format[Response]

  implicit val endpointMethodFormats: OFormat[EndpointMethod] = Json.format[EndpointMethod]

  implicit val endpointFormats: OFormat[Endpoint] = Json.format[Endpoint]

  implicit val formatScope: Format[Scope] = Json.format[Scope]

  implicit val formatApiDetailParsed: Format[ApiDetail] = Json.format[ApiDetail]

  implicit val formatFileTransferDetail: Format[FileTransferDetail] = Json.format[FileTransferDetail]

  implicit val formatPublishRequest: Format[PublishRequest] = Json.format[PublishRequest]

  implicit val formatFileTransferPublishRequest: Format[FileTransferPublishRequest] = Json.format[FileTransferPublishRequest]

  implicit val formatPublishError: Format[PublishError] = Json.format[PublishError]

  implicit val formatPublishDetails: Format[PublishDetails] = Json.format[PublishDetails]

  implicit val formatMultipartPublishResponse: Format[MultipartPublishResponse] = Json.format[MultipartPublishResponse]

  implicit val formatPublishResult: Format[PublishResult] = Json.format[PublishResult]

  implicit val formatIntegrationResponse: Format[IntegrationResponse] = Json.format[IntegrationResponse]

  implicit val formatDeleteIntegrationsResponse: Format[DeleteIntegrationsResponse] = Json.format[DeleteIntegrationsResponse]

  implicit val formatErrorResponseMessage: Format[ErrorResponseMessage] = Json.format[ErrorResponseMessage]
  implicit val formatErrorResponse: OFormat[ErrorResponse]               = Json.format[ErrorResponse]

  implicit val formatPlatformContactsResponse: Format[PlatformContactResponse] = Json.format[PlatformContactResponse]

  implicit val integrationPlatformReportFormats: OFormat[IntegrationPlatformReport]               = Json.format[IntegrationPlatformReport]
  implicit val formatFileTransferTransportsByPlatform: OFormat[FileTransferTransportsForPlatform] = Json.format[FileTransferTransportsForPlatform]

}
