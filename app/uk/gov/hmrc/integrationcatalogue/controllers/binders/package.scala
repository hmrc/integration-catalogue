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

package uk.gov.hmrc.integrationcatalogue.controllers

import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType, IntegrationType}

import java.util.UUID
import scala.util.Try

package object binders {

  private def integrationIdFromString(text: String): Either[String, IntegrationId] = {
    Try(UUID.fromString(text))
      .toOption
      .toRight(s"Cannot accept $text as IntegrationsId")
      .map(IntegrationId(_))
  }

  implicit def integrationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[IntegrationId] = new PathBindable[IntegrationId] {
    override def bind(key: String, value: String): Either[String, IntegrationId] = {
      textBinder.bind(key, value).flatMap(integrationIdFromString)
    }

    override def unbind(key: String, integrationId: IntegrationId): String = {
      textBinder.unbind(key, integrationId.value.toString)
    }
  }

  private def handleStringToPlatformType(stringVal: String): Either[String, PlatformType] = {
    Try(PlatformType.withNameInsensitive(stringVal))
      .toOption
      .toRight(s"Cannot accept $stringVal as PlatformType")
  }

  private def handleStringToIntegrationType(stringVal: String): Either[String, IntegrationType] = {
    Try(IntegrationType.withNameInsensitive(stringVal))
      .toOption
      .toRight(s"Cannot accept $stringVal as IntegrationType")
  }

  implicit def integrationTypeQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[IntegrationType] =
    new QueryStringBindable[IntegrationType] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, IntegrationType]] = {
        textBinder.bind(key, params).map {
          case Right(integrationType) => handleStringToIntegrationType(integrationType)
          case Left(_) => Left("Unable to bind an integrationType")
        }
      }

      override def unbind(key: String, integrationType: IntegrationType): String = {
        textBinder.unbind(key, integrationType.entryName)
      }
    }

  implicit def platformTypeQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[PlatformType] =
    new QueryStringBindable[PlatformType] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PlatformType]] = {
        textBinder.bind(key, params).map {
          case Right(platform) => handleStringToPlatformType(platform)
          case Left(_) => Left("Unable to bind an platform")
        }
      }

      override def unbind(key: String, platform: PlatformType): String = {
        textBinder.unbind(key, platform.toString)
      }
    }

}