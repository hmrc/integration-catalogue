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
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, IntegrationDetail, IntegrationFilter}
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.utils.ApiNumberExtractor

import scala.concurrent.{ExecutionContext, Future}

class ApiNumberExtractionJob @Inject()(
    apiNumberExtractor: ApiNumberExtractor,
    integrationRepository: IntegrationRepository
)(implicit ec: ExecutionContext) extends MongoJob with Logging {

  private val batchSize = 100

  override def run(): Future[Unit] = {
    logger.info(s"Running API number extraction job...")

    val batchSize = 20

    def processBatch(currentPage: Int, accSummary: MigrationSummary): Future[MigrationSummary] = {
      integrationRepository.findWithFilters(IntegrationFilter(currentPage = Some(currentPage), itemsPerPage = Some(batchSize))).flatMap(response => {
        logger.info(s"Processing batch ${currentPage + 1}...")
        if (response.results.isEmpty) {
          Future.successful(accSummary)
        } else {
          val updatedApiDetails = response.results.flatMap(integrationDetail => {
              val currentApiDetail = integrationDetail.asInstanceOf[ApiDetail]
              val transformedApiDetails = apiNumberExtractor.extract(currentApiDetail)
              val apiNumberExtracted = transformedApiDetails.title != currentApiDetail.title

              Option.when(apiNumberExtracted)(transformedApiDetails)
            })

          val updateResults = Future.sequence(updatedApiDetails.map(apiDetail => {
            integrationRepository.findAndModify(apiDetail).map {
              case Right((a, _)) => true
              case Left(e) => false
            }
          }))

          updateResults.flatMap(r => {
            val successCount = r.count(identity)
            val failureCount = r.size - successCount
            val batchSummary = MigrationSummary(
              response.results.size,
              updatedApiDetails.size,
              successCount,
              failureCount
            )

            processBatch(currentPage + 1, accSummary + batchSummary)
          })
        }
      })
    }

    val resultFuture = processBatch(0, MigrationSummary(0, 0, 0, 0))
    resultFuture.map(finalSummary => {
      logger.info(s"API number extraction job completed. Final summary: $finalSummary")
    })
  }
}

case class MigrationSummary(
  apiCount: Int,
  validApiNumberCount: Int,
  extractSuccessCount: Int,
  extractFailureCount: Int
) {
  override def toString: String = {
    "ApiNumberExtractionJob Migration Report: " +
      s"Total APIs=$apiCount, " +
      s"APIs with valid numbers=$validApiNumberCount, " +
      s"API numbers extracted successfully=$extractSuccessCount, " +
      s"API number extraction failures=$extractFailureCount"
  }

  def +(other: MigrationSummary): MigrationSummary =
    MigrationSummary(
      apiCount + other.apiCount,
      validApiNumberCount + other.validApiNumberCount,
      extractSuccessCount + other.extractSuccessCount,
      extractFailureCount + other.extractFailureCount
    )

}
