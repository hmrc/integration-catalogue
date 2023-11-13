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

package uk.gov.hmrc.integrationcatalogue.controllers

import play.api.Logging
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.{ValidateApiPublishRequestAction, ValidateAuthorizationHeaderAction}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.service.PublishService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class MultipartPublishController @Inject()(
    cc: ControllerComponents,
    publishService: PublishService,
    validateApiPublishRequest: ValidateApiPublishRequestAction,
    validateAuthorizationHeaderAction: ValidateAuthorizationHeaderAction,
    playBodyParsers: PlayBodyParsers
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc)
    with Logging {

  def publishApi(): Action[MultipartFormData[Files.TemporaryFile]] = (Action andThen
    validateAuthorizationHeaderAction andThen
    validateApiPublishRequest).async(playBodyParsers.multipartFormData) {
    implicit request: ValidatedApiPublishRequest[MultipartFormData[Files.TemporaryFile]] =>
      (request.body.file("selectedFile"), request.body.dataParts.get("selectedFile")) match {
        case (Some(selectedFile), _)              =>
          val bufferedSource = Source.fromFile(selectedFile.ref.path.toFile)
          val fileContents   = bufferedSource.getLines().mkString("\r\n")
          bufferedSource.close()
          publishService.publishApi(PublishRequest(request.publisherReference, request.platformType, request.specificationType, fileContents))
            .map(handleMultipartPublishResult)
        case (None, Some(dataParts: Seq[String])) =>
          dataParts match {
            case Nil                        =>
              logger.info("selectedFile is missing from requestBody")
              Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("selectedFile is missing from requestBody"))))))
            case oasStringList: Seq[String] =>
              publishService.publishApi(PublishRequest(request.publisherReference, request.platformType, request.specificationType, oasStringList.head))
                .map(handleMultipartPublishResult)
          }
        case (_, _)                               =>
          logger.info("selectedFile is missing from requestBody")
          Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("selectedFile is missing from requestBody"))))))
      }
  }

  private def handleMultipartPublishResult(publishResult: PublishResult) = {
    publishResult.publishDetails match {
      case Some(details) =>
        val resultAsJson = Json.toJson(PublishDetails.toMultipartPublishResponse(details))
        if (details.isUpdate) Ok(resultAsJson) else Created(resultAsJson)
      case None          => if (publishResult.errors.nonEmpty) {
          BadRequest(Json.toJson(ErrorResponse(publishResult.errors.map(x => ErrorResponseMessage(x.message)))))
        } else {
          BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Failed to publish API: Unexpected error")))))
        }
    }
  }
}
