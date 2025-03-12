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

package uk.gov.hmrc.integrationcatalogue.config

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType

case class PlatformNumbering(platformType: PlatformType, start: Int)

object PlatformNumbering {

  implicit val platformNumberingConfigLoader: ConfigLoader[PlatformNumbering] =
    (rootConfig: Config, path: String) => {
      val config = rootConfig.getConfig(path)

      PlatformNumbering(
        platformType = PlatformType.withName(config.getString("platform")),
        start = config.getInt("start")
      )
    }

}

trait ApiNumbering {

  def buildApiNumber(sequence: Int): String = s"$prefix$sequence"

  def prefix: String

  def platforms: Seq[PlatformNumbering]

  def forPlatformType(platformType: PlatformType): Option[PlatformNumbering] = {
    platforms.find(_.platformType == platformType)
  }

}

@Singleton
class ApiNumberingImpl @Inject()(configuration: Configuration) extends ApiNumbering {

  override val prefix: String =
    configuration.get[String]("apiNumbering.prefix")

  override val platforms: Seq[PlatformNumbering] =
    configuration.get[Map[String, PlatformNumbering]]("apiNumbering.platforms").values.toSeq

}
