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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc._
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.IdentifierAction
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.{ApiTeam, ErrorResponse, ErrorResponseMessage, FileTransferPublishRequest, PublishRequest}
import uk.gov.hmrc.integrationcatalogue.service.PublishService

@Singleton
class PublishController @Inject() (
    cc: ControllerComponents,
    publishService: PublishService,
    identify: IdentifierAction
  )(implicit ec: ExecutionContext
  ) extends BackendController(cc)
    with Logging {

  def publishFileTransfer(): Action[JsValue] = identify.async(parse.tolerantJson) { implicit request =>
    if (validateJsonString[FileTransferPublishRequest](request.body.toString())) {
      val bodyVal = request.body.as[FileTransferPublishRequest]
      for {
        publishResult <- publishService.publishFileTransfer(bodyVal)
      } yield Ok(Json.toJson(publishResult))
    } else {
      logger.warn("Invalid request body, must be a valid publish request")
      Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body"))))))
    }
  }

  def publishApi(): Action[JsValue] = identify.async(parse.tolerantJson) { implicit request =>
    if (validateJsonString[PublishRequest](request.body.toString())) {
      val bodyVal = request.body.as[PublishRequest]
      for {
        publishResult <- publishService.publishApi(bodyVal)
      } yield Ok(Json.toJson(publishResult))
    } else {
      logger.warn("Invalid request body, must be a valid publish request")
      Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body"))))))
    }
  }

  def linkApiToTeam(): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[ApiTeam].fold(
        invalid => {
          logger.warn(s"Error parsing request body: ${JsError.toJson(invalid)}")
          Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body"))))))
        },
        apiTeam =>
          publishService.linkApiToTeam(apiTeam).map(_ => NoContent)
      )
  }

  private def validateJsonString[T](body: String)(implicit reads: Reads[T]) = {
    validateJson[T](body, body => Json.parse(body))
  }

  private def validateJson[T](body: String, f: String => JsValue)(implicit reads: Reads[T]): Boolean = {
    Try[T] {
      f(body).as[T]
    } match {
      case Success(_) => true
      case Failure(_) => false
    }
  }
}
