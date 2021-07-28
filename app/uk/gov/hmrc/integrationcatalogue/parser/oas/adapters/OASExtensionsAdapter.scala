/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.parser.oas.adapters

import cats.data.Validated._
import cats.data._
import cats.implicits._
import io.swagger.v3.oas.models.info.Info

import java.util
import scala.collection.JavaConverters._

import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import org.joda.time.format.ISODateTimeFormat

trait OASExtensionsAdapter extends ExtensionKeys {

  def parseExtensions(info: Info,
                      publisherReference: Option[String],
                      appConfig: AppConfig): Either[cats.data.NonEmptyList[String], IntegrationCatalogueExtensions] = {
    getIntegrationCatalogueExtensionsMap(getExtensions(info))
      .andThen(integrationCatalogueExtensions => {
        {
          (
            getBackends(integrationCatalogueExtensions),
            getPublisherReference(integrationCatalogueExtensions, publisherReference),
            getShortDescription(integrationCatalogueExtensions, appConfig),
            getStatus(integrationCatalogueExtensions),
            getReviewedDate(integrationCatalogueExtensions)
          )
        }.mapN((backends, publisherReference, shortDescription, status, reviewedDate) => {
          IntegrationCatalogueExtensions(backends, publisherReference, shortDescription, status, reviewedDate)
        })
      })
      .toEither
  }

  private def getExtensions(info: Info): Map[String, AnyRef] = {
    info.getExtensions match {
      case x if x == null => Map.empty[String, AnyRef]
      case ext            => ext.asScala.toMap
    }
  }

  private def getIntegrationCatalogueExtensionsMap(extensions: Map[String, AnyRef]): ValidatedNel[String, Map[String, AnyRef]] =
    extensions.get(EXTENSIONS_KEY) match {
      case None                                                     => Validated.valid(Map.empty)
      case Some(e) if e.isInstanceOf[java.util.Map[String, AnyRef]] =>
        Validated.valid(e.asInstanceOf[java.util.Map[String, AnyRef]]
          .asScala
          .toMap
          .filter { case (k, _) => k != null })
      case Some(_)                                                  => "attribute x-integration-catalogue is not of type `object`".invalidNel[Map[String, AnyRef]]
    }

  private def getBackends(extensions: Map[String, AnyRef]): ValidatedNel[String, Seq[String]] = {
    extensions.get(BACKEND_EXTENSION_KEY) match {
      case None                                         => Validated.valid(Seq.empty)
      case Some(listOfBackends: util.ArrayList[AnyRef]) =>
        Validated.valid(listOfBackends.asScala.toList.map(_.toString))
      case unknown                                      => s"backends must be a list but was: $unknown".invalidNel[Seq[String]]
    }
  }

  def getShortDescription(extensions: Map[String, AnyRef], appConfig: AppConfig) : ValidatedNel[String, Option[String]] = {
    extensions.get(SHORT_DESC_EXTENSION_KEY) match {
      case None =>  Validated.valid(None)
      case Some(x: String) => if(x.length > appConfig.shortDescLength) {
        s"Short Description cannot be more than ${appConfig.shortDescLength} characters long.".invalidNel[Option[String]]
      } else Validated.valid(Some(x))
      case unknown => "Short Description must be a String".invalidNel[Option[String]]

    }
  }

  def getStatus(extensions: Map[String, AnyRef]): ValidatedNel[String, ApiStatus] = {
    extensions.get(STATUS_EXTENSION_KEY) match {
      case None => Validated.valid(LIVE)
      case Some(x: String) => {
          if(ApiStatus.values.toList.map(_.entryName).contains(x.toUpperCase)){
            Validated.valid(ApiStatus.withName(x))
          } else "Status must be one of ALPHA, BETA, LIVE or DEPRECATED".invalidNel[ApiStatus]
      }
      case unknown => "Status must be a String".invalidNel[ApiStatus]
    }
  }

  def getReviewedDate(extensions: Map[String, AnyRef]): ValidatedNel[String, DateTime] = {
    extensions.get(REVIEWED_DATE_EXTENSION_KEY) match {
      case None =>  "Reviewed date must be provided".invalidNel[DateTime]
      case Some(x: String) =>
        Try[DateTime]{
          DateTime.parse(x, ISODateTimeFormat.dateOptionalTimeParser())
        } match {
         case Success(dateTime) => Validated.valid(dateTime)
         case Failure(e) =>  println(e.getMessage())
         "Reviewed date is not a valid date".invalidNel[DateTime]
        }
      case Some(_)  => "Reviewed date is not a valid date".invalidNel[DateTime]
    }
  }

  def getPublisherReference(extensions: Map[String, AnyRef], publisherReference: Option[String]): ValidatedNel[String, String] = {

    def handlePublisherReference(publisherReference: Option[String]) = publisherReference match {
      case None    => "Publisher Reference must be provided and must be valid".invalidNel[String]
      case Some(x) => Validated.valid(x)
    }

    def handleMultiplePublisherReferences(publisherReference: Option[String], extractedPublisherRef: String) = {
      publisherReference match {
        case None    => Validated.valid(extractedPublisherRef)
        case Some(x) if (x.equalsIgnoreCase(extractedPublisherRef)) => Validated.valid(extractedPublisherRef)
        case Some(_) =>  "Publisher reference provided twice but they do not match".invalidNel[String]
      }
    }

    extensions.get(PUBLISHER_REF_EXTENSION_KEY) match {
      case None             => handlePublisherReference(publisherReference)
      case Some(x: String)  => handleMultiplePublisherReferences(publisherReference, x)
      case Some(x: Integer) => handleMultiplePublisherReferences(publisherReference, x.toString)
      case Some(x: java.lang.Double)  => handleMultiplePublisherReferences(publisherReference, x.toString)
      case Some(o)  => s"Invalid value. Expected a string, integer or double but found value: $o of type ${o.getClass.toString}".invalidNel[String]
    }
  }
}

case class IntegrationCatalogueExtensions(backends: Seq[String], publisherReference: String, shortDescription: Option[String], status: ApiStatus, reviewedDate: DateTime)
