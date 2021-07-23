/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.integrationcatalogue.parser

import cats.data.Validated._
import cats.data._
import org.scalatest.{Assertion, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter
import uk.gov.hmrc.integrationcatalogue.parser.oas.{OASFileLoader, OASParserService}
import uk.gov.hmrc.integrationcatalogue.testdata._

import scala.io.BufferedSource
import scala.io.Source.fromURL
import scala.collection.mutable.LinkedList

class OASParserServiceISpec extends WordSpec with Matchers with OasParsedItTestData with OasTestData with GuiceOneAppPerSuite {

  trait Setup {

    val adapterService: OASV3Adapter = app.injector.instanceOf[OASV3Adapter]
    val fileLoader: OASFileLoader = new OASFileLoader
    val OASSpecType: SpecificationType = SpecificationType.OAS_V3
    val objInTest = new OASParserService(fileLoader, adapterService)

    def parseFileToString(filename: String): String = {
      val x: BufferedSource = fromURL(url = getClass.getResource(filename))
      val fileContents = x.mkString
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

    def validateHeader(header: Header, expectedName: String, expectedDescription  : String): Assertion = {
      header.name shouldBe expectedName
      header.ref shouldBe None
      header.deprecated shouldBe None
      header.required shouldBe None
      header.description shouldBe Some(expectedDescription)
    }

    def testValidationFailureMessage(filePath: String, expectedErrorMessage: String, setHeaderPublisherRef: Boolean = true) = {

      val publisherReference = if(setHeaderPublisherRef) Some("SOMEFILEREFERENCE") else None
      val oasFileContents: String = parseFileToString(filePath)

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(publisherReference, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case errors: Invalid[NonEmptyList[List[String]]] =>
          errors.e.head.contains(expectedErrorMessage) shouldBe true
        case _ => fail()
      }

    }

  }

  "OASParserService" should {
    "parse oas file correctly with schema" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject)                    =>
          parsedObject.title shouldBe "API1000 Get Data"
          val componentHeaders = parsedObject.components.headers
          val componentSchemas = parsedObject.components.schemas
          val componentParameters = parsedObject.components.parameters

          componentHeaders.size shouldBe 1
          validateHeader(componentHeaders.head, "CorrelationId","A UUID format string for the transaction used for traceability purposes")
          componentHeaders.head.schema should not be None

           val headerSchemas = componentHeaders.head.schema.head
          headerSchemas.ref shouldBe None
          headerSchemas.`type` shouldBe Some("string")
          headerSchemas.pattern shouldBe Some("^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$")
          componentSchemas.size shouldBe 18

          componentParameters.size shouldBe 4
          val idTypeParam = componentParameters.head
          idTypeParam.name shouldBe Some("idType")
          idTypeParam.in shouldBe Some("path")
          idTypeParam.description shouldBe Some("Required - Possible values for idType")
          idTypeParam.required shouldBe Some(true)
          idTypeParam.allowEmptyValue shouldBe None
          idTypeParam.schema should not be None
          idTypeParam.schema.flatMap(_.pattern) shouldBe Some("^[A-Z0-9]{3,6}$")
          idTypeParam.schema.flatMap(_.`type`) shouldBe Some("string")

          val idValueParam = componentParameters(1)
          idValueParam.name shouldBe Some("idValue")
          idValueParam.in shouldBe Some("query")
          idValueParam.description shouldBe Some("Required - Value of")
          idValueParam.required shouldBe Some(true)
          idValueParam.deprecated shouldBe Some(false)
          idValueParam.allowEmptyValue shouldBe Some(false)
          idValueParam.schema should not be None
          idValueParam.schema.flatMap(_.pattern) shouldBe Some("^([A-Z0-9]{1,15})$")
          idValueParam.schema.flatMap(_.`type`) shouldBe Some("string")

          val environment = componentParameters(2)
          environment.name shouldBe Some("Environment")
          environment.in shouldBe Some("header")
          environment.description shouldBe Some("Mandatory. The environment in use.")
          environment.required shouldBe Some(true)
          environment.deprecated shouldBe None
          environment.allowEmptyValue shouldBe None
          environment.schema should not be None
          environment.schema.flatMap(_.`type`) shouldBe Some("string")
          environment.schema.map(_.`enum`) shouldBe Some(List("stuff", "stuf1", "stuff3"))


          parsedObject.endpoints.size shouldBe 1
          parsedObject.endpoints.head.methods.size shouldBe 1
          parsedObject.publisherReference shouldBe publisherReference
          val getMethod: EndpointMethod = parsedObject.endpoints.head.methods.head
          getMethod.httpMethod shouldBe "GET"
          getMethod.request shouldBe None
          getMethod.parameters.size shouldBe 5

          getMethod.parameters.head.ref shouldBe Some("#/components/parameters/environment")
          getMethod.parameters.head.name shouldBe None
          getMethod.parameters(1).ref shouldBe Some("#/components/parameters/correlationId")
          getMethod.parameters(1).name shouldBe None
          getMethod.parameters(2).ref shouldBe Some("#/components/parameters/idTypeParam")
          getMethod.parameters(2).name shouldBe None
          getMethod.parameters(3).ref shouldBe Some("#/components/parameters/idValueParam")
          getMethod.parameters(3).name shouldBe None

          val inlineParameter = getMethod.parameters(4)
          inlineParameter.name shouldBe Some("InlineId")
          inlineParameter.ref shouldBe None
          inlineParameter.in shouldBe Some("header")
          inlineParameter.description shouldBe Some("A UUID format string for the transaction.")
          inlineParameter.required shouldBe Some(true)
          inlineParameter.schema.flatMap(_.`type`) shouldBe Some("string")
          inlineParameter.schema.flatMap(_.pattern) shouldBe Some("^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$")

          
          getMethod.responses.size shouldBe 8
          val response200: Response = getMethod.responses.filter(_.statusCode.toInt == 200).head
          response200.mediaType shouldBe Some("application/json")

          response200.headers.size shouldBe 1
          response200.headers.nonEmpty shouldBe true
          validateRefHeader(response200.headers.head, expectedName = "CorrelationId", expectedRef = "#/components/headers/CorrelationId")

          val response401: Response = getMethod.responses.filter(_.statusCode.toInt == 401).head
          response401.schema.isDefined shouldBe true
          response401.schema.head.not.isDefined shouldBe true
          response401.schema.head.not.head.isInstanceOf[ComposedSchema] shouldBe true
          val anyOf401Schemas = response401.schema.head.not.head.asInstanceOf[ComposedSchema].anyOf
          anyOf401Schemas.map(_.ref.getOrElse("")) should contain only ("#/components/schemas/orgName56String", "#/components/schemas/utrType")

          response401.headers.size shouldBe 1
          response401.headers.nonEmpty shouldBe true
          validateRefHeader(response401.headers.head, expectedName = "CorrelationId", expectedRef = "#/components/headers/CorrelationId")

          val response204: Response = getMethod.responses.filter(_.statusCode.toInt == 204).head
          response204.mediaType shouldBe None
          response204.schema shouldBe None

          response204.headers.size shouldBe 1
          response204.headers.nonEmpty shouldBe true
          val header204 = response204.headers.head
          header204.name shouldBe "Location"
          header204.ref shouldBe None
          header204.deprecated shouldBe Some(false)
          header204.required shouldBe Some(true)
          header204.description shouldBe Some("Location of the  request.")
          header204.schema.isDefined shouldBe true
          val headerSchema = header204.schema.get
          headerSchema.description shouldBe Some("Location of the authorisation request.")
          headerSchema.`type` shouldBe Some("string")



        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }
    "parse oas file correctly with default response" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withDefaultResponse.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.title shouldBe "API1000 Get Data"

          parsedObject.endpoints.size shouldBe 1
          parsedObject.endpoints.head.methods.size shouldBe 1
          val getMethod: EndpointMethod = parsedObject.endpoints.head.methods.head
          getMethod.httpMethod shouldBe "GET"
          getMethod.responses.size shouldBe 9
          val responseDefault: Response = getMethod.responses.filter(_.statusCode == "default").head
          responseDefault.description shouldBe Some("Test default response")

      }
    }
    "parse oas file and return endpoints in the order specified in the oas spec" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
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
        case Valid(parsedObject)                    =>
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

    "parse oas file correctly with valid short description" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withValidShortDesc.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.shortDescription.getOrElse("") shouldBe "Hello Im a sensible short description, you wont find me getting too long and breaking any tests. No sireee!!"
        case _ => fail()
      }

    }

    "parse oas file correctly with valid status" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withValidStatus.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.apiStatus shouldBe ApiStatus.BETA
        case _ => fail()
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
      testValidationFailureMessage("/API1000_withInvalidPublisherRef-isWrongType.yaml", "Invalid value. Expected a string, integer or double but found value: true of type class java.lang.Boolean", setHeaderPublisherRef = false)
    }

    "catch error in oas file with missing extension publisher-reference" in new Setup {
      testValidationFailureMessage("/API1000_withMissingPublisherRef.yaml", "Publisher Reference must be provided and must be valid", setHeaderPublisherRef = false)
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
      testValidationFailureMessage("/API1000_InValidStatus.yaml", "Status must be one of ALPHA, BETA, LIVE or DEPRECATED")
    }

    "catch error when publisher ref in oas file does not match the one in the request" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-String.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case errors: Invalid[NonEmptyList[List[String]]] =>
          errors.e.head.contains("Publisher reference provided twice but they do not match") shouldBe true
        case _ => fail()      }

    }

    "parse oas file correctly with valid publisher ref of type string" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-String.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(None, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.publisherReference shouldBe "API 1001"
        case _ => fail()
      }

    }

    "parse oas file correctly with valid publisher ref of type double" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-Double.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(None, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.publisherReference shouldBe "1.5"
        case _ => fail()
      }

    }

    "parse oas file correctly with valid publisher ref of type integer" in new Setup {
      val oasFileContents: String = parseFileToString("/API1000_withValidPublisherRef-Integer.yaml")

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(None, PlatformType.CORE_IF, OASSpecType, oasFileContents)
      result match {
        case Valid(parsedObject) =>
          parsedObject.publisherReference shouldBe "1001"
        case _ => fail()
      }

    }

      "parse OpenApi object correctly without extensions" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, rawOASData)

      result match {
        case Valid(parsedObject)                    =>
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.platform shouldBe PlatformType.CORE_IF
          parsedObject.specificationType shouldBe SpecificationType.OAS_V3
          parsedObject.shortDescription shouldBe None
          parsedObject.apiStatus shouldBe ApiStatus.LIVE

          parsedObject.endpoints.head.methods.head.responses.head.description shouldBe Some("Successful Response")
          parsedObject.endpoints.head.methods.head.responses.head.schema.get.ref shouldBe Some("#/components/schemas/successResponse")

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name.getOrElse("") shouldBe oasContactName
          contact.emailAddress.getOrElse("") shouldBe oasContactEMail
          parsedObject.hods shouldBe Nil

        case _: Invalid[NonEmptyList[List[String]]] => fail()
      }
    }

    "handle when spec file is not present" in new Setup {

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some("someref"), PlatformType.CORE_IF, OASSpecType, "{Unparseable}")
      result match {
        case _: Valid[ApiDetail]                    => fail("should not return a parsed object")
        case _: Invalid[NonEmptyList[List[String]]] => succeed
      }

    }
  }
}
