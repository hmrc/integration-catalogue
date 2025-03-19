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

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, ApiDetailSummary}
import uk.gov.hmrc.integrationcatalogue.repository.{ApiDetailSummaryRepository, IntegrationRepository}
import uk.gov.hmrc.integrationcatalogue.utils.ApiNumberGenerator

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ApiNumberGenerationJob @Inject()(
  summaryRepository: ApiDetailSummaryRepository,
  integrationRepository: IntegrationRepository,
  apiNumberGenerator: ApiNumberGenerator
)(implicit ec: ExecutionContext) extends MongoJob with Logging {

  import ApiNumberGenerationJob.*

  override def run(): Future[Unit] = {
    processApis().map(_ => ())
  }

  def processApis(): Future[MigrationSummary] = {
    summaryRepository.findWithFilters(searchTerms = List.empty, platformTypes = List.empty)
      .map(_.sortBy(_.created))
      .map(_.map(processApi))
      .flatMap(Future.sequence(_))
      .map(
        _.foldRight(MigrationSummary())((result, summary) => summary.add(result))
      )
      .andThen {
        case Success(summary) =>
          logger.info(s"API number generation migration complete: summary=${summary.report}")
        case Failure(e) =>
          logger.error("API number generation migration failed", e)
      }
  }

  private def processApi(api: ApiDetailSummary): Future[MigrationResult] = {
    apiNumberGenerator.generate(api.platform, api.apiNumber).flatMap {
      case Some(apiNumber) if !api.apiNumber.contains(apiNumber) => updateApi(api, apiNumber)
      case Some(_) => Future.successful(HasApiNumber)
      case None => Future.successful(NoRule)
    }
  }

  private def updateApi(api: ApiDetailSummary, apiNumber: String): Future[MigrationResult] = {
    integrationRepository.findByPublisherRef(api.platform, api.publisherReference).flatMap {
      case Some(apiDetail: ApiDetail) =>
        integrationRepository
          .findAndModify(apiDetail.copy(apiNumber = Some(apiNumber)))
          .map {
            case Right(_) => ApiNumberUpdated
            case Left(e) =>
              logger.error(s"Error updating API ${apiDetail.id}", e)
              UpdateError
          }
      case _ =>
        logger.error(s"Cannot find API ${api.id}")
        Future.successful(UpdateError)
    }
  }

}

object ApiNumberGenerationJob {

  sealed trait MigrationResult

  private case object NoRule extends MigrationResult
  private case object HasApiNumber extends MigrationResult
  private case object ApiNumberUpdated extends MigrationResult
  private case object UpdateError extends MigrationResult

  case class MigrationSummary(apis: Int, hasApiNumber: Int, updated: Int, error: Int) {

    def add(migrationResult: MigrationResult): MigrationSummary = {
      migrationResult match {
        case NoRule => copy(apis = apis + 1)
        case HasApiNumber => copy(apis = apis + 1, hasApiNumber = hasApiNumber + 1)
        case ApiNumberUpdated => copy(apis = apis + 1, updated = updated + 1)
        case UpdateError => copy(apis = apis + 1, error = error + 1)
      }
    }

    def report: String = {
      Seq(
        s"apis: $apis",
        s"noRule: ${apis - hasApiNumber - updated - error}",
        s"hasApiNumber: $hasApiNumber",
        s"updated: $updated",
        s"error: $error"
      ).mkString(", ")
    }

  }

  object MigrationSummary {

    def apply(): MigrationSummary = {
      MigrationSummary(0, 0, 0, 0)
    }

  }

}
