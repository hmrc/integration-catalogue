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

package uk.gov.hmrc.integrationcatalogue.parser.oas

import cats.data.NonEmptyList
import io.swagger.v3.oas.models.info.Info
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.{ExtensionKeys, IntegrationCatalogueExtensions, OASExtensionsAdapter}
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

import java.util

class OasExtensionsAdapterSpec extends WordSpec with Matchers with MockitoSugar with ApiTestData with OasTestData with BeforeAndAfterEach with ExtensionKeys with OASExtensionsAdapter {

  trait Setup {
    val publisherRefValue = "SOMEREFERENCE"
    val publisherRefInt = new java.lang.Integer(1234)
    val publisherRefDouble = new java.lang.Double(1.5)
    val publisherRefList = new util.ArrayList[Object]()

    val backendValues = new util.ArrayList[Object]()
    backendValues.add("ITMP"); backendValues.add("NPS")


    val extensionsWithOnlyBackends = new util.HashMap[String, Object]()
    extensionsWithOnlyBackends.put(BACKEND_EXTENSION_KEY, backendValues)

    val extensionsWithOnlyPublisherReference = new util.HashMap[String, Object]()
    extensionsWithOnlyPublisherReference.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefValue)

    val extensionsWithBothPublisherReferenceAndBackends = new util.HashMap[String,Object]()
    extensionsWithBothPublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefValue)
    extensionsWithBothPublisherReferenceAndBackends.put(BACKEND_EXTENSION_KEY, backendValues)

    def generateInfoObject(extensionsValues: util.HashMap[String, Object]): Info ={
      val info = new Info()
      val extensions = new util.HashMap[String, Object]()

      extensions.put(EXTENSIONS_KEY, extensionsValues)
      info.setExtensions(extensions)
      info
    }


  }

  "parse" should {

    "return Right when extensions is empty but publisher reference from header is provided" in new Setup {
      val info = new Info()
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(info, Some(publisherRefValue))
      result match {
        case Left(_)      => fail
        case Right(extensions) =>
          extensions.backends shouldBe Nil
          extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Left with error when extensions is empty and publisher reference is empty" in new Setup {
      val info = new Info()
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(info, None)
      result match {
        case Left(errors)   => errors.head shouldBe "Publisher Reference must be provided and must be valid"
        case Right(_) => fail
      }
    }

  "return Right when extensions has backends but no publisher reference and publisher reference from header is provided" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithOnlyBackends), Some(publisherRefValue))
      result match {
        case Left(_)      => fail
        case Right(extensions) =>
                extensions.backends shouldBe List("ITMP", "NPS")
                extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Right when extensions has backends and publisher reference and publisher reference from header matches extensions" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithBothPublisherReferenceAndBackends), Some(publisherRefValue))
      result match {
        case Left(_)      => fail
        case Right(extensions) =>
                extensions.backends shouldBe List("ITMP", "NPS")
                extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Left with error when extensions has a value but no publisher reference provided" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithOnlyBackends), None)
      result match {
        case Left(errors)   => errors.head shouldBe "Publisher Reference must be provided and must be valid"
        case Right(_) => fail
      }
    }


    "return Left when extensions is set with backends and publisher ref and header publisher reference does not match extensions value" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithBothPublisherReferenceAndBackends), Some("SOMEOTHERREFERENCE"))
      result match {
        case Left(errors)   => errors.head shouldBe "Publisher reference provided twice but they do not match"
        case Right(_) => fail
      }
    }


    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as string" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithBothPublisherReferenceAndBackends), None)
      result match {
        case Left(_)   => fail
        case Right(extensions) => 
                extensions.backends shouldBe List("ITMP", "NPS")
                extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as Integer" in new Setup {
        extensionsWithBothPublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefInt)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithBothPublisherReferenceAndBackends), None)
      result match {
        case Left(_)   => fail
        case Right(extensions) => 
                extensions.backends shouldBe List("ITMP", "NPS")
                extensions.publisherReference shouldBe publisherRefInt.toString
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as Double" in new Setup {
        extensionsWithBothPublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefDouble)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithBothPublisherReferenceAndBackends), None)
      result match {
        case Left(_)   => fail
        case Right(extensions) => 
                extensions.backends shouldBe List("ITMP", "NPS")
                extensions.publisherReference shouldBe publisherRefDouble.toString
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as List" in new Setup {
        extensionsWithBothPublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefList)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithBothPublisherReferenceAndBackends), None)
      result match {
        case Left(errors)   => errors.head shouldBe "Invalid value. Expected a string but found : [] class java.util.ArrayList"
        case Right(_) => fail
      }
    }
    
    "return Right with backend strings when extensions is set and backends are defined" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(extensionsWithOnlyBackends), Some(publisherRefValue))
      result match {
        case Left(_)   => fail
        case Right(extensions) => 
         extensions.backends shouldBe List("ITMP", "NPS")
         extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Right with empty List when extensions is set and no backends are defined" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(generateInfoObject(new util.HashMap()), Some(""))
      result match {
        case Left(_)   => fail
        case Right(extensions) => extensions.backends shouldBe Nil
      }
    }

    "return a Left with the correct error message when extensions is unexpected format " in new Setup {
    val info = new Info()
    val extensionsWithEmptyBackends = new util.HashMap[String, Object]()

      extensionsWithEmptyBackends.put(EXTENSIONS_KEY, "")
      info.setExtensions(extensionsWithEmptyBackends)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(info, Some(""))
      result match {
        case Left(errors) => errors.head shouldBe "attribute x-integration-catalogue is not of type `object`"
        case Right(_) => fail
      }
    }

    "return a Left with the correct error message when backends is unexpected format " in new Setup {
      val info = new Info()
      val extensionsWithNullBackends = new util.HashMap[String, Object]()
      val invalidBackends = new util.HashMap[String, util.List[String]]()
      invalidBackends.put(BACKEND_EXTENSION_KEY, null)
      extensionsWithNullBackends.put(EXTENSIONS_KEY, invalidBackends)
      info.setExtensions(extensionsWithNullBackends)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(info,Some(""))
      result match {
        case Left(errors) => errors.head shouldBe "backends must be a list but was: Some(null)"
        case Right(_) => fail
      }
    }

  }
}
