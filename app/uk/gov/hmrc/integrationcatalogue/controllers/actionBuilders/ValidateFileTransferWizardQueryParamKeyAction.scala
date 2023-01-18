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

package uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import _root_.uk.gov.hmrc.http.HttpErrorFunctions

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionFilter, Request, Result}

import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}

@Singleton
class ValidateFileTransferWizardQueryParamKeyAction @Inject() ()(implicit ec: ExecutionContext)
    extends ActionFilter[Request] with HttpErrorFunctions with Logging {
  override def executionContext: ExecutionContext = ec

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    val validKeys      = List("source", "target")
    val queryParamKeys = request.queryString.keys

    Future.successful(validateQueryParamKey(validKeys, queryParamKeys))
  }

  private def validateQueryParamKey(validKeys: List[String], queryParamKeys: Iterable[String]) = {
    if (!queryParamKeys.forall(validKeys.contains(_))) {
      logger.info("Invalid query parameter key provided. It is case sensitive")
      Some(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid query parameter key provided. It is case sensitive"))))))
    } else {
      val queryParamsSeq = queryParamKeys.toSeq
      if (queryParamsSeq.isEmpty || (queryParamsSeq.contains("source") && queryParamsSeq.contains("target"))) None
      else Some(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("You must either provide both source and target or no query parameters"))))))
    }
  }

}
