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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import play.api.mvc.{Action, AnyContent}
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models.common.ContactInformation
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.PlatformContactResponse
import play.api.libs.json.Json
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

@Singleton
class PlatformController @Inject()(controllerComponents: ControllerComponents, appConfig: AppConfig)
                                    (implicit val ec: ExecutionContext)
                                     extends BackendController(controllerComponents){

                                    
  def getPlatformContacts() : Action[AnyContent] = Action.async { request =>
        Future.successful(Ok(Json.toJson(appConfig.platformContacts)))
  }                                  
}