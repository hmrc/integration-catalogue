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
import org.mongodb.scala.model.Sorts.ascending
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, IntegrationDetail}
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.integrationcatalogue.utils.ApiNumberExtractor
import org.mongodb.scala.ObservableFuture
import scala.concurrent.{ExecutionContext, Future}

class ApiNumberExtractionJob @Inject()(
    apiNumberExtractor: ApiNumberExtractor,
    integrationRepository: IntegrationRepository
)(implicit ec: ExecutionContext) extends MongoJob with Logging {

  private val batchSize = 50

  override def run(): Future[Unit] = {
    logger.info(s"Running API number extraction job...")

    def processBatch(currentPage: Int, accSummary: MigrationSummary): Future[MigrationSummary] = {
      /* We can't use integrationRepository.findWithFilters() to retrieve the results here because it sorts by title -
      since we are updating the title it means some rows get fetched multiple times and some never get fetched. We
      ensure consistent ordering by sorting on 'id' although other fields would also work */
      integrationRepository.collection.find()
        .skip(currentPage * batchSize)
        .limit(batchSize)
        .sort(ascending("id"))
        .toFuture()
        .map(_.toList)
        .flatMap(response => {
          if (response.isEmpty) {
            logger.info("No more APIs to process")
            Future.successful(accSummary)
          } else {
            logger.info(s"Processing batch ${currentPage + 1}...")
            val updatedApiDetails = response.collect { case apiDetail: ApiDetail => apiDetail }.flatMap(currentApiDetail => {
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
              val batchSummary = MigrationSummary(
                apiCount = response.size,
                validApiNumberCount = updatedApiDetails.size,
                extractSuccessCount = successCount,
                extractFailureCount = r.size - successCount
              )
              logger.info(s"Batch ${currentPage + 1} summary: $batchSummary")
              processBatch(currentPage + 1, accSummary + batchSummary)
            })
          }
        })
    }

    val resultFuture = processBatch(0, MigrationSummary())
    resultFuture.map(finalSummary => {
      logger.info(s"API number extraction job completed. Final summary: $finalSummary")
    })
  }
}

case class MigrationSummary(
  apiCount: Int = 0,
  validApiNumberCount: Int = 0,
  extractSuccessCount: Int = 0,
  extractFailureCount: Int = 0
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
