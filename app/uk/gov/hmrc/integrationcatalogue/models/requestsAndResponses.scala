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

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes
import uk.gov.hmrc.integrationcatalogue.models.common._

import java.time.Instant

case class IntegrationResponse(count: Int, pagedCount: Option[Int] = None, results: List[IntegrationDetail])

case class PublishRequest(
  publisherReference: Option[String],
  platformType: PlatformType,
  specificationType: SpecificationType,
  contents: String,
  autopublish: Boolean
)

object PublishRequest {

  def apply(
    publisherReference: Option[String],
    platformType: PlatformType,
    specificationType: SpecificationType,
    contents: String,
  ): PublishRequest = {
    PublishRequest(publisherReference, platformType, specificationType, contents, autopublish = false)
  }

}

case class FileTransferPublishRequest(
    fileTransferSpecificationVersion: String, // Set to 0.1?
    publisherReference: String,
    title: String,
    description: String,
    platformType: PlatformType,               // Split this to Platform and type. TBD
    lastUpdated: Instant,
    reviewedDate: Instant,
    contact: ContactInformation,              // (single name + email)
    sourceSystem: List[String],               // One or many
    targetSystem: List[String],
    transports: List[String],
    fileTransferPattern: String
  )

case class IntegrationFilter(
    searchText: List[String] = List.empty,
    platforms: List[PlatformType] = List.empty,
    backends: List[String] = List.empty,
    itemsPerPage: Option[Int] = None,
    currentPage: Option[Int] = None,
    typeFilter: Option[IntegrationType] = None,
    teamIds: List[String] = List.empty
  )

case class PublishError(code: Int, message: String)

object PublishError {

  def missingTeamLink(): PublishError = {
    PublishError(ErrorCodes.MISSING_TEAM_LINK, "A team link must exist when auto-publishing")
  }

}

case class PublishDetails(isUpdate: Types.IsUpdate, integrationId: IntegrationId, publisherReference: String, platformType: PlatformType)

object PublishDetails {

  def toMultipartPublishResponse(details: PublishDetails): MultipartPublishResponse = {
    MultipartPublishResponse(details.integrationId, details.publisherReference, details.platformType)
  }
}

case class MultipartPublishResponse(id: IntegrationId, publisherReference: String, platformType: PlatformType)
case class PublishResult(isSuccess: Boolean, publishDetails: Option[PublishDetails] = None, errors: List[PublishError] = List.empty)

sealed trait DeleteApiResult
case object NotFoundDeleteApiResult  extends DeleteApiResult
case object NoContentDeleteApiResult extends DeleteApiResult

case class ErrorResponseMessage(message: String)
case class ErrorResponse(errors: List[ErrorResponseMessage])

case class DeleteIntegrationsResponse(numberOfIntegrationsDeleted: Int)

case class PlatformContactResponse(platformType: PlatformType, contactInfo: Option[ContactInformation], overrideOasContacts: Boolean)

case class IntegrationCount(platform: PlatformType, integrationType: String)
case class IntegrationCountResponse(_id: IntegrationCount, count: Int)

case class IntegrationPlatformReport(platformType: PlatformType, integrationType: IntegrationType, count: Int)

case class FileTransferPlatform(platform: PlatformType)
case class FileTransferTransportsResponse(_id: FileTransferPlatform, transports: List[String])
case class FileTransferTransportsForPlatform(platform: PlatformType, transports: List[String])

case class ExtractedHeaders(publisherReference: Option[String], platformType: PlatformType, specificationType: SpecificationType)

case class ValidatedApiPublishRequest[A](
  publisherReference: Option[String],
  platformType: PlatformType,
  specificationType: SpecificationType,
  request: Request[A]
) extends WrappedRequest[A](request)
