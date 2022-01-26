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

package uk.gov.hmrc.integrationcatalogue.service

import cats.data.Validated._
import cats.data._
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.controllers.ErrorCodes._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASParserService
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PublishService  @Inject()(oasParser: OASParserService, integrationRepository: IntegrationRepository, uuidService: UuidService) extends Logging{

  def publishFileTransfer(request: FileTransferPublishRequest)(implicit ec: ExecutionContext) : Future[PublishResult] = {
    val integrationId = IntegrationId(uuidService.newUuid())

    val fileTransferDetail = FileTransferDetail.fromFileTransferPublishRequest(request, integrationId)

    integrationRepository.findAndModify(fileTransferDetail).flatMap {
      case Right((fileTransfer, isUpdate)) =>
        Future.successful(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate, fileTransfer.id, fileTransfer.publisherReference, fileTransfer.platform))))
      case Left(error) =>
        Future.successful(PublishResult(isSuccess = false, errors = List(PublishError(API_UPSERT_ERROR, "Unable to upsert file transfer"))))
    }
  }

    def publishApi(request: PublishRequest)(implicit ec: ExecutionContext) : Future[PublishResult] = {

        val parseResult: ValidatedNel[List[String], ApiDetail] =
          oasParser.parse(request.publisherReference, request.platformType, request.specificationType, request.contents)


        parseResult match {
          case x : Invalid[NonEmptyList[List[String]]] => mapErrorsToPublishResult(x)
          case Valid(apiDetailParsed) =>
          //TODO: validate parsed api model
            integrationRepository.findAndModify(apiDetailParsed).flatMap {
              case Right((api, isUpdate)) =>
                Future.successful(PublishResult(isSuccess = true, Some(PublishDetails(isUpdate, api.id, api.publisherReference, api.platform))))
              case Left(error) =>
                Future.successful(PublishResult(isSuccess = false, errors = List(PublishError(API_UPSERT_ERROR, "Unable to upsert api"))))
            }
        }
    }

    def mapErrorsToPublishResult(invalidNel: Invalid[NonEmptyList[List[String]]]): Future[PublishResult] = {
      val flatList = invalidNel.e.toList.flatten
      Future.successful(PublishResult(isSuccess = false, None, flatList.map(err => PublishError(OAS_PARSE_ERROR, err))))
    }
  
}
