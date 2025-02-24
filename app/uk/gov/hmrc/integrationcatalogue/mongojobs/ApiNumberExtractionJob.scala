/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.mongojobs

import com.google.inject.Inject
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, IntegrationFilter}
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.utils.ApiNumberExtractor

import scala.None
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class ApiNumberExtractionJob @Inject()(
    apiNumberExtractor: ApiNumberExtractor,
    integrationRepository: IntegrationRepository
)(implicit ec: ExecutionContext) extends MongoJob with Logging {

  override def run(): Future[Unit] = {
    logger.info(s"Running API number extraction job...")
    integrationRepository.findWithFilters(IntegrationFilter()).map(response => {
      val updatedApiDetails = response.results
        .flatMap(integrationDetail => {
          val currentApiDetail = integrationDetail.asInstanceOf[ApiDetail]
          val transformedApiDetails = apiNumberExtractor.extract(currentApiDetail)
          val apiNumberExtracted = transformedApiDetails.title != currentApiDetail.title

          Option.when(apiNumberExtracted)(transformedApiDetails)
        })
      val allUpdates = Future.sequence(updatedApiDetails.map(apiDetail => {
        integrationRepository.findAndModify(apiDetail).flatMap {
          case Right((a, _)) =>
            Future.successful(())
          case Left(e) =>
            Future.failed(e)
        }
      })).map(_ => ())
      Await.result(allUpdates, 2.seconds)
      allUpdates
    })
  }

}

