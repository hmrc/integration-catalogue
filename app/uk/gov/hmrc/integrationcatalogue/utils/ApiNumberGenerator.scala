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

package uk.gov.hmrc.integrationcatalogue.utils

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.integrationcatalogue.config.ApiNumbering
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.repository.PlatformSequenceRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiNumberGenerator @Inject()(
  apiNumbering: ApiNumbering,
  repository: PlatformSequenceRepository
)(implicit ec: ExecutionContext) {

  def generate(platform: PlatformType, existingApiNumber: Option[String]): Future[Option[String]] = {
    apiNumbering.forPlatformType(platform) match {
      case Some(platformNumbering) if existingApiNumber.isEmpty =>
        repository.nextValue(platformNumbering).map(
          sequence =>
            Some(apiNumbering.buildApiNumber(sequence))
        )
      case Some(_) => Future.successful(existingApiNumber)
      case None => Future.successful(None)
    }
  }

}
