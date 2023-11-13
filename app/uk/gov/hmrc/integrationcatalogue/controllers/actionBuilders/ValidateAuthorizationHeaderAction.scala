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

import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage, HeaderKeys}
import uk.gov.hmrc.integrationcatalogue.utils.ValidateParameters

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ValidateAuthorizationHeaderAction @Inject() (appConfig: AppConfig)(implicit ec: ExecutionContext) extends ActionFilter[Request] with HttpErrorFunctions
    with ValidateParameters with Logging {

  override def executionContext: ExecutionContext = ec

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {

    val authHeader         = request.headers.get(HeaderNames.AUTHORIZATION).getOrElse("")
    val platformTypeHeader = request.headers.get(HeaderKeys.platformKey).getOrElse("")

    if (authHeader.nonEmpty && base64Decode(authHeader).map(_ == appConfig.authorizationKey).getOrElse(false)) {
      Future.successful(None)
    } else {
      validatePlatformType(platformTypeHeader) match {
        case Some(platformType) => validatePlatformAuthHeader(platformType, authHeader)
        case None               =>
          Future.successful(Some(
            BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("platform type header is missing or invalid")))))
          ))
      }
    }
  }

  private def validatePlatformAuthHeader(platformType: PlatformType, authHeader: String) = {
    appConfig.authPlatformMap.foreach(k => {
      val encodedString = Try(new String(Base64.getEncoder.encode(k._2.getBytes), StandardCharsets.UTF_8))
      logger.info(s"${k._1}  ${encodedString.getOrElse("ERROR-ENCODING")}")
    })
    appConfig.authPlatformMap.get(platformType) match {
      case Some(authKey) => if (base64Decode(authHeader).map(_ == authKey).getOrElse(false)) Future.successful(None)
        else {
          logger.info("Unauthorised -  key didn't match value in config")
          Future.successful(Some(Unauthorized(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Authorisation failed")))))))
        }
      case None          =>
        logger.info("Unauthorised - no config for platform type found")
        Future.successful(Some(Unauthorized(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Authorisation failed")))))))
    }
  }

  private def base64Decode(stringToDecode: String): Try[String] = Try(new String(Base64.getDecoder.decode(stringToDecode), StandardCharsets.UTF_8))

}
