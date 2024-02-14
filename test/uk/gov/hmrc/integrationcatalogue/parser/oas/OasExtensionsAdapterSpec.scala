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

package uk.gov.hmrc.integrationcatalogue.parser.oas

import java.util

import cats.data.NonEmptyList
import io.swagger.v3.oas.models.info.Info
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.ApiStatus
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.{ExtensionKeys, IntegrationCatalogueExtensions, OASExtensionsAdapter}
import uk.gov.hmrc.integrationcatalogue.testdata.{ApiTestData, OasTestData}

class OasExtensionsAdapterSpec extends AnyWordSpec
    with Matchers with MockitoSugar with ApiTestData with OasTestData with BeforeAndAfterEach with ExtensionKeys with OASExtensionsAdapter {

  trait Setup {
    val publisherRefValue  = "SOMEREFERENCE"
    val publisherRefInt    = java.lang.Integer.valueOf(1234)
    val publisherRefDouble = java.lang.Double.valueOf(1.5)
    val publisherRefList   = new util.ArrayList[Object]()
    val shortDescription   = "I am a short description"

    val mockAppConfig: AppConfig = mock[AppConfig]

    val backendValues = new util.ArrayList[Object]()
    backendValues.add("ITMP"); backendValues.add("NPS")

    val extensionsWithOnlyBackendsAndReviewDate = new util.HashMap[String, Object]()
    extensionsWithOnlyBackendsAndReviewDate.put(REVIEWED_DATE_EXTENSION_KEY, "2020-12-25")
    extensionsWithOnlyBackendsAndReviewDate.put(BACKEND_EXTENSION_KEY, backendValues)

    val extensionsWithReviewDateAndStatus = new util.HashMap[String, Object]()
    extensionsWithReviewDateAndStatus.put(REVIEWED_DATE_EXTENSION_KEY, "2020-12-25")
    extensionsWithReviewDateAndStatus.put(STATUS_EXTENSION_KEY, ApiStatus.ALPHA.entryName)

    val extensionsWithInvalidStatus = new util.HashMap[String, Object]()
    extensionsWithInvalidStatus.put(STATUS_EXTENSION_KEY, "invalid_status")

    val extensionsWithWrongTypeStatus = new util.HashMap[String, Object]()
    extensionsWithWrongTypeStatus.put(STATUS_EXTENSION_KEY, java.lang.Double.valueOf(10.5))

    val extensionsWithEmptyStatus = new util.HashMap[String, Object]()
    extensionsWithEmptyStatus.put(STATUS_EXTENSION_KEY, "")

    val extensionsWithNullStatus = new util.HashMap[String, Object]()
    extensionsWithNullStatus.put(STATUS_EXTENSION_KEY, "")

    val extensionsWithReviewDateAndPublisherReference = new util.HashMap[String, Object]()
    extensionsWithReviewDateAndPublisherReference.put(REVIEWED_DATE_EXTENSION_KEY, "2020-12-25")
    extensionsWithReviewDateAndPublisherReference.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefValue)

    val extensionsWithInvalidShortDesc = new util.HashMap[String, Object]()
    extensionsWithInvalidShortDesc.put(SHORT_DESC_EXTENSION_KEY, java.lang.Double.valueOf(10.5))

    val extensionsWithReviewDatePublisherReferenceAndBackends = new util.HashMap[String, Object]()
    extensionsWithReviewDatePublisherReferenceAndBackends.put(REVIEWED_DATE_EXTENSION_KEY, "2020-12-25")
    extensionsWithReviewDatePublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefValue)
    extensionsWithReviewDatePublisherReferenceAndBackends.put(BACKEND_EXTENSION_KEY, backendValues)

    val extensionsWithBadReviewDateString = new util.HashMap[String, Object]()
    extensionsWithBadReviewDateString.put(REVIEWED_DATE_EXTENSION_KEY, "Cheese")

    val extensionsWithBadReviewDateType = new util.HashMap[String, Object]()
    extensionsWithBadReviewDateType.put(REVIEWED_DATE_EXTENSION_KEY, Integer.valueOf(12345))

    val extensionsWithShortDescAndPublisherReferenceAndBackends = new util.HashMap[String, Object]()
    extensionsWithShortDescAndPublisherReferenceAndBackends.put(REVIEWED_DATE_EXTENSION_KEY, "2020-12-25")
    extensionsWithShortDescAndPublisherReferenceAndBackends.put(SHORT_DESC_EXTENSION_KEY, shortDescription)
    extensionsWithShortDescAndPublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefValue)
    extensionsWithShortDescAndPublisherReferenceAndBackends.put(BACKEND_EXTENSION_KEY, backendValues)
    extensionsWithShortDescAndPublisherReferenceAndBackends.put(STATUS_EXTENSION_KEY, ApiStatus.DEPRECATED.entryName)

    val extensionsWithReviewDateAndTime = new util.HashMap[String, Object]()
    extensionsWithReviewDateAndTime.put(REVIEWED_DATE_EXTENSION_KEY, "2020-12-25T12:00:00")

    def generateInfoObject(extensionsValues: util.HashMap[String, Object]): Info = {
      val info       = new Info()
      val extensions = new util.HashMap[String, Object]()

      extensions.put(EXTENSIONS_KEY, extensionsValues)
      info.setExtensions(extensions)
      info
    }

  }

  "parse" should {

    "return Right when extensions is empty but publisher reference from header is provided" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDateAndPublisherReference), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors)      =>
          errors.map(println)
          fail()
        case Right(extensions) =>
          extensions.backends shouldBe Nil
          extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Left with error when extensions is empty and publisher reference is empty" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(new util.HashMap[String, Object]()), None, mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Publisher Reference must be provided and must be valid"
        case Right(_)     => fail()
      }
    }

    "return Right when extensions has backends but no status or publisher reference and publisher reference from header is provided" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithOnlyBackendsAndReviewDate), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefValue
          extensions.status shouldBe ApiStatus.LIVE
      }
    }

    "return Right when extensions has backends and publisher reference and publisher reference from header matches extensions" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDatePublisherReferenceAndBackends), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Left with error when extensions has a value but no publisher reference provided" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithOnlyBackendsAndReviewDate), None, mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Publisher Reference must be provided and must be valid"
        case Right(_)     => fail()
      }
    }

    "return Left when extensions is set with backends and publisher ref and header publisher reference does not match extensions value" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDatePublisherReferenceAndBackends), Some("SOMEOTHERREFERENCE"), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Publisher reference provided twice but they do not match"
        case Right(_)     => fail()
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as string" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDatePublisherReferenceAndBackends), None, mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as Integer" in new Setup {
      extensionsWithReviewDatePublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefInt)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDatePublisherReferenceAndBackends), None, mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefInt.toString
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as Double" in new Setup {
      extensionsWithReviewDatePublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefDouble)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDatePublisherReferenceAndBackends), None, mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefDouble.toString
      }
    }

    "return Right with backend strings and publisher ref when extensions is set with backends and publisher ref as List" in new Setup {
      extensionsWithReviewDatePublisherReferenceAndBackends.put(PUBLISHER_REF_EXTENSION_KEY, publisherRefList)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDatePublisherReferenceAndBackends), None, mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Invalid value. Expected a string, integer or double but found value: [] of type class java.util.ArrayList"
        case Right(_)     => fail()
      }
    }

    "return Right with backend strings when extensions is set and backends are defined" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithOnlyBackendsAndReviewDate), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Right when extensions is and only review datetime is added" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDateAndTime), Some(""), mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) => extensions.backends shouldBe Nil
      }
    }

    "return Left when extensions is empty" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(new util.HashMap[String, Object]()), Some(""), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Reviewed date must be provided"
        case Right(_)     => fail()
      }
    }

    "return a Left with the correct error message when extensions is unexpected format " in new Setup {
      val info                        = new Info()
      val extensionsWithEmptyBackends = new util.HashMap[String, Object]()

      extensionsWithEmptyBackends.put(EXTENSIONS_KEY, "")
      info.setExtensions(extensionsWithEmptyBackends)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(info, Some(""), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "attribute x-integration-catalogue is not of type `object`"
        case Right(_)     => fail()
      }
    }

    "return a Left with the correct error message when backends is unexpected format " in new Setup {
      val info                                                                 = new Info()
      val extensionsWithNullBackends                                           = new util.HashMap[String, Object]()
      val invalidBackends                                                      = new util.HashMap[String, util.List[String]]()
      invalidBackends.put(BACKEND_EXTENSION_KEY, null)
      extensionsWithNullBackends.put(EXTENSIONS_KEY, invalidBackends)
      info.setExtensions(extensionsWithNullBackends)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] = parseExtensions(info, Some(""), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "backends must be a list but was: Some(null)"
        case Right(_)     => fail()
      }
    }

    "return Right with short description when extensions is set and short description is defined" in new Setup {
      when(mockAppConfig.shortDescLength).thenReturn(180)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithShortDescAndPublisherReferenceAndBackends), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(_)           => fail()
        case Right(extensions) =>
          extensions.backends shouldBe List("ITMP", "NPS")
          extensions.publisherReference shouldBe publisherRefValue
          extensions.shortDescription.isDefined shouldBe true
          extensions.status shouldBe ApiStatus.DEPRECATED
      }
    }

    "return Left when short description length is over maximum specified in config" in new Setup {
      when(mockAppConfig.shortDescLength).thenReturn(10)
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithShortDescAndPublisherReferenceAndBackends), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe s"Short Description cannot be more than ${mockAppConfig.shortDescLength} characters long."
        case Right(_)     => fail()
      }
    }

    "return Left when short description is not a string" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithInvalidShortDesc), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe s"Short Description must be a String"
        case Right(_)     => fail()
      }
    }

    "return Right when extensions only has status" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithReviewDateAndStatus), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(e)           => fail()
        case Right(extensions) =>
          extensions.status shouldBe ApiStatus.ALPHA
          extensions.backends shouldBe List.empty
          extensions.publisherReference shouldBe publisherRefValue
      }
    }

    "return Left when status is invalid" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithInvalidStatus), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe s"Status must be one of ALPHA, BETA, LIVE or DEPRECATED"
        case Right(_)     => fail()
      }
    }

    "return Left when status is not a string" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithWrongTypeStatus), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe s"Status must be one of ALPHA, BETA, LIVE or DEPRECATED"
        case Right(_)     => fail()
      }
    }

    "return Left when status empty" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithEmptyStatus), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe s"Status must be one of ALPHA, BETA, LIVE or DEPRECATED"
        case Right(_)     => fail()
      }
    }

    "return Left when status null" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithNullStatus), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe s"Status must be one of ALPHA, BETA, LIVE or DEPRECATED"
        case Right(_)     => fail()
      }
    }

    "return Left when status reviewed date is not an accepted iso 8601 format" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithBadReviewDateString), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Reviewed date is not a valid date"
        case Right(_) => fail()
      }
    }

    "return Left when status reviewed date is not a String" in new Setup {
      val result: Either[NonEmptyList[String], IntegrationCatalogueExtensions] =
        parseExtensions(generateInfoObject(extensionsWithBadReviewDateType), Some(publisherRefValue), mockAppConfig)
      result match {
        case Left(errors) => errors.head shouldBe "Reviewed date is not a valid date"
        case Right(_) => fail()
      }
    }

  }
}
