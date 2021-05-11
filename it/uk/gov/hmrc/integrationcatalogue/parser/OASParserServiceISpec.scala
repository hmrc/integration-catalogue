/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.integrationcatalogue.parser

import cats.data.Validated._
import cats.data._
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.integrationcatalogue.models.common.{ContactInformation, PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, ComposedSchema, DefaultSchema, EndpointMethod, Header, Response}
import uk.gov.hmrc.integrationcatalogue.parser.oas.{OASFileLoader, OASParserService}
import uk.gov.hmrc.integrationcatalogue.testdata._

import scala.io.BufferedSource
import scala.io.Source.fromURL
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.OASV3Adapter

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

    def validateRefHeader(header: Header, expectedName: String, expectedRef: String) = {
      header.name shouldBe expectedName
      header.ref shouldBe Some(expectedRef)
      header.deprecated shouldBe None
      header.required shouldBe None
      header.description shouldBe None
    }

    def validateHeader(header: Header, expectedName: String, expectedDescription  : String) = {
      header.name shouldBe expectedName
      header.ref shouldBe None
      header.deprecated shouldBe None
      header.required shouldBe None
      header.description shouldBe Some(expectedDescription)
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
          componentHeaders.head.schema should not be(None)

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
          idTypeParam.schema should not be(None)
          idTypeParam.schema.flatMap(_.pattern) shouldBe Some("^[A-Z0-9]{3,6}$")
          idTypeParam.schema.flatMap(_.`type`) shouldBe Some("string")

          val idValueParam = componentParameters(1)
          idValueParam.name shouldBe Some("idValue")
          idValueParam.in shouldBe Some("query")
          idValueParam.description shouldBe Some("Required - Value of")
          idValueParam.required shouldBe Some(true)
          idValueParam.deprecated shouldBe Some(false)
          idValueParam.allowEmptyValue shouldBe Some(false)
          idValueParam.schema should not be(None)
          idValueParam.schema.flatMap(_.pattern) shouldBe Some("^([A-Z0-9]{1,15})$")
          idValueParam.schema.flatMap(_.`type`) shouldBe Some("string")

          val environment = componentParameters(2)
          environment.name shouldBe Some("Environment")
          environment.in shouldBe Some("header")
          environment.description shouldBe Some("Mandatory. The environment in use.")
          environment.required shouldBe Some(true)
          environment.deprecated shouldBe None
          environment.allowEmptyValue shouldBe None
          environment.schema should not be(None)
          environment.schema.flatMap(_.`type`) shouldBe Some("string")
          environment.schema.map(_.`enum`) shouldBe Some(List("stuff", "stuf1", "stuff3"))


          parsedObject.endpoints.size shouldBe 1
          parsedObject.endpoints.head.methods.size shouldBe 1
          parsedObject.publisherReference shouldBe publisherReference
          val getMethod: EndpointMethod = parsedObject.endpoints.head.methods.head
          getMethod.httpMethod shouldBe "GET"
          getMethod.request shouldBe None
          getMethod.parameters.size shouldBe 5
          getMethod.parameters.foreach(println)
          getMethod.parameters(0).ref shouldBe Some("#/components/parameters/environment")
          getMethod.parameters(0).name shouldBe None
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
          val response200: Response = getMethod.responses.filter(_.statusCode == 200).head
          response200.mediaType shouldBe Some("application/json")

          response200.headers.size shouldBe 1
          response200.headers.headOption.isDefined shouldBe true
          validateRefHeader(response200.headers.head, expectedName = "CorrelationId", expectedRef = "#/components/headers/CorrelationId")

          val response401: Response = getMethod.responses.filter(_.statusCode == 401).head
          response401.schema.isDefined shouldBe true
          response401.schema.head.not.isDefined shouldBe true
          response401.schema.head.not.head.isInstanceOf[ComposedSchema] shouldBe true
          val anyOf401Schemas = response401.schema.head.not.head.asInstanceOf[ComposedSchema].anyOf
          anyOf401Schemas.map(_.ref.getOrElse("")) should contain only ("#/components/schemas/orgName56String", "#/components/schemas/utrType")

          response401.headers.size shouldBe 1
          response401.headers.headOption.isDefined shouldBe true
          validateRefHeader(response401.headers.head, expectedName = "CorrelationId", expectedRef = "#/components/headers/CorrelationId")

          val response204: Response = getMethod.responses.filter(_.statusCode == 204).head
          response204.mediaType shouldBe None
          response204.schema shouldBe None

          response204.headers.size shouldBe 1
          response204.headers.headOption.isDefined shouldBe true
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

    "parse OpenApi object correctly with extensions" in new Setup {
      val publisherReference = "SOMEFILEREFERENCE"

      val result: ValidatedNel[List[String], ApiDetail] = objInTest.parse(Some(publisherReference), PlatformType.CORE_IF, OASSpecType, rawOASDataWithExtensions)

      result match {
        case Valid(parsedObject)                    =>
          parsedObject.title shouldBe oasApiName
          parsedObject.description shouldBe oasApiDescription
          parsedObject.version shouldBe oasVersion
          parsedObject.searchText shouldBe s"$oasApiName $oasApiDescription CORE IF ITMP Individual Tax Management Platform NPS National Insurance and PAYE System"
          parsedObject.platform shouldBe PlatformType.CORE_IF
          parsedObject.specificationType shouldBe SpecificationType.OAS_V3

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name shouldBe oasContactName
          contact.emailAddress shouldBe oasContactEMail
          parsedObject.hods shouldBe List("ITMP", "NPS")

        case _: Invalid[NonEmptyList[List[String]]] => fail()
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
          parsedObject.searchText shouldBe s"$oasApiName $oasApiDescription CORE IF "
          parsedObject.platform shouldBe PlatformType.CORE_IF
          parsedObject.specificationType shouldBe SpecificationType.OAS_V3

          parsedObject.endpoints.head.methods.head.responses.head.description shouldBe Some("Successful Response")
          parsedObject.endpoints.head.methods.head.responses.head.schema.get.ref shouldBe Some("#/components/schemas/successResponse")

          val contact: ContactInformation = parsedObject.maintainer.contactInfo.head
          contact.name shouldBe oasContactName
          contact.emailAddress shouldBe oasContactEMail
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
