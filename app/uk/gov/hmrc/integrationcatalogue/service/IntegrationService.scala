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

package uk.gov.hmrc.integrationcatalogue.service

import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType

@Singleton
class IntegrationService @Inject()(integrationRepository: IntegrationRepository) extends Logging {

  def findWithFilters(filter: IntegrationFilter): Future[IntegrationResponse] ={
    integrationRepository.findWithFilters(filter)
  }

  def findById(integrationId: IntegrationId): Future[Option[IntegrationDetail]] = {
    integrationRepository.findById(integrationId)

  }

  def deleteByIntegrationId(integrationId: IntegrationId)(implicit ec: ExecutionContext): Future[DeleteApiResult] = {

    def doDelete(integrationId: IntegrationId): Future[NoContentDeleteApiResult.type] = {
     integrationRepository.deleteById(integrationId).map(_ => NoContentDeleteApiResult)
    }

    integrationRepository.findById(integrationId).flatMap {
        case None                     => Future.successful(NotFoundDeleteApiResult)
        case Some(_: IntegrationDetail) => doDelete(integrationId)
      }
  }

  def deleteByPlatform(platform: PlatformType): Future[Int] = {
    integrationRepository.deleteByPlatform(platform)
  }

}


