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

package uk.gov.hmrc.integrationcatalogue.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.integrationcatalogue.config.ContactInformationForPlatform._
import uk.gov.hmrc.integrationcatalogue.models.PlatformContactResponse
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType._

@Singleton
class AppConfig @Inject() (config: Configuration) {

  val defaultShortDescLength: Int   = 180
  val oldIndexesToDrop: Seq[String] = config.getOptional[Seq[String]]("mongodb.oldIndexesToDrop").getOrElse(Seq.empty)
  val publishApiNumberIgnoreList: Set[String] = config.get[Seq[String]]("publish.apiNumber.ignoreList").toSet
  val apiTeamsTtlDays: Long = config.getOptional[Long]("mongodb.apiTeamsTtlDays").getOrElse(180)
  val shortDescLength: Int = config.getOptional[Int]("publish.shortDesc.maxLength").getOrElse(defaultShortDescLength)

  val authorizationKey: String = getString("authorizationKey")
  val cmaAuthorizationKey: String = getString("auth.authKey.cma")
  val apiPlatformAuthorizationKey: String = getString("auth.authKey.apiPlatform")
  val coreIfAuthorizationKey: String = getString("auth.authKey.coreIF")
  val desAuthorizationKey: String = getString("auth.authKey.DES")
  val cdsClassicAuthorizationKey: String = getString("auth.authKey.cdsClassic")
  val transactionEngineAuthorizationKey: String = getString("auth.authKey.transactionEngine")
  val sdesAuthorizationKey: String = getString("auth.authKey.SDES")
  val digiAuthorizationKey: String = getString("auth.authKey.DIGI")
  val dapiAuthorizationKey: String = getString("auth.authKey.DAPI")
  val cipAuthorizationKey: String = getString("auth.authKey.CIP")
  val hipAuthorizationKey: String = getString("auth.authKey.HIP")

  val authPlatformMap: Map[PlatformType, String] = Map(
    CMA -> cmaAuthorizationKey,
    API_PLATFORM -> apiPlatformAuthorizationKey,
    CORE_IF -> coreIfAuthorizationKey,
    DES -> desAuthorizationKey,
    CDS_CLASSIC -> cdsClassicAuthorizationKey,
    TRANSACTION_ENGINE -> transactionEngineAuthorizationKey,
    SDES -> sdesAuthorizationKey,
    DIGI -> digiAuthorizationKey,
    DAPI -> dapiAuthorizationKey,
    CIP -> cipAuthorizationKey,
    HIP -> hipAuthorizationKey
  )

  def platformContacts: Seq[PlatformContactResponse] = {
    // TODO handle empty config values
    PlatformType.values.toList
      .map(platform =>
        PlatformContactResponse(
          platform,
          getContactInformationForPlatform(
            platform,
            config.getOptional[String](s"platforms.$platform.name"),
            config.getOptional[String](s"platforms.$platform.email")
          ),
          config.getOptional[Boolean](s"platforms.$platform.overrideOasContacts").getOrElse(false)
        )
      )
  }

  def getString(key: String) =
    config.getOptional[String](key).getOrElse(throwConfigNotFoundError(key))

  private def throwConfigNotFoundError(key: String) =
    throw new RuntimeException(s"Could not find config key '$key'")

}
