/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.metrics

import com.google.inject.Singleton
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.repository.IntegrationRepository
import uk.gov.hmrc.mongo.metrix.MetricSource

import javax.inject.Inject
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class TotalApisCount @Inject()(val integrationRepository: IntegrationRepository) extends MetricSource with Logging {

  override def metrics(implicit ec: ExecutionContext): Future[Map[String, Int]] = {
    integrationRepository.getTotalApisCount().map { apisCount =>
      logger.info(s"[METRIC] Collecting metrics for Total APIs Count - $apisCount")
      Map("totalApisCount" -> apisCount.toInt)
    }
  }

}
