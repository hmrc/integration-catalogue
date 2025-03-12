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

package uk.gov.hmrc.integrationcatalogue

import com.google.inject.AbstractModule
import uk.gov.hmrc.integrationcatalogue.config.{ApiNumbering, ApiNumberingImpl}
import uk.gov.hmrc.integrationcatalogue.controllers.actionBuilders.{AuthenticatedIdentifierAction, IdentifierAction}
import uk.gov.hmrc.integrationcatalogue.scheduled.MetricsScheduler

import java.time.Clock

class CustomModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[MetricsScheduler]).asEagerSingleton()
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
    bind(classOf[ApiNumbering]).to(classOf[ApiNumberingImpl]).asEagerSingleton()
  }

}
