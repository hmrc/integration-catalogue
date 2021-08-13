/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType, SpecificationType, ContactInformation}
import org.joda.time.DateTime
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType

case class IntegrationResponse(count: Int, pagedCount: Option[Int] = None, results: List[IntegrationDetail])

case class PublishRequest(publisherReference: Option[String], platformType: PlatformType, specificationType: SpecificationType, contents: String)

case class FileTransferPublishRequest(
                              fileTransferSpecificationVersion: String, // Set to 0.1?
                              publisherReference: String,
                              title: String,
                              description: String,
                              platformType: PlatformType, // Split this to Platform and type. TBD
                              lastUpdated: DateTime,
                              reviewedDate: DateTime,
                              contact: ContactInformation, // (single name + email)
                              sourceSystem: List[String], // One or many
                              targetSystem: List[String],
                              transports: List[String],
                              fileTransferPattern: String)

// TODO : Move me
case class IntegrationFilter(searchText: List[String] = List.empty, platforms: List[PlatformType] = List.empty, backends: List[String] = List.empty, itemsPerPage: Option[Int] = None, currentPage: Option[Int] = None, typeFilter: Option[IntegrationType] = None)

//TODO remove code from PublishError
case class PublishError(code: Int, message: String)

case class PublishDetails(isUpdate: Types.IsUpdate, integrationId: IntegrationId, publisherReference: String, platformType: PlatformType)

case class PublishResult(isSuccess: Boolean, publishDetails: Option[PublishDetails] = None, errors: List[PublishError] = List.empty)

sealed trait DeleteApiResult

case object NotFoundDeleteApiResult extends DeleteApiResult
case object NoContentDeleteApiResult extends DeleteApiResult


case class ErrorResponseMessage(message: String)
case class ErrorResponse(errors: List[ErrorResponseMessage])

case class DeleteIntegrationsResponse(numberOfIntegrationsDeleted: Int)


case class PlatformContactResponse(platformType: PlatformType, contactInfo: Option[ContactInformation])

case class IntegrationCountResponse(platform: PlatformType, count: Int)