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

package uk.gov.hmrc.integrationcatalogue.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.service.IntegrationService
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class IntegrationController @Inject()(controllerComponents: ControllerComponents,
                                      integrationService: IntegrationService,
                                      validateQueryParamKeyAction: ValidateQueryParamKeyAction)
                                     (implicit val ec: ExecutionContext)
    extends BackendController(controllerComponents)
    with Logging {

  def findById(id: IntegrationId): Action[AnyContent] = Action.async {
     integrationService.findById(id).map{
       case Some(result) => Ok(Json.toJson(result))
       case None =>
       logger.info(s"Integration not found: $id")
       NotFound
     }
  }

 def findWithFilters(searchTerm: List[String],
                     platformFilter: List[PlatformType],
                     backendsFilter: List[String],
                     itemsPerPage: Option[Int],
                     currentPage: Option[Int]) : Action[AnyContent] =
    (Action andThen validateQueryParamKeyAction).async {
      integrationService.findWithFilters(IntegrationFilter(searchTerm, platformFilter, backendsFilter, itemsPerPage, currentPage))
      .map(result => {
       logger.warn(s"FindWithFilter results: ${result.count} - SearchTerms: ${valuesOrNone(searchTerm)} PlatformFilters: ${valuesOrNone(platformFilter.map(_.toString))} itemsPerPage: ${itemsPerPage.map(_.toString).getOrElse("Value Not Set")} currentPage: ${currentPage.map(_.toString).getOrElse("Value Not Set")}" )
        Ok(Json.toJson(result))
      })
  }

  private def valuesOrNone(listOfThings: List[String]) ={
    listOfThings match{
      case Nil => "None"
      case _ => listOfThings.mkString(" ")
    }
  }

  def deleteByIntegrationId(integrationId: IntegrationId): Action[AnyContent] = Action.async { implicit request =>
    integrationService.deleteByIntegrationId(integrationId).map {
        case NoContentDeleteApiResult => NoContent
        case _ => NotFound
      }

  }

  def deleteWithFilters(platformFilter: List[PlatformType]) : Action[AnyContent] = Action.async {
     if(platformFilter.size>1) {
      Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("only one platform can be deleted at a time"))))))
     } else {
       platformFilter.headOption match {
        case Some(platform) => integrationService.deleteByPlatform(platform).map(numberDeleted => {
              logger.warn(s"DeleteWithFilters numberDeleted is $numberDeleted PlatformFilters: ${valuesOrNone(platformFilter.map(_.toString))}")
                Ok(Json.toJson(DeleteIntegrationsResponse(numberDeleted)))})
        case None => Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("DeleteWithFilters no platformtype passed as filter"))))))
        
      }
    }
  }
}
