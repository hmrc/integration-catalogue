/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.integrationcatalogue.parser

import cats.data.Validated._
import cats.data._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter
import uk.gov.hmrc.integrationcatalogue.parser.oas.{OASFileLoader, OASParserService}
import uk.gov.hmrc.integrationcatalogue.testdata._

import scala.io.BufferedSource
import scala.io.Source.fromURL

class OASParserServiceISpec extends AnyWordSpec with Matchers with OasParsedItTestData with OasTestData with GuiceOneAppPerSuite {

  trait Setup {

    val adapterService: OASV3Adapter   = app.injector.instanceOf[OASV3Adapter]
    val fileLoader: OASFileLoader      = new OASFileLoader
    val OASSpecType: SpecificationType = SpecificationType.OAS_V3
    val objInTest                      = new OASParserService(fileLoader, adapterService)

    def parseFileToString(filename: String): String = {
      val x: BufferedSource = fromURL(url = getClass.getResource(filename))
      val fileContents      = x.mkString
      x.close()
      fileContents
    }

    def validateRefHeader(header: Header, expectedName: String, expectedRef: String): Assertion = {
      header.name shouldBe expectedName
      header.ref shouldBe Some(expectedRef)
      header.deprecated shouldBe None
      header.required shouldBe None
      header.description shouldBe None
    }

    def validateHeader(header: Header, expectedName: String, expectedDescription: String): Assertion = {
      header.name shouldBe expectedName
      header.ref shouldBe None
      header.deprecated shouldBe None
      header.required shouldBe None
      header.description shouldBe Some(expectedDescription)
    }

    def testValidationFailureMessage(filePath: String, expectedErrorMessage: String, setHeaderPublisherRef: Boolean = true): Assertion = {

      val publisherReference      = if (setHeaderPublisherRef) Some("SOMEFILEREFERENCE") else None
      val oasFileContents: String = parseFileToString(filePath)

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(publisherReference, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Invalid(errors: NonEmptyList[List[String]]) =>
          errors.head.contains(expectedErrorMessage) shouldBe true
        case _                                           => fail()
      }

    }

  }

  "OASParserService" should {
    "parse oas file correctly with schema" in new Setup {
      val publisherReference      = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe "API1000 Get Data"

          parsedObject.endpoints.size shouldBe 1
          parsedObject.endpoints.head.methods.size shouldBe 1
          parsedObject.publisherReference shouldBe publisherReference
          val getMethod: EndpointMethod = parsedObject.endpoints.head.methods.head
          getMethod.httpMethod shouldBe "GET"
        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }
    "parse oas file correctly with default response" in new Setup {
      val publisherReference      = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withDefaultResponse.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe "API1000 Get Data"

          parsedObject.endpoints.size shouldBe 1
          parsedObject.endpoints.head.methods.size shouldBe 1
          val getMethod: EndpointMethod = parsedObject.endpoints.head.methods.head
          getMethod.httpMethod shouldBe "GET"
        case _                   => fail()
      }
    }

    "parse oas file and return endpoints in the order specified in the oas spec" in new Setup {
      val publisherReference      = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_multipleEndpoints.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject)                    =>
          parsedObject.title shouldBe "Swagger Petstore - OpenAPI 3.0"

          val endpoints = parsedObject.endpoints
          endpoints.size shouldBe 5
          endpoints.head.methods.size shouldBe 2

          val expectedEndpointsInOrder = List("/pet", "/pet/findByStatus", "/pet/findByTags", "/pet/{petId}", "/pet/{petId}/uploadImage")
          endpoints.map(_.path) shouldBe expectedEndpointsInOrder
        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

