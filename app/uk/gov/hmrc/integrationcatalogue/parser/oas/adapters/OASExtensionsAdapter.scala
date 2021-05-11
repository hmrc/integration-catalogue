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
import play.api.Logging

import java.util
import javax.inject.Singleton
import scala.collection.JavaConverters._

@Singleton
class OASExtensionsAdapter extends Logging with ExtensionKeys {

  def parse(info: Info, publisherReference: Option[String]): Either[cats.data.NonEmptyList[String], IntegrationCatalogueExtensions] = {

    getIntegrationCatalogueExtensionsMap(getExtensions(info))
      .andThen(integrationCatalogueExtensions => {
        {
          (
            getBackends(integrationCatalogueExtensions),
            getPublisherReference(integrationCatalogueExtensions, publisherReference)
          )
        }.mapN((backends, publisherReference) => {
          IntegrationCatalogueExtensions(backends, publisherReference)
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
      case None                                     => handlePublisherReference(publisherReference)
      case Some(obj) if (obj.isInstanceOf[String])  =>
        handleMultiplePublisherReferences(publisherReference, obj.asInstanceOf[String])

      case Some(obj) if (obj.isInstanceOf[Integer]) =>
        val extensionPublisherReference = obj.asInstanceOf[Integer].toString
        handleMultiplePublisherReferences(publisherReference, extensionPublisherReference)
      case Some(obj) if (obj.isInstanceOf[Double])  =>
        val extensionPublisherReference = obj.asInstanceOf[Double].toString
        handleMultiplePublisherReferences(publisherReference, extensionPublisherReference)
      case Some(o)  => s"Invalid value. Expected a string but found : $o ${o.getClass.toString}".invalidNel[String]
    }
  }

  // private def getStringValue(o: AnyRef): ValidatedNel[String, String] = {
  //   o match {
  //     case s: String           => Validated.valid(s)
  //     case n: Integer          => Validated.valid(n.toString)
  //     case n: java.lang.Double => Validated.valid(n.toString)
  //     // TODO : We should pass what field this is parsing so we can add it to the error
  //     case _                   => s"Invalid value. Expected a string but found : $o ${o.getClass.toString}".invalidNel[String]
  //   }
  // }
}

case class IntegrationCatalogueExtensions(backends: Seq[String], publisherReference: String)