    "parse OpenApi object correctly with extensions" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, rawOASDataWithExtensions)

      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe PlatformType.CORE_IF
          parsedObject.specificationType shouldBe SpecificationType.OAS_V3

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name.getOrElse("") shouldBe oasContactName
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail
          parsedObject.hods shouldBe List("ITMP", "NPS")
          parsedObject.shortDescription.isDefined shouldBe true
          parsedObject.shortDescription shouldBe Some("I am a short description")

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

    "parse OpenApi object correctly with null in contact name" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, rawOASData(""))

      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe PlatformType.CORE_IF
          parsedObject.specificationType shouldBe SpecificationType.OAS_V3

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name shouldBe None
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

    "parse oas file correctly with valid short description" in new Setup {
      val publisherReference      = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withValidShortDesc.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject
            .shortDescription
            .getOrElse("") shouldBe "Hello Im a sensible short description, you wont find me getting too long and breaking any tests. No sireee!!"
        case _                   => fail()
      }

    }

    "parse oas file correctly with valid status" in new Setup {
      val publisherReference      = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withValidStatus.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.apiStatus shouldBe ApiStatus.BETA
        case _                   => fail()
      }

    }

    "catch error in oas file with short description over maximum length set in config" in new Setup {
      testValidationFailureMessage("/API1000_withInvalidShortDesc-tooLong.yaml", "Short Description cannot be more than 180 characters long.")

    }

    "catch error in oas file with short description type is not a string" in new Setup {
      testValidationFailureMessage("/API1000_withInvalidShortDesc-isWrongType.yaml", "Short Description must be a String")
    }

    "catch error in oas file when attribute x-integration-catalogue is not of type object" in new Setup {
      testValidationFailureMessage("/API1000_withInvalid-x-integration-catalogue.yaml", "attribute x-integration-catalogue is not of type `object`")
    }

    "catch error in oas file with extension backends type is not a list" in new Setup {
      testValidationFailureMessage("/API1000_withInvalidBackends-tooWrongType.yaml", "backends must be a list but was: Some(Invalid backend)")
    }

    "catch error in oas file with extension publisher-reference type is not a string, double or integer" in new Setup {
      testValidationFailureMessage(
        "/API1000_withInvalidPublisherRef-isWrongType.yaml",
        "Invalid value. Expected a string, integer or double but found value: true of type class java.lang.Boolean",
        setHeaderPublisherRef = false
      )
    }

    "catch error in oas file with missing extension publisher-reference" in new Setup {
      testValidationFailureMessage(
        "/API1000_withMissingPublisherRef.yaml",
        "Publisher Reference must be provided and must be valid",
        setHeaderPublisherRef = false
      )
    }

    "catch error in oas file with missing title" in new Setup {
      testValidationFailureMessage("/API1000_InvalidwithMissingTitle.yaml", "Invalid OAS, title missing from OAS specification")
    }

    "catch error in oas file with missing version" in new Setup {
      testValidationFailureMessage("/API1000_InvalidwithMissingVersion.yaml", "Invalid OAS, version missing from OAS specification")
    }

    "catch error in oas file with missing info object" in new Setup {
      testValidationFailureMessage("/API1000_InvalidwithMissingInfo.yaml", "Invalid OAS, info item missing from OAS specification")
    }

    "catch error in invalid oas file" in new Setup {
      testValidationFailureMessage("/API1000_InvalidOpenApiSpec.yaml", "attribute openapi is missing")
    }

    "catch error in invalid oas status" in new Setup {
      testValidationFailureMessage("/API1000_InvalidStatus.yaml", "Status must be one of ALPHA, BETA, LIVE or DEPRECATED")
    }

    "catch error in invalid oas reviewed date" in new Setup {
      testValidationFailureMessage("/API1000_withInvalidReviewedDate.yaml", "Reviewed date is not a valid date")
    }

    "catch error in invalid oas with no reviewed date" in new Setup {
      testValidationFailureMessage("/API1000_withNoReviewedDate.yaml", "Reviewed date must be provided")
    }

    "catch error in invalid oas with empty reviewed date" in new Setup {
      testValidationFailureMessage("/API1000_withEmptyReviewedDate.yaml", "Reviewed date is not a valid date")
    }

    "catch error when publisher ref in oas file does not match the one in the request" in new Setup {
      val publisherReference      = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-String.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case errors: Invalid[NonEmptyList[List[String]]] =>
          errors.e.head.contains("Publisher reference provided twice but they do not match") shouldBe true
        case _                                           => fail()
      }

    }

    "parse oas file correctly with valid publisher ref of type string" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-String.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(None, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.publisherReference shouldBe "API 1001"
        case _                   => fail()
      }

    }

    "parse oas file correctly with valid publisher ref of type double" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-Double.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(None, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.publisherReference shouldBe "1.5"
        case _                   => fail()
      }

    }

    "parse oas file correctly with valid publisher ref of type integer" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-Integer.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(None, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.publisherReference shouldBe "1001"
        case _                   => fail()
      }

    }

    "parse OpenApi object correctly without extensions" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(
        Some(publisherReference),
        PlatformType.CORE_IF,
        OASSpecType,
        rawOASData(oasContactName)
      )

      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe PlatformType.CORE_IF
          parsedObject.specificationType shouldBe SpecificationType.OAS_V3
          parsedObject.shortDescription shouldBe None
          parsedObject.apiStatus shouldBe ApiStatus.LIVE

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name.getOrElse("") shouldBe oasContactName
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail
          parsedObject.hods shouldBe Nil

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

    "handle when spec file is not present" in new Setup {

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some("someref"), PlatformType.CORE_IF, OASSpecType, "{Unparseable}")
      result shouldBe a[Invalid[_]]

    }

    "parse oas file with scopes correctly" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_multipleEndpoints.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some("test-publisher-ref"), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.endpoints.foreach(
            endpoint =>
              endpoint.methods.foreach(
                method =>
                  method.scopes shouldBe List("write:pets", "read:pets")
              )
          )
        case _                   => fail()
      }
    }

    "parse oas file correctly with scopes" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withFlowsAndScopes.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe "API1000 Get Data"

          parsedObject.endpoints.size shouldBe 1
          parsedObject.endpoints.head.methods.size shouldBe 1
          parsedObject.publisherReference shouldBe publisherReference
          val getMethod: EndpointMethod = parsedObject.endpoints.head.methods.head
          getMethod.httpMethod shouldBe "GET"
          parsedObject.scopes shouldBe Set(Scope("read_pets",Some("read your pets")), Scope("write_pets", Some("modify pets in your account")))
        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

  }

}
